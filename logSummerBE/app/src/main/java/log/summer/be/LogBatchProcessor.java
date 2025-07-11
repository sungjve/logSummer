package log.summer.be;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class LogBatchProcessor {

    private final BlockingQueue<Log> logQueue;
    private final LogRepository logRepository;

    @Value("${log.batch.size:100}") // application.properties에서 설정, 기본값 100
    private int batchSize;

    @Value("${log.batch.timeout.seconds:5}") // application.properties에서 설정, 기본값 5초
    private int batchTimeoutSeconds;

    private long lastFlushTime;

    // 얼마나 많은 로그를 저장했는지 추적
    private final AtomicLong totalLogsSaved = new AtomicLong(0);

    public LogBatchProcessor(LogRepository logRepository) {
        this.logRepository = logRepository;
        this.logQueue = new LinkedBlockingQueue<>();
    }

    @PostConstruct
    public void init() {
        this.lastFlushTime = System.currentTimeMillis();
    }

    public void addLog(Log log) {
        logQueue.add(log);
        if (logQueue.size() >= batchSize) {
            flushLogs();
        }
    }

    @Scheduled(fixedRateString = "${log.batch.timeout.seconds:5}000") // 설정된 타임아웃마다 실행
    public void scheduledFlush() {
        if (!logQueue.isEmpty() && (System.currentTimeMillis() - lastFlushTime) >= (batchTimeoutSeconds * 1000)) {
            flushLogs();
        }
    }

    private synchronized void flushLogs() {
        if (logQueue.isEmpty()) {
            return;
        }

        List<Log> logsToSave = new ArrayList<>();
        logQueue.drainTo(logsToSave, batchSize); // batchSize만큼 또는 큐에 있는 모든 로그를 가져옴

        if (!logsToSave.isEmpty()) {
            try {
                int[] insertedRows = logRepository.batchInsert(logsToSave);
                int count = 0;
                for (int rows : insertedRows) {
                    count += rows; // 실제 삽입된 행의 수를 합산
                }
                totalLogsSaved.addAndGet(count);
                System.out.println("Saved " + count + " logs in batch. Total logs saved: " + totalLogsSaved.get());
            } catch (Exception e) {
                System.err.println("Error saving logs in batch: " + e.getMessage());
                // 실패한 로그를 다시 큐에 넣거나, 별도의 처리 (예: 데드 레터 큐) 고려
            }
        }
        lastFlushTime = System.currentTimeMillis();
    }
}
