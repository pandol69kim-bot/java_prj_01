# 작업 로그

## 메타 정보
- **날짜:** 2026-04-13
- **작업명:** Task #3 — JWT 인증 시스템 및 Spring Security 구성
- **소요 시간:** ~20분

## 프롬프트 (사용자 요청)
> #3 작업시작

## 작업 내용

### 서브태스크 3.1: JwtTokenProvider
- HMAC-SHA 서명, `access`/`refresh` 타입 클레임으로 구분
- `generateAccessToken(username, role)` / `generateRefreshToken(username)`
- `validateToken`, `extractUsername`, `extractRole`, `isAccessToken`, `isRefreshToken`
- 예외별 로그 레벨 분리 (만료 DEBUG, 위변조 WARN)

### 서브태스크 3.2: Spring Security 필터 체인
- `JwtAuthenticationFilter`: `OncePerRequestFilter`, Bearer 토큰 추출 → SecurityContext 설정
- `SecurityConfig`: CSRF disable, STATELESS 세션, CORS 설정
- 공개 경로: `POST /api/v1/auth/**`, `/actuator/health`, Swagger UI
- 관리자 경로: `/api/v1/admin/**` ROLE_ADMIN 전용
- 401/403 JSON 에러 응답 (ApiResponse 포맷)
- `CustomUserDetailsService`: UserRepository 기반 UserDetails 로드

### 서브태스크 3.3: 인증 API
- `POST /api/v1/auth/login` — username/password → AccessToken + RefreshToken
- `POST /api/v1/auth/refresh` — RefreshToken → 새 토큰 쌍
- `POST /api/v1/auth/logout` — RefreshToken 무효화
- DTO: `LoginRequest`, `RefreshRequest` (record + Bean Validation), `TokenResponse`

### 서브태스크 3.4: RefreshToken 저장/갱신
- `RefreshToken` 엔티티: 1인 1토큰 정책 (username unique)
- `rotate()` 메서드로 토큰 교체 시 새 인스턴스 반환 (불변 패턴)
- `isExpired()` DB 만료 검사
- `RefreshTokenCleanupScheduler`: 매일 새벽 3시 만료 토큰 일괄 삭제
- `V5__create_refresh_tokens.sql` Flyway 마이그레이션

## 생성된 파일
- `infrastructure/security/JwtTokenProvider.java`
- `infrastructure/security/JwtAuthenticationFilter.java`
- `infrastructure/security/SecurityConfig.java`
- `infrastructure/security/CustomUserDetailsService.java`
- `infrastructure/security/RefreshTokenCleanupScheduler.java`
- `domain/auth/RefreshToken.java`
- `domain/auth/RefreshTokenRepository.java`
- `api/auth/AuthController.java`
- `api/auth/AuthService.java`
- `api/auth/LoginRequest.java`, `RefreshRequest.java`, `TokenResponse.java`
- `db/migration/V5__create_refresh_tokens.sql`
- `test/.../JwtTokenProviderTest.java` (5개 테스트)
- `test/.../AuthControllerTest.java` (5개 테스트)

## 특이사항 / 결정 사항
- Refresh Token Rotation: 갱신 시 기존 토큰 즉시 교체 → 탈취 탐지 가능
- 만료 검사는 DB(expiresAt)와 JWT 서명 두 레이어에서 이중 검증
- 로그인 시 기존 refresh_token이 있으면 rotate(), 없으면 신규 생성
- 다음 태스크: #4 외부 시스템 연동 WebClient 모듈 개발
