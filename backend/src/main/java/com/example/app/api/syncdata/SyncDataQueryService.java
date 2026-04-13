package com.example.app.api.syncdata;

import com.example.app.common.exception.BusinessException;
import com.example.app.domain.sync.ExternalDataSync;
import com.example.app.domain.sync.ExternalDataSyncRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SyncDataQueryService {

    private final ExternalDataSyncRepository externalDataSyncRepository;

    @Transactional(readOnly = true)
    public Page<SyncDataResponse> findBySystem(Long systemId, Pageable pageable) {
        return externalDataSyncRepository
                .findByExternalSystemId(systemId, pageable)
                .map(SyncDataResponse::from);
    }

    @Transactional(readOnly = true)
    public SyncDataResponse findById(Long id) {
        ExternalDataSync sync = externalDataSyncRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(
                        "동기화 레코드를 찾을 수 없습니다. id=" + id));
        return SyncDataResponse.from(sync);
    }
}
