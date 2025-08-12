# Log Summer Consumer

## 개요
로그 컨슈머 서비스는 Redis 대기열에서 로그를 가져와 처리하고 MySQL 데이터베이스에 저장하는 역할을 합니다.

## 기술 스택
- Java 21
- Spring Boot 3.3.1
- Spring Data Redis
- Spring JDBC
- MySQL 8.0
- Docker

## 빌드 및 실행 방법

### Gradle로 빌드하기
```bash
./gradlew clean build
```

### Docker로 실행하기
```bash
docker-compose up -d
```

## 배치 처리 로직
- Redis 대기열에서 로그를 일정 주기로 가져와 배치 처리합니다.
- 처리된 로그는 MySQL 데이터베이스에 저장됩니다.

## MySQL 데이터베이스 통합

### 데이터베이스 스키마
- `logs` 테이블에는 다음 필드가 포함됩니다:
  - `id`: 로그 항목의 고유 식별자 (자동 증가)
  - `timestamp`: 로그가 생성된 시간
  - `level`: 로그 레벨 (INFO, WARN, ERROR 등)
  - `logger`: 로그를 생성한 로거 또는 서비스 이름
  - `message`: 로그 메시지 내용
  - `thread`: 로그를 생성한 스레드 이름
  - `exception`: 예외 정보 (있는 경우)
  - `created_at`: 로그가 데이터베이스에 저장된 시간

### 데이터베이스 설정
- Docker Compose를 통해 MySQL 8.0 컨테이너가 자동으로 구성됩니다.
- 데이터베이스 이름: `log_summer`
- 기본 사용자 이름: `root`
- 기본 비밀번호: `password`
- 포트: `3306`
- 문자셋: `utf8mb4`

## 환경 설정
- Redis 및 MySQL 연결 설정은 `application.properties` 또는 환경 변수를 통해 구성할 수 있습니다.
- 스키마는 애플리케이션 시작 시 자동으로 적용됩니다 (`schema.sql` 파일 사용).