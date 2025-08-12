-- 로그 테이블 생성 스크립트
CREATE TABLE IF NOT EXISTS logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME NOT NULL,
    level VARCHAR(10) NOT NULL,
    logger VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    thread VARCHAR(100),
    exception TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 로그 조회 성능 향상을 위한 인덱스
CREATE INDEX IF NOT EXISTS idx_logs_timestamp ON logs (timestamp);
CREATE INDEX IF NOT EXISTS idx_logs_level ON logs (level);
CREATE INDEX IF NOT EXISTS idx_logs_logger ON logs (logger);