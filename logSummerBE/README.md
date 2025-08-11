# Log Summer Backend

## 프로젝트 구조

이 프로젝트는 두 개의 독립적인 애플리케이션으로 구성되어 있습니다:

1. **Log Producer**: 로그를 수신하여 Redis에 전달하는 애플리케이션
2. **Log Consumer**: Redis에서 로그를 가져와 데이터베이스에 저장하는 애플리케이션

## 프로젝트 발전 과정

이 프로젝트는 대용량 로그를 효율적으로 처리하기 위해 단계적으로 발전했습니다. 우아한테크캠프의 'Log Bat' 프로젝트에서 영감을 받아 로그 매니저 시스템을 구현하는 과정에서 다음과 같은 단계를 거쳤습니다:

### 1단계: 멀티 쓰레드 처리 도입
- 초기에는 단일 쓰레드로 로그를 처리하는 방식으로 구현
- 대용량 로그 처리 시 성능 병목 현상 발생
- 로그 수집과 저장을 분리하여 별도의 쓰레드 풀을 사용하는 방식으로 개선
- 리더 쓰레드와 워커 쓰레드 풀을 구성하여 병렬 처리 구현
- 리더 쓰레드는 로그 큐를 모니터링하고, 워커 쓰레드는 실제 DB 저장 작업을 담당

### 2단계: 배치 처리 및 타임아웃 적용
- 개별 로그를 하나씩 DB에 저장하는 방식에서 배치 처리 방식으로 전환
- 배치 사이즈(log.batch.size) 설정을 통해 한 번에 처리할 로그 수 지정
- 타임아웃(log.batch.timeout.seconds) 설정을 통해 일정 시간 후 강제 저장 구현
- 배치 처리를 통해 DB 연결 및 I/O 오버헤드 감소
- JDBC의 batchUpdate를 활용하여 대량의 로그를 효율적으로 저장

### 3단계: Redis 도입 및 분산 처리
- 단일 애플리케이션에서 로그 수집과 저장을 모두 처리하는 방식에서 분산 처리 방식으로 전환
- Redis를 메시지 브로커로 도입하여 로그 생산자와 소비자 분리
- 두 개의 독립적인 애플리케이션으로 분리하여 확장성 향상
  - Log Producer: 로그 수신 및 Redis 큐에 저장 담당
  - Log Consumer: Redis 큐에서 로그를 가져와 DB에 저장 담당
- 시스템 장애 시에도 로그 유실 방지 및 안정성 확보
- Redis의 List 자료구조를 활용하여 FIFO(First In First Out) 방식의 로그 처리 구현

## 아키텍처

```
[클라이언트] -> [Log Producer (8080)] -> [Redis] -> [Log Consumer (8081)] -> [Database]
```

- **Log Producer**: REST API를 통해 로그를 수신하고 Redis 큐에 저장
- **Redis**: 메시지 브로커 역할을 하며 두 애플리케이션 간의 통신을 담당
- **Log Consumer**: Redis 큐에서 로그를 가져와 배치 처리 후 데이터베이스에 저장

## 성능 개선 효과

각 단계별 개선을 통해 다음과 같은 성능 향상을 얻을 수 있었습니다:

### 멀티 쓰레드 처리 도입 효과
- 로그 수집과 저장 작업의 병렬화로 처리량 증가
- 단일 쓰레드 대비 약 8-10배 처리 속도 향상
- 시스템 리소스(CPU, 메모리) 효율적 활용

### 배치 처리 및 타임아웃 적용 효과
- DB 연결 횟수 감소로 오버헤드 최소화
- 초당 처리 가능한 로그 수 약 5배 증가
- 배치 사이즈 최적화를 통한 메모리 사용량 조절 가능

### Redis 도입 및 분산 처리 효과
- 시스템 간 느슨한 결합(Loose Coupling)으로 확장성 향상
- 일시적인 부하 증가 시에도 안정적인 로그 처리 가능
- 각 애플리케이션의 독립적인 스케일링 가능
- 시스템 장애 시에도 로그 데이터 보존

## 실행 방법

### Docker Compose 사용

```bash
# 전체 시스템 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f

# 종료
docker-compose down
```

### 개별 실행 (개발 환경)

```bash
# Redis 실행
docker-compose up -d redis

# Log Producer 실행
./gradlew :log-producer:bootRun

# Log Consumer 실행 (별도 터미널에서)
./gradlew :log-consumer:bootRun
```

## API 사용법

### 로그 전송

```bash
curl -X POST http://localhost:8080/log \
  -H "Content-Type: application/json" \
  -d '{"serviceName":"test-service","message":"This is a test log message"}'
```

## 설정

각 애플리케이션의 `application.properties` 파일에서 다음 설정을 변경할 수 있습니다:

### Log Producer
- Redis 연결 정보
- 서버 포트

### Log Consumer
- Redis 연결 정보
- 데이터베이스 연결 정보
- 배치 처리 설정 (크기, 타임아웃)
- 서버 포트