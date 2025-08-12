package log.summer.producer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import log.summer.producer.model.Log;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LogService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String LOG_QUEUE_KEY = "log:queue";

    public LogService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendLogToRedis(Log log) {
        try {
            String logAsString = objectMapper.writeValueAsString(log);
            redisTemplate.opsForList().leftPush(LOG_QUEUE_KEY, logAsString);
            System.out.println("Log sent to Redis: " + logAsString);
        } catch (Exception e) {
            System.err.println("Error serializing log and sending to Redis: " + e.getMessage());
        }
    }
}