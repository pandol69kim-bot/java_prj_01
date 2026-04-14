package com.example.app.api.sync;

import com.example.app.common.dto.PageResponse;
import com.example.app.common.exception.BusinessException;
import com.example.app.common.response.ApiResponse;
import com.example.app.domain.externalsystem.ExternalSystem;
import com.example.app.domain.externalsystem.ExternalSystemRepository;
import com.example.app.domain.sync.SyncLog;
import com.example.app.domain.sync.SyncLogRepository;
import com.example.app.infrastructure.scheduler.DataSyncService;
import com.example.app.infrastructure.scheduler.SyncMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "Sync", description = "데이터 동기화 관리 API")
@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class SyncController {

    private final DataSyncService dataSyncService;
    private final SyncMetricsService syncMetricsService;
    private final ExternalSystemRepository externalSystemRepository;
    private final SyncLogRepository syncLogRepository;

    @Operation(summary = "동기화 현황 요약", description = "최근 24시간 동기화 상태 집계")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<SyncMetricsService.SyncSummary>> getSummary() {
        return ResponseEntity.ok(ApiResponse.ok(syncMetricsService.getSummary()));
    }

    @Operation(summary = "수동 동기화 트리거", description = "특정 외부 시스템 즉시 동기화 실행 (ADMIN 전용)")
    @PostMapping("/trigger/{systemCode}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SyncLogResponse>> triggerSync(
            @PathVariable String systemCode,
            @RequestParam(defaultValue = "/data") String apiPath) {

        ExternalSystem system = externalSystemRepository.findBySystemCode(systemCode)
                .orElseThrow(() -> BusinessException.notFound(
                        "외부 시스템을 찾을 수 없습니다: " + systemCode));

        SyncLog log = dataSyncService.syncSystem(system, apiPath, SyncLog.TriggerType.MANUAL);
        return ResponseEntity.ok(ApiResponse.ok(SyncLogResponse.from(log)));
    }

    @Operation(summary = "실패 레코드 수동 재처리", description = "FAILED 상태 레코드 즉시 재처리 (ADMIN 전용)")
    @PostMapping("/retry-failed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RetryResponse>> retryFailed() {
        int retried = dataSyncService.retryFailedRecords();
        return ResponseEntity.ok(ApiResponse.ok(new RetryResponse(retried)));
    }

    @Operation(summary = "동기화 실행 로그 목록 (페이징)")
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<PageResponse<SyncLogDetailResponse>>> getLogs(
            @Parameter(description = "외부 시스템 ID") @RequestParam(required = false) Long systemId,
            @Parameter(description = "상태 (RUNNING|COMPLETED|PARTIAL_FAILED|FAILED)")
            @RequestParam(required = false) String status,
            @Parameter(description = "트리거 유형 (SCHEDULED|MANUAL|API)")
            @RequestParam(required = false) String triggerType,
            @Parameter(description = "시간 범위(시간 단위, 기본 24h)") @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Specification<SyncLog> spec = buildLogSpec(systemId, status, triggerType, hours);
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "triggeredAt"));

        PageResponse<SyncLogDetailResponse> result = PageResponse.from(
                syncLogRepository.findAll(spec, pageable).map(SyncLogDetailResponse::from));
        return ResponseEntity.ok(result.toApiResponse());
    }

    private Specification<SyncLog> buildLogSpec(Long systemId, String status, String triggerType, int hours) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (systemId != null) {
                predicates.add(cb.equal(root.get("externalSystem").get("id"), systemId));
            }
            if (status != null && !status.isBlank()) {
                try {
                    predicates.add(cb.equal(root.get("status"), SyncLog.SyncLogStatus.valueOf(status.toUpperCase())));
                } catch (IllegalArgumentException ignored) {}
            }
            if (triggerType != null && !triggerType.isBlank()) {
                try {
                    predicates.add(cb.equal(root.get("triggerType"), SyncLog.TriggerType.valueOf(triggerType.toUpperCase())));
                } catch (IllegalArgumentException ignored) {}
            }
            if (hours > 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("triggeredAt"), LocalDateTime.now().minusHours(hours)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public record SyncLogResponse(
            Long id, String systemCode, String status,
            String triggerType, int totalCount, int successCount, int failCount
    ) {
        public static SyncLogResponse from(SyncLog log) {
            return new SyncLogResponse(
                    log.getId(),
                    log.getExternalSystem().getSystemCode(),
                    log.getStatus().name(),
                    log.getTriggerType().name(),
                    log.getTotalCount(),
                    log.getSuccessCount(),
                    log.getFailCount()
            );
        }
    }

    public record RetryResponse(int retriedCount) {}

    public record SyncLogDetailResponse(
            Long id,
            Long systemId,
            String systemCode,
            String status,
            String triggerType,
            int totalCount,
            int successCount,
            int failCount,
            String triggeredAt,
            String completedAt,
            String errorSummary
    ) {
        public static SyncLogDetailResponse from(SyncLog log) {
            return new SyncLogDetailResponse(
                    log.getId(),
                    log.getExternalSystem().getId(),
                    log.getExternalSystem().getSystemCode(),
                    log.getStatus().name(),
                    log.getTriggerType().name(),
                    log.getTotalCount(),
                    log.getSuccessCount(),
                    log.getFailCount(),
                    log.getTriggeredAt() != null ? log.getTriggeredAt().toString() : null,
                    log.getCompletedAt() != null ? log.getCompletedAt().toString() : null,
                    log.getErrorSummary()
            );
        }
    }
}
