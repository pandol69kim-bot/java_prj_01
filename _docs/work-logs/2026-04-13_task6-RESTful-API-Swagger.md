# 작업 로그

## 메타 정보
- **날짜:** 2026-04-13
- **작업명:** Task #6 — RESTful API 엔드포인트 설계 및 Swagger 문서화
- **소요 시간:** ~20분

## 프롬프트 (사용자 요청)
> #6 작업시작

## 작업 내용

### 6.1: SwaggerConfig (OpenAPI + JWT 보안 스킴)
- `BearerAuth` SecurityScheme (HTTP Bearer JWT) 전역 등록
- 서버 목록: 로컬(8080) + 운영(api.example.com)
- 공통 응답 형식 및 인증 방법 API 문서 내 설명 포함
- Swagger UI: `/swagger-ui.html`, OpenAPI JSON: `/api/v1/api-docs`

### 6.2: PageResponse<T> 공통 페이징 DTO
- `Page<T>.from()`, `Page<S, T>.from(mapper)` 팩토리 메서드
- `toApiResponse()` → `ApiResponse<PageResponse<T>>` 변환 (meta 포함)

### 6.3: ExternalSystem CRUD API
- `GET  /api/v1/external-systems` — 목록(페이징, 기본 20건)
- `GET  /api/v1/external-systems/active` — 활성 전체 조회
- `GET  /api/v1/external-systems/{id}` — 단건 조회
- `POST /api/v1/external-systems` — 등록 (ADMIN, 201)
- `PATCH /api/v1/external-systems/{id}/api-key` — API 키 갱신 (ADMIN)
- `DELETE /api/v1/external-systems/{id}` — 비활성화 (ADMIN, 소프트 삭제)
- `ExternalSystemRequest`: Bean Validation (systemCode 정규식, URL 패턴, 범위 제한)
- `ExternalSystemResponse`: API 키 제외한 안전한 응답 (보안)

### 6.4: SyncData 조회 API
- `GET /api/v1/sync-data?systemId={id}` — 시스템별 목록 (페이징 + 정렬)
- `GET /api/v1/sync-data/{id}` — 단건 조회
- 정렬: `createdAt` / `syncedAt`, `asc` / `desc`

## 전체 API 엔드포인트 목록

| Method | Path | 권한 | 설명 |
|--------|------|------|------|
| POST | /api/v1/auth/login | 공개 | 로그인 |
| POST | /api/v1/auth/refresh | 공개 | 토큰 갱신 |
| POST | /api/v1/auth/logout | 인증 | 로그아웃 |
| GET | /api/v1/external-systems | 인증 | 시스템 목록 |
| POST | /api/v1/external-systems | ADMIN | 시스템 등록 |
| PATCH | /api/v1/external-systems/{id}/api-key | ADMIN | API 키 갱신 |
| DELETE | /api/v1/external-systems/{id} | ADMIN | 비활성화 |
| GET | /api/v1/sync-data | 인증 | 동기화 데이터 목록 |
| GET | /api/v1/sync-data/{id} | 인증 | 동기화 데이터 단건 |
| GET | /api/v1/sync/summary | 인증 | 동기화 현황 |
| POST | /api/v1/sync/trigger/{code} | ADMIN | 수동 동기화 |
| POST | /api/v1/sync/retry-failed | ADMIN | 실패 재처리 |

## 생성/수정된 파일
- `infrastructure/config/SwaggerConfig.java`
- `common/dto/PageResponse.java`
- `api/externalsystem/ExternalSystemRequest.java`
- `api/externalsystem/ExternalSystemResponse.java`
- `api/externalsystem/ExternalSystemService.java`
- `api/externalsystem/ExternalSystemController.java`
- `api/syncdata/SyncDataResponse.java`
- `api/syncdata/SyncDataQueryService.java`
- `api/syncdata/SyncDataController.java`
- `test/.../ExternalSystemControllerTest.java` (6개 테스트)
- `test/.../SyncDataControllerTest.java` (3개 테스트)

## 특이사항 / 결정 사항
- `ExternalSystemResponse`에서 API 키 필드 제외 (보안 — 응답에 키 노출 방지)
- DELETE는 물리 삭제가 아닌 `active=false` 소프트 삭제
- Task 6 서브태스크가 0개로 expand 미완료 → 태스크 설명 기반으로 직접 구현
- 다음 태스크: #7 Vue.js 3 프론트엔드 기반 구조 및 공통 컴포넌트
