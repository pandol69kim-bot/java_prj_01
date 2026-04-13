package com.example.app.domain.sync;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExternalDataSyncRepository extends JpaRepository<ExternalDataSync, Long> {

    Optional<ExternalDataSync> findByExternalSystemIdAndExternalRefId(
            Long externalSystemId, String externalRefId);

    Page<ExternalDataSync> findByExternalSystemId(Long externalSystemId, Pageable pageable);

    List<ExternalDataSync> findByStatusAndRetryCountLessThan(
            ExternalDataSync.SyncStatus status, int maxRetry);

    @Query("""
            SELECT e FROM ExternalDataSync e
            WHERE e.externalSystem.id = :systemId
              AND e.status = :status
              AND e.createdAt >= :since
            ORDER BY e.createdAt DESC
            """)
    List<ExternalDataSync> findBySystemAndStatusSince(
            @Param("systemId") Long systemId,
            @Param("status") ExternalDataSync.SyncStatus status,
            @Param("since") LocalDateTime since);

    @Query("""
            SELECT COUNT(e) FROM ExternalDataSync e
            WHERE e.externalSystem.id = :systemId
              AND e.status = :status
            """)
    long countBySystemIdAndStatus(
            @Param("systemId") Long systemId,
            @Param("status") ExternalDataSync.SyncStatus status);
}
