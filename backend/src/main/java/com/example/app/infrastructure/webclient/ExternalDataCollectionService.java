package com.example.app.infrastructure.webclient;

import com.example.app.common.exception.BusinessException;
import com.example.app.domain.externalsystem.ExternalSystem;
import com.example.app.domain.externalsystem.ExternalSystemRepository;
import com.example.app.domain.sync.ExternalDataSync;
import com.example.app.domain.sync.ExternalDataSyncRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 외부 시스템 데이터 수집 및 동기화 서비스.
 * ExternalApiClient를 통해 데이터를 수신하고 DB에 저장한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalDataCollectionService {

    private final ExternalApiClient externalApiClient;
    private final ExternalSystemRepository externalSystemRepository;
    private final ExternalDataSyncRepository externalDataSyncRepository;
    private final ObjectMapper objectMapper;

    /**
     * 활성 외부 시스템 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ExternalSystem> findActiveSystems() {
        return externalSystemRepository.findAllByActiveTrue();
    }

    /**
     * 특정 외부 시스템에서 데이터를 수집하고 PENDING 레코드로 저장한다.
     *
     * @param systemCode 외부 시스템 코드
     * @param apiPath    호출할 API 경로
     * @return 저장된 레코드 수
     */
    @Transactional
    public Mono<Integer> fetchAndSave(String systemCode, String apiPath) {
        ExternalSystem system = externalSystemRepository.findBySystemCode(systemCode)
                .orElseThrow(() -> BusinessException.notFound(
                        "외부 시스템을 찾을 수 없습니다: " + systemCode));

        if (!system.isActive()) {
            return Mono.error(BusinessException.badRequest(
                    "비활성 외부 시스템입니다: " + systemCode));
        }

        return externalApiClient.getList(system, apiPath, Map.class)
                .map(item -> toJsonSafely(item))
                .filter(json -> json != null)
                .map(json -> {
                    String refId = extractRefId(json);
                    return ExternalDataSync.pending(system, refId, json);
                })
                .filter(sync -> !isDuplicate(system.getId(), sync.getExternalRefId()))
                .collectList()
                .map(syncs -> {
                    List<ExternalDataSync> saved = externalDataSyncRepository.saveAll(syncs);
                    log.info("[{}] {}건 수집 완료 (경로: {})",
                            systemCode, saved.size(), apiPath);
                    return saved.size();
                })
                .doOnError(ExternalApiException.class, e ->
                        log.error("[{}] 데이터 수집 실패: {}", systemCode, e.getMessage()));
    }

    /**
     * PENDING 상태 레코드를 처리(PROCESSING → COMPLETED/FAILED)한다.
     */
    @Transactional
    public int processPendingRecords(int maxRetry) {
        List<ExternalDataSync> pendingList = externalDataSyncRepository
                .findByStatusAndRetryCountLessThan(ExternalDataSync.SyncStatus.PENDING, maxRetry);

        int successCount = 0;
        for (ExternalDataSync sync : pendingList) {
            try {
                ExternalDataSync completed = sync.markCompleted(sync.getRawPayload());
                externalDataSyncRepository.save(completed);
                successCount++;
            } catch (Exception e) {
                log.warn("레코드 처리 실패 [id={}]: {}", sync.getId(), e.getMessage());
                externalDataSyncRepository.save(sync.markFailed(e.getMessage()));
            }
        }
        return successCount;
    }

    private String toJsonSafely(Object item) {
        try {
            return objectMapper.writeValueAsString(item);
        } catch (Exception e) {
            log.warn("JSON 직렬화 실패: {}", e.getMessage());
            return null;
        }
    }

    private String extractRefId(String json) {
        try {
            Map<?, ?> map = objectMapper.readValue(json, Map.class);
            Object id = map.get("id");
            return id != null ? id.toString() : String.valueOf(json.hashCode());
        } catch (Exception e) {
            return String.valueOf(json.hashCode());
        }
    }

    private boolean isDuplicate(Long systemId, String refId) {
        return externalDataSyncRepository
                .findByExternalSystemIdAndExternalRefId(systemId, refId)
                .isPresent();
    }
}
