# 작업 로그

## 메타 정보
- **날짜:** 2026-04-13
- **작업명:** Task #4 — 외부 시스템 연동 WebClient 모듈 개발
- **소요 시간:** ~20분

## 프롬프트 (사용자 요청)
> #4 작업시작

## 작업 내용

### 서브태스크 4.1: WebClientConfig
- Reactor Netty `ConnectionProvider` 커넥션 풀 (maxConnections: 100, pendingAcquire: 500)
- Connect/Read/Write 타임아웃 설정 (application.yml `timeout-seconds` 참조)
- gzip 압축 지원
- 요청/응답 DEBUG 로깅 필터

### 서브태스크 4.2: ExternalApiClient + 에러 핸들링
- `get()`, `post()`, `getList()` 범용 메서드
- `onStatus(4xx/5xx)` → `ExternalApiException` 변환
- `ExternalApiException`: systemCode, statusCode, responseBody 보관, `isClientError()`/`isServerError()`
- fallback 메서드: Circuit Breaker 개방 시 명확한 오류 메시지 반환

### 서브태스크 4.3: Resilience4j Circuit Breaker + Retry
- **Circuit Breaker**: 10회 중 50% 실패 → OPEN, 30초 후 HALF_OPEN, 5회 허용
- **Retry**: 최대 3회, 지수 백오프(1s × 2배), 4xx는 재시도 제외
- **TimeLimiter**: 30초 타임아웃
- `ExternalApiException`(4xx)은 CB 실패 카운트에서 제외 — 서버 장애와 비즈니스 오류 분리

### 서브태스크 4.4: 타임아웃/커넥션 풀 설정
- `WebClientConfig`에 통합 처리
- `maxIdleTime: 30s`, `maxLifeTime: 10m`, `evictInBackground: 60s`

### ExternalSystemService
- `fetchAndSave()`: 외부 API 호출 → JSON 직렬화 → 중복 제거 → PENDING 레코드 저장
- `processPendingRecords()`: PENDING → COMPLETED/FAILED 처리

## 생성/수정된 파일
- `infrastructure/webclient/WebClientConfig.java`
- `infrastructure/webclient/ExternalApiClient.java`
- `infrastructure/webclient/ExternalApiException.java`
- `infrastructure/webclient/ExternalSystemService.java`
- `backend/build.gradle` (resilience4j, mockwebserver 의존성 추가)
- `application.yml` (resilience4j CB/Retry/TimeLimiter 설정 추가)
- `test/.../ExternalApiClientTest.java` (4개 테스트, MockWebServer 사용)

## 특이사항 / 결정 사항
- 4xx 오류는 Circuit Breaker 실패 카운트 제외 — 외부 시스템 장애가 아닌 비즈니스 오류로 처리
- Retry는 IO/Timeout 예외만 재시도, 4xx는 즉시 실패 전달
- 다음 태스크: #5 데이터 동기화 스케줄링 및 배치 처리 구현
