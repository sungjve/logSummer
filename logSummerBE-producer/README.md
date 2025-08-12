# Log Summer Producer

## 개요
로그 프로듀서 서비스는 로그를 수신하여 Redis 대기열에 추가하는 역할을 합니다.

## 기술 스택
- Java 21
- Spring Boot 3.3.1
- Spring Data Redis
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

## API 엔드포인트
- 로그 수신 API: `/api/logs`

## 환경 설정
- Redis 연결 설정은 `application.properties` 또는 환경 변수를 통해 구성할 수 있습니다.