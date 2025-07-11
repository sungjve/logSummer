package log.summer.be;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
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
        String sql = "INSERT INTO logs (service_name, log_level, message, created_at) VALUES (?, ?, ?, ?)";

        return jdbcTemplate.batchUpdate(sql, logs, logs.size(),
                (ps, log) -> {
                    ps.setString(1, log.getServiceName());
                    // 기본 로그 레벨은 INFO로 설정
                    ps.setString(2, "INFO"); 
                    ps.setString(3, log.getMessage());
                    ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                });
    }
}
