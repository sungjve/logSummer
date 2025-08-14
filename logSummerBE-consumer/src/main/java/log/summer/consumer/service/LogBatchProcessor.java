package log.summer.consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import log.summer.consumer.model.Log;
import log.summer.consumer.repository.LogRepository;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class LogBatchProcessor implements DisposableBean {

    private final LogRepository logRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ExecutorService workerPool;
    private final ExecutorService leaderThread;

    private static final String LOG_QUEUE_KEY = "log:queue";

    @Value("${log.batch.size:100}")
    private int batchSize;

    @Value("${log.batch.timeout.seconds:5}")
    private long batchTimeoutSeconds;

    private final AtomicLong totalLogsSaved = new AtomicLong(0);

    public LogBatchProcessor(LogRepository logRepository, RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.logRepository = logRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;

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

                // Redis의 BRPOP을 사용하여 첫 번째 로그를 가져올 때까지 대기 (타임아웃 0은 무한대기)
                String firstLogString = redisTemplate.opsForList().rightPop(LOG_QUEUE_KEY, 0, TimeUnit.SECONDS);
                if (firstLogString != null) {
                    logsToSave.add(deserializeLog(firstLogString));
                }

                // 추가 로그를 배치 크기만큼 최대한 빠르게 가져옴 (non-blocking)
                while (logsToSave.size() < batchSize) {
                    String logString = redisTemplate.opsForList().rightPop(LOG_QUEUE_KEY);
                    if (logString == null) {
                        break; // 큐가 비었으면 중단
                    }
                    logsToSave.add(deserializeLog(logString));
                }

                if (!logsToSave.isEmpty()) {
                    submitBatch(logsToSave);
                }

            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                }
                System.err.println("Error processing log queue: " + e.getMessage());
                // 잠시 후 재시도
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private Log deserializeLog(String logString) throws IOException {
        return objectMapper.readValue(logString, Log.class);
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
            }
        });
    }

    @Override
    public void destroy() throws InterruptedException {
        System.out.println("Shutting down LogBatchProcessor...");
        leaderThread.shutdownNow();
        if (!leaderThread.awaitTermination(5, TimeUnit.SECONDS)) {
            System.err.println("Leader thread did not terminate in time.");
        }

        // 워커 풀 종료
        workerPool.shutdown();
        if (!workerPool.awaitTermination(60, TimeUnit.SECONDS)) {
            System.err.println("Worker pool did not terminate in time.");
            workerPool.shutdownNow();
        }
        System.out.println("LogBatchProcessor shut down complete.");
    }
}