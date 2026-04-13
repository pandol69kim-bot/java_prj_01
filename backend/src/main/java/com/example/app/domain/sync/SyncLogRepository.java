package com.example.app.domain.sync;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {

    Page<SyncLog> findByExternalSystemId(Long externalSystemId, Pageable pageable);

    Optional<SyncLog> findTopByExternalSystemIdOrderByTriggeredAtDesc(Long externalSystemId);

    List<SyncLog> findByExternalSystemIdAndTriggeredAtBetween(
            Long externalSystemId, LocalDateTime from, LocalDateTime to);

    @Query("""
            SELECT s FROM SyncLog s
            WHERE s.externalSystem.id = :systemId
              AND s.status = 'RUNNING'
            """)
    List<SyncLog> findRunningBySystemId(@Param("systemId") Long systemId);

    @Query("""
            SELECT s.status, COUNT(s) FROM SyncLog s
            WHERE s.triggeredAt >= :since
            GROUP BY s.status
            """)
    List<Object[]> countByStatusSince(@Param("since") LocalDateTime since);
}
