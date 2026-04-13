package com.example.app.infrastructure.scheduler;

import com.example.app.domain.externalsystem.ExternalSystem;
import com.example.app.domain.externalsystem.ExternalSystemRepository;
import com.example.app.domain.sync.ExternalDataSync;
import com.example.app.domain.sync.ExternalDataSyncRepository;
import com.example.app.domain.sync.SyncLog;
import com.example.app.domain.sync.SyncLogRepository;
import com.example.app.infrastructure.webclient.ExternalApiClient;
import com.example.app.infrastructure.webclient.ExternalApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 외부 시스템 데이터 동기화 핵심 서비스.
 * 수집(fetch) → 저장(save) → 처리(process) 파이프라인을 관리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataSyncService {

    private final ExternalSystemRepository externalSystemRepository;
    private final ExternalDataSyncRepository externalDataSyncRepository;
    private final SyncLogRepository syncLogRepository;
    private final ExternalApiClient externalApiClient;
    private final ObjectMapper objectMapper;

    @Value("${sync.scheduler.batch-size:100}")
    private int batchSize;

    @Value("${sync.scheduler.max-retry:3}")
    private int maxRetry;

    /**
     * 단일 외부 시스템 전체 동기화 실행.
     * SyncLog를 생성해 실행 이력을 기록한다.
     *
     * @param system      대상 외부 시스템
     * @param apiPath     데이터 수집 경로
     * @param triggerType 트리거 유형
     * @return 생성된 SyncLog
     */
    @Transactional
    public SyncLog syncSystem(ExternalSystem system, String apiPath,
                               SyncLog.TriggerType triggerType) {
        SyncLog runningLog = syncLogRepository.save(SyncLog.start(system, triggerType));
        log.info("[{}] 동기화 시작 (trigger={})", system.getSystemCode(), triggerType);

        List<ExternalDataSync> fetched = fetchFromExternal(system, apiPath);
        SyncResult result = processBatch(fetched);

        SyncLog completedLog = runningLog.complete(
                result.total(), result.success(), result.fail());
        SyncLog saved = syncLogRepository.save(completedLog);

        log.info("[{}] 동기화 완료 — 전체:{} 성공:{} 실패:{}",
                system.getSystemCode(), result.total(), result.success(), result.fail());
        return saved;
    }

    /**
     * 모든 활성 시스템에 대해 동기화 실행 (스케줄러 호출용)
     */
    @Transactional
    public void syncAllActiveSystems(String defaultApiPath) {
        List<ExternalSystem> activeSystems = externalSystemRepository.findAllByActiveTrue();
        log.info("전체 동기화 시작 — 활성 시스템 {}개", activeSystems.size());

        for (ExternalSystem system : activeSystems) {
            try {
                syncSystem(system, defaultApiPath, SyncLog.TriggerType.SCHEDULED);
            } catch (Exception e) {
                log.error("[{}] 동기화 중 예외 발생: {}", system.getSystemCode(), e.getMessage(), e);
            }
        }
    }

    /**
     * FAILED 상태 레코드 재처리 (최대 재시도 횟수 미만인 것만)
     *
     * @return 재처리 성공 건수
     */
    @Transactional
    public int retryFailedRecords() {
        List<ExternalDataSync> failedList = externalDataSyncRepository
                .findByStatusAndRetryCountLessThan(ExternalDataSync.SyncStatus.FAILED, maxRetry);

        if (failedList.isEmpty()) {
            return 0;
        }

        log.info("실패 레코드 재처리 시작 — {}건", failedList.size());
        SyncResult result = processBatch(failedList);
        log.info("실패 레코드 재처리 완료 — 성공:{} 실패:{}", result.success(), result.fail());
        return result.success();
    }

    // ── 내부 구현 ─────────────────────────────────────────────────────────────

    private List<ExternalDataSync> fetchFromExternal(ExternalSystem system, String apiPath) {
        List<ExternalDataSync> result = new ArrayList<>();
        try {
            externalApiClient.getList(system, apiPath, Map.class)
                    .map(item -> toJsonSafely(item))
                    .filter(json -> json != null)
                    .toStream()
                    .forEach(json -> {
                        String refId = extractRefId(json);
                        boolean isDuplicate = externalDataSyncRepository
                                .findByExternalSystemIdAndExternalRefId(system.getId(), refId)
                                .isPresent();

                        if (!isDuplicate) {
                            result.add(ExternalDataSync.pending(system, refId, json));
                        }
                    });
        } catch (ExternalApiException e) {
            log.warn("[{}] 외부 API 호출 실패, 수집 건너뜀: {}",
                    system.getSystemCode(), e.getMessage());
        }
        return result;
    }

    private SyncResult processBatch(List<ExternalDataSync> records) {
        int total = records.size();
        int success = 0;
        int fail = 0;

        // 배치 크기로 나눠서 처리
        for (int i = 0; i < records.size(); i += batchSize) {
            List<ExternalDataSync> batch = records.subList(i,
                    Math.min(i + batchSize, records.size()));

            List<ExternalDataSync> toSave = new ArrayList<>();
            for (ExternalDataSync sync : batch) {
                try {
                    toSave.add(sync.markCompleted(sync.getRawPayload()));
                    success++;
                } catch (Exception e) {
                    log.warn("레코드 처리 실패 [refId={}]: {}",
                            sync.getExternalRefId(), e.getMessage());
                    toSave.add(sync.markFailed(e.getMessage()));
                    fail++;
                }
            }
            externalDataSyncRepository.saveAll(toSave);
        }

        return new SyncResult(total, success, fail);
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

    public record SyncResult(int total, int success, int fail) {}
}
