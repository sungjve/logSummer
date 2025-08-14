package log.summer.consumer.repository;

import log.summer.consumer.model.Log;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class LogRepository {

    private final JdbcTemplate jdbcTemplate;

    public LogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int[] batchInsert(List<Log> logs) {
        String sql = "INSERT INTO logs (timestamp, level, logger, message, thread, exception, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

        return jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                Log log = logs.get(i);
                // timestamp가 null이면 현재 시간으로 설정
                LocalDateTime timestamp = log.getTimestamp() != null ? log.getTimestamp() : LocalDateTime.now();
                ps.setTimestamp(1, Timestamp.valueOf(timestamp));
                
                // level이 null이면 INFO로 설정
                ps.setString(2, log.getLevel() != null ? log.getLevel() : "INFO");
                
                // logger가 null이면 serviceName 사용, 둘 다 null이면 "unknown"으로 설정
                String logger = log.getLogger();
                if (logger == null) {
                    logger = log.getServiceName() != null ? log.getServiceName() : "unknown";
                }
                ps.setString(3, logger);
                
                // message는 필수 필드이지만 null 체크
                ps.setString(4, log.getMessage() != null ? log.getMessage() : "");
                
                // thread는 선택적 필드
                ps.setString(5, log.getThread());
                
                // exception은 선택적 필드
                ps.setString(6, log.getException());
                
                // created_at은 현재 시간으로 설정
                ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            }
            
            @Override
            public int getBatchSize() {
                return logs.size();
            }
        });
    }
}