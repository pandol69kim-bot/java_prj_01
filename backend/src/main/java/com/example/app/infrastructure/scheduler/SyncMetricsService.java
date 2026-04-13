package com.example.app.infrastructure.scheduler;

import com.example.app.domain.sync.ExternalDataSync;
import com.example.app.domain.sync.ExternalDataSyncRepository;
import com.example.app.domain.sync.SyncLog;
import com.example.app.domain.sync.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 동기화 상태 현황 집계 서비스.
 * 대시보드 API에서 호출해 실시간 현황을 반환한다.
 */
@Service
@RequiredArgsConstructor
public class SyncMetricsService {

    private final ExternalDataSyncRepository externalDataSyncRepository;
    private final SyncLogRepository syncLogRepository;

    /**
     * 최근 24시간 동기화 현황 요약
     */
    @Transactional(readOnly = true)
    public SyncSummary getSummary() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);

        // SyncLog 상태별 집계
        List<Object[]> logCounts = syncLogRepository.countByStatusSince(since);
        Map<String, Long> logByStatus = logCounts.stream()
                .collect(Collectors.toMap(
                        row -> ((SyncLog.SyncLogStatus) row[0]).name(),
                        row -> (Long) row[1]
                ));

        // ExternalDataSync 상태별 집계 (활성 시스템 전체)
        long pending = countByStatus(since, ExternalDataSync.SyncStatus.PENDING);
        long processing = countByStatus(since, ExternalDataSync.SyncStatus.PROCESSING);
        long completed = countByStatus(since, ExternalDataSync.SyncStatus.COMPLETED);
        long failed = countByStatus(since, ExternalDataSync.SyncStatus.FAILED);
        long skipped = countByStatus(since, ExternalDataSync.SyncStatus.SKIPPED);

        return new SyncSummary(
                pending, processing, completed, failed, skipped,
                logByStatus.getOrDefault("RUNNING", 0L),
                logByStatus.getOrDefault("COMPLETED", 0L),
                logByStatus.getOrDefault("PARTIAL_FAILED", 0L),
                logByStatus.getOrDefault("FAILED", 0L),
                since
        );
    }

    /**
     * 특정 시스템의 최근 SyncLog 목록
     */
    @Transactional(readOnly = true)
    public List<SyncLog> getRecentLogs(Long systemId, int limit) {
        return syncLogRepository
                .findByExternalSystemId(systemId,
                        org.springframework.data.domain.PageRequest.of(0, limit))
                .getContent();
    }

    private long countByStatus(LocalDateTime since, ExternalDataSync.SyncStatus status) {
        // 전체 카운트 (시스템 무관)는 네이티브 쿼리로 처리 — 여기서는 합산
        return externalDataSyncRepository.findByStatusAndRetryCountLessThan(status, Integer.MAX_VALUE)
                .stream()
                .filter(s -> s.getCreatedAt() != null && s.getCreatedAt().isAfter(since))
                .count();
    }

    public record SyncSummary(
            long dataPending,
            long dataProcessing,
            long dataCompleted,
            long dataFailed,
            long dataSkipped,
            long jobRunning,
            long jobCompleted,
            long jobPartialFailed,
            long jobFailed,
            LocalDateTime since
    ) {}
}
