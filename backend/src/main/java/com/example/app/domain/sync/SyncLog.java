package com.example.app.domain.sync;

import com.example.app.domain.externalsystem.ExternalSystem;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 동기화 작업의 실행 이력을 기록하는 엔티티.
 * 스케줄러 또는 수동 트리거 실행마다 1건이 생성된다.
 */
@Entity
@Table(name = "sync_logs", indexes = {
    @Index(name = "idx_sync_logs_system_id", columnList = "external_system_id"),
    @Index(name = "idx_sync_logs_triggered_at", columnList = "triggered_at"),
    @Index(name = "idx_sync_logs_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "external_system_id", nullable = false)
    private ExternalSystem externalSystem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncLogStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    private TriggerType triggerType;

    @Column(name = "triggered_at", nullable = false, updatable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "total_count", nullable = false)
    private int totalCount = 0;

    @Column(name = "success_count", nullable = false)
    private int successCount = 0;

    @Column(name = "fail_count", nullable = false)
    private int failCount = 0;

    @Column(name = "error_summary", length = 2000)
    private String errorSummary;

    public enum SyncLogStatus {
        RUNNING, COMPLETED, PARTIAL_FAILED, FAILED
    }

    public enum TriggerType {
        SCHEDULED, MANUAL, API
    }

    public static SyncLog start(ExternalSystem externalSystem, TriggerType triggerType) {
        SyncLog log = new SyncLog();
        log.externalSystem = externalSystem;
        log.triggerType = triggerType;
        log.status = SyncLogStatus.RUNNING;
        log.triggeredAt = LocalDateTime.now();
        return log;
    }

    public SyncLog complete(int totalCount, int successCount, int failCount) {
        SyncLog updated = copyBase();
        updated.totalCount = totalCount;
        updated.successCount = successCount;
        updated.failCount = failCount;
        updated.completedAt = LocalDateTime.now();
        updated.status = failCount == 0
                ? SyncLogStatus.COMPLETED
                : (successCount > 0 ? SyncLogStatus.PARTIAL_FAILED : SyncLogStatus.FAILED);
        return updated;
    }

    public SyncLog fail(String errorSummary) {
        SyncLog updated = copyBase();
        updated.completedAt = LocalDateTime.now();
        updated.status = SyncLogStatus.FAILED;
        updated.errorSummary = errorSummary;
        return updated;
    }

    private SyncLog copyBase() {
        SyncLog updated = new SyncLog();
        updated.externalSystem = this.externalSystem;
        updated.triggerType = this.triggerType;
        updated.triggeredAt = this.triggeredAt;
        updated.totalCount = this.totalCount;
        updated.successCount = this.successCount;
        updated.failCount = this.failCount;
        updated.errorSummary = this.errorSummary;
        updated.status = this.status;
        return updated;
    }
}
