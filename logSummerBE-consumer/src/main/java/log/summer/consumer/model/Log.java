package log.summer.consumer.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Log implements Serializable {
    private Long id;
    private LocalDateTime timestamp;
    private String level;
    private String logger;
    private String message;
    private String thread;
    private String exception;
    private LocalDateTime createdAt;
    
    // 이전 버전과의 호환성을 위한 필드
    private String serviceName;

    // 기본 생성자 (JSON 역직렬화를 위해 필요)
    public Log() {
        this.timestamp = LocalDateTime.now();
        this.level = "INFO";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLogger() {
        return logger;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
        // 이전 버전 호환성을 위해 serviceName을 logger로 설정
        if (this.logger == null) {
            this.logger = serviceName;
        }
    }
}