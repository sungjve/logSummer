package log.summer.be;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class LogService {

    private final JdbcTemplate jdbcTemplate;

    public LogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveLog(Log log) {
        String sql = "INSERT INTO logs (service_name, message) VALUES (?, ?)";
        jdbcTemplate.update(sql, log.getServiceName(), log.getMessage());
    }
}
