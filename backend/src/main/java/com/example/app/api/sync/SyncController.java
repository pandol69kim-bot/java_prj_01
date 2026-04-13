package com.example.app.api.sync;

import com.example.app.common.exception.BusinessException;
import com.example.app.common.response.ApiResponse;
import com.example.app.domain.externalsystem.ExternalSystem;
import com.example.app.domain.externalsystem.ExternalSystemRepository;
import com.example.app.domain.sync.SyncLog;
import com.example.app.infrastructure.scheduler.DataSyncService;
import com.example.app.infrastructure.scheduler.SyncMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Sync", description = "데이터 동기화 관리 API")
@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class SyncController {

    private final DataSyncService dataSyncService;
    private final SyncMetricsService syncMetricsService;
    private final ExternalSystemRepository externalSystemRepository;

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
}
