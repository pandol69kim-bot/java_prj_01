package com.example.app.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 데이터 동기화 스케줄러.
 * sync.scheduler.enabled=false 로 테스트/개발 환경에서 비활성화 가능.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sync.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class DataSyncScheduler {

    private static final String DEFAULT_SYNC_PATH = "/data";

    private final DataSyncService dataSyncService;

    /**
     * 전체 동기화: 매 10분마다 모든 활성 외부 시스템 데이터 수집
     * cron: "0 *&#47;10 * * * *"
     */
    @Scheduled(cron = "${sync.scheduler.full-sync-cron:0 */10 * * * *}")
    public void runFullSync() {
        log.info("[Scheduler] 전체 동기화 시작");
        try {
            dataSyncService.syncAllActiveSystems(DEFAULT_SYNC_PATH);
        } catch (Exception e) {
            log.error("[Scheduler] 전체 동기화 중 예외 발생", e);
        }
    }

    /**
     * 실패 레코드 재처리: 매 5분마다 FAILED 상태 레코드 재시도
     * cron: "0 *&#47;5 * * * *"
     */
    @Scheduled(cron = "${sync.scheduler.retry-cron:0 */5 * * * *}")
    public void runRetryFailed() {
        log.info("[Scheduler] 실패 레코드 재처리 시작");
        try {
            int retried = dataSyncService.retryFailedRecords();
            if (retried > 0) {
                log.info("[Scheduler] 실패 레코드 {}건 재처리 성공", retried);
            }
        } catch (Exception e) {
            log.error("[Scheduler] 실패 레코드 재처리 중 예외 발생", e);
        }
    }
}
