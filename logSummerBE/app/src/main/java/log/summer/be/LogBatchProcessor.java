package log.summer.be;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class LogBatchProcessor implements DisposableBean {

    private final BlockingQueue<Log> logQueue;
    private final LogRepository logRepository;
    private final ExecutorService workerPool;
    private final ExecutorService leaderThread;

    @Value("${log.batch.size:100}")
    private int batchSize;

    @Value("${log.batch.timeout.seconds:5}")
    private long batchTimeoutSeconds;

    private final AtomicLong totalLogsSaved = new AtomicLong(0);

    public LogBatchProcessor(LogRepository logRepository) {
        this.logRepository = logRepository;
        this.logQueue = new LinkedBlockingQueue<>();
        // 워커 스레드 풀: 데이터베이스에 로그를 쓰는 작업을 처리
        this.workerPool = Executors.newFixedThreadPool(10, new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "log-worker-" + threadNumber.getAndIncrement());
                if (t.isDaemon()) t.setDaemon(false);
                if (t.getPriority() != Thread.NORM_PRIORITY) t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        });
        // 리더 스레드: 큐를 감시하고 작업을 워커 풀에 제출
        this.leaderThread = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "log-leader-thread");
                if (t.isDaemon()) t.setDaemon(false);
                if (t.getPriority() != Thread.NORM_PRIORITY) t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        });
    }

    @PostConstruct
    public void init() {
        leaderThread.submit(this::processQueue);
    }

    private void processQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<Log> logsToSave = new ArrayList<>();
                // 첫 번째 로그를 가져올 때까지 대기
                logsToSave.add(logQueue.take());

                // 타임아웃 또는 배치 크기에 도달할 때까지 추가 로그를 수집
                long timeoutNanos = TimeUnit.SECONDS.toNanos(batchTimeoutSeconds);
                long deadline = System.nanoTime() + timeoutNanos;

                while (logsToSave.size() < batchSize) {
                    long remainingNanos = deadline - System.nanoTime();
                    if (remainingNanos <= 0) {
                        break; // 타임아웃
                    }
                    Log log = logQueue.poll(remainingNanos, TimeUnit.NANOSECONDS);
                    if (log == null) {
                        break; // 타임아웃
                    }
                    logsToSave.add(log);
                }

                if (!logsToSave.isEmpty()) {
                    submitBatch(logsToSave);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Leader thread interrupted, shutting down.");
                break;
            }
        }
        // 루프 종료 후 남은 로그 처리
        flushRemainingOnShutdown();
    }

    public void addLog(Log log) {
        // 큐가 가득 찼을 경우를 대비해 offer 사용 (LinkedBlockingQueue는 기본적으로 unbounded)
        if (!logQueue.offer(log)) {
            System.err.println("Log queue is full. Log was dropped.");
        }
    }

    private void submitBatch(List<Log> logs) {
        workerPool.submit(() -> {
            try {
                int[] insertedRows = logRepository.batchInsert(logs);
                int count = 0;
                for (int row : insertedRows) {
                    count += row;
                }
                totalLogsSaved.addAndGet(count);
                System.out.println("Saved " + count + " logs in batch. Total logs saved: " + totalLogsSaved.get());
            } catch (Exception e) {
                System.err.println("Error saving logs in batch: " + e.getMessage());
                // 실패 처리 로직 (예: 로그 파일에 기록, 재시도 큐에 추가)
            }
        });
    }

    private void flushRemainingOnShutdown() {
        List<Log> remainingLogs = new ArrayList<>();
        logQueue.drainTo(remainingLogs);
        if (!remainingLogs.isEmpty()) {
            System.out.println("Flushing " + remainingLogs.size() + " remaining logs on shutdown.");
            submitBatch(remainingLogs);
        }
    }

    @Override
    public void destroy() throws InterruptedException {
        System.out.println("Shutting down LogBatchProcessor...");
        // 리더 스레드 종료
        leaderThread.shutdownNow(); // interrupt() 호출
        if (!leaderThread.awaitTermination(5, TimeUnit.SECONDS)) {
            System.err.println("Leader thread did not terminate in time.");
        }

        // 종료 직전에 큐에 남은 로그 모두 처리
        flushRemainingOnShutdown();

        // 워커 풀 종료
        workerPool.shutdown();
        if (!workerPool.awaitTermination(60, TimeUnit.SECONDS)) {
            System.err.println("Worker pool did not terminate in time.");
            workerPool.shutdownNow();
        }
        System.out.println("LogBatchProcessor shut down complete.");
    }
}
