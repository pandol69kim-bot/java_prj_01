package com.example.app.api.syncdata;

import com.example.app.common.exception.BusinessException;
import com.example.app.domain.externalsystem.ExternalSystem;
import com.example.app.domain.externalsystem.ExternalSystemRepository;
import com.example.app.domain.sync.ExternalDataSync;
import com.example.app.domain.sync.ExternalDataSyncRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SyncDataQueryService {

    private final ExternalDataSyncRepository repository;
    private final ExternalSystemRepository externalSystemRepository;

    @Transactional(readOnly = true)
    public Page<SyncDataResponse> findWithFilters(
            Long systemId,
            ExternalDataSync.SyncStatus status,
            String refId,
            Pageable pageable) {
        return repository.findAll(buildSpec(systemId, status, refId), pageable)
                .map(SyncDataResponse::from);
    }

    @Transactional(readOnly = true)
    public List<ExternalDataSync> findAllForExport(
            Long systemId,
            ExternalDataSync.SyncStatus status,
            String refId) {
        return repository.findAll(buildSpec(systemId, status, refId));
    }

    @Transactional
    public SyncDataResponse createManual(ManualSyncRequest req) {
        ExternalSystem system = externalSystemRepository.findById(req.systemId())
                .orElseThrow(() -> BusinessException.notFound(
                        "외부 시스템을 찾을 수 없습니다. id=" + req.systemId()));

        ExternalDataSync.SyncStatus status = parseStatus(req.status());
        ExternalDataSync sync = ExternalDataSync.forManualEntry(
                system, req.externalRefId(), req.rawPayload(), status);

        return SyncDataResponse.from(repository.save(sync));
    }

    @Transactional(readOnly = true)
    public SyncDataResponse findById(Long id) {
        ExternalDataSync sync = repository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(
                        "동기화 레코드를 찾을 수 없습니다. id=" + id));
        return SyncDataResponse.from(sync);
    }

    private ExternalDataSync.SyncStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return ExternalDataSync.SyncStatus.PENDING;
        try {
            return ExternalDataSync.SyncStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ExternalDataSync.SyncStatus.PENDING;
        }
    }

    static Specification<ExternalDataSync> buildSpec(
            Long systemId, ExternalDataSync.SyncStatus status, String refId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("externalSystem").get("id"), systemId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (refId != null && !refId.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("externalRefId")),
                        "%" + refId.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
