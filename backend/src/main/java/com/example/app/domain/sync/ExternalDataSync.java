package com.example.app.domain.sync;

import com.example.app.common.entity.BaseEntity;
import com.example.app.domain.externalsystem.ExternalSystem;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 외부 시스템에서 수신한 동기화 데이터를 저장하는 엔티티.
 * 원본 데이터(rawPayload)와 처리 상태를 함께 관리한다.
 */
@Entity
@Table(name = "external_data_syncs", indexes = {
    @Index(name = "idx_eds_system_id_status", columnList = "external_system_id, status"),
    @Index(name = "idx_eds_external_ref_id", columnList = "external_ref_id"),
    @Index(name = "idx_eds_synced_at", columnList = "synced_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExternalDataSync extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "external_system_id", nullable = false)
    private ExternalSystem externalSystem;

    /** 외부 시스템에서 부여한 원본 레코드 식별자 */
    @Column(name = "external_ref_id", nullable = false, length = 200)
    private String externalRefId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncStatus status;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "processed_payload", columnDefinition = "TEXT")
    private String processedPayload;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    public enum SyncStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, SKIPPED
    }

    public static ExternalDataSync pending(ExternalSystem externalSystem,
                                            String externalRefId,
                                            String rawPayload) {
        ExternalDataSync sync = new ExternalDataSync();
        sync.externalSystem = externalSystem;
        sync.externalRefId = externalRefId;
        sync.rawPayload = rawPayload;
        sync.status = SyncStatus.PENDING;
        return sync;
    }

    public ExternalDataSync markProcessing() {
        return withStatus(SyncStatus.PROCESSING, this.processedPayload, null);
    }

    public ExternalDataSync markCompleted(String processedPayload) {
        ExternalDataSync updated = withStatus(SyncStatus.COMPLETED, processedPayload, null);
        updated.syncedAt = LocalDateTime.now();
        return updated;
    }

    public ExternalDataSync markFailed(String errorMessage) {
        ExternalDataSync updated = withStatus(SyncStatus.FAILED, this.processedPayload, errorMessage);
        updated.retryCount = this.retryCount + 1;
        return updated;
    }

    private ExternalDataSync withStatus(SyncStatus newStatus, String processed, String error) {
        ExternalDataSync updated = new ExternalDataSync();
        updated.externalSystem = this.externalSystem;
        updated.externalRefId = this.externalRefId;
        updated.rawPayload = this.rawPayload;
        updated.processedPayload = processed;
        updated.errorMessage = error;
        updated.retryCount = this.retryCount;
        updated.syncedAt = this.syncedAt;
        updated.status = newStatus;
        return updated;
    }
}
