package log.summer.be;

import org.springframework.stereotype.Service;

@Service
public class LogService {

    private final LogBatchProcessor logBatchProcessor;

    public LogService(LogBatchProcessor logBatchProcessor) {
        this.logBatchProcessor = logBatchProcessor;
    }

    public void processLog(Log log) {
        logBatchProcessor.addLog(log);
    }
}