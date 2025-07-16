package log.summer.be;

import java.io.Serializable;

public class Log implements Serializable {
    private String serviceName;
    private String message;

    // 기본 생성자 (JSON 역직렬화를 위해 필요)
    public Log() {
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}