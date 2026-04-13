package com.example.app.infrastructure.scheduler;

import com.example.app.domain.externalsystem.ExternalSystem;
import com.example.app.domain.externalsystem.ExternalSystemRepository;
import com.example.app.domain.sync.ExternalDataSync;
import com.example.app.domain.sync.ExternalDataSyncRepository;
import com.example.app.domain.sync.SyncLog;
import com.example.app.domain.sync.SyncLogRepository;
import com.example.app.infrastructure.webclient.ExternalApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DataSyncServiceTest {

    @Mock private ExternalSystemRepository externalSystemRepository;
    @Mock private ExternalDataSyncRepository externalDataSyncRepository;
    @Mock private SyncLogRepository syncLogRepository;
    @Mock private ExternalApiClient externalApiClient;

    @InjectMocks
    private DataSyncService dataSyncService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ExternalSystem testSystem;

    @BeforeEach
    void setUp() throws Exception {
        // ObjectMapper 필드 주입 (Mockito @InjectMocks는 final 필드 미지원)
        var field = DataSyncService.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(dataSyncService, objectMapper);

        var batchField = DataSyncService.class.getDeclaredField("batchSize");
        batchField.setAccessible(true);
        batchField.set(dataSyncService, 100);

        var retryField = DataSyncService.class.getDeclaredField("maxRetry");
        retryField.setAccessible(true);
        retryField.set(dataSyncService, 3);

        testSystem = ExternalSystem.of(
                "TEST", "테스트", "https://api.test.com",
                ExternalSystem.AuthType.NONE, null
        );
    }

    @Test
    void syncSystem_정상_동기화시_SyncLog를_반환한다() {
        // Arrange
        Map<String, Object> item = Map.of("id", "REF-001", "value", "data1");
        given(externalApiClient.getList(any(), anyString(), eq(Map.class)))
                .willReturn(Flux.just(item));
        given(externalDataSyncRepository
                .findByExternalSystemIdAndExternalRefId(any(), eq("REF-001")))
                .willReturn(Optional.empty());
        given(externalDataSyncRepository.saveAll(anyList()))
                .willAnswer(inv -> inv.getArgument(0));

        SyncLog runningLog = SyncLog.start(testSystem, SyncLog.TriggerType.MANUAL);
        given(syncLogRepository.save(any())).willReturn(runningLog);

        // Act
        SyncLog result = dataSyncService.syncSystem(
                testSystem, "/data", SyncLog.TriggerType.MANUAL);

        // Assert
        assertThat(result).isNotNull();
        verify(syncLogRepository, times(2)).save(any()); // start + complete
    }

    @Test
    void syncSystem_중복_refId는_저장하지_않는다() {
        // Arrange
        Map<String, Object> item = Map.of("id", "REF-DUPE", "value", "data");
        given(externalApiClient.getList(any(), anyString(), eq(Map.class)))
                .willReturn(Flux.just(item));

        ExternalDataSync existing = ExternalDataSync.pending(testSystem, "REF-DUPE", "{}");
        given(externalDataSyncRepository
                .findByExternalSystemIdAndExternalRefId(any(), eq("REF-DUPE")))
                .willReturn(Optional.of(existing));

        SyncLog runningLog = SyncLog.start(testSystem, SyncLog.TriggerType.SCHEDULED);
        given(syncLogRepository.save(any())).willReturn(runningLog);

        // Act
        dataSyncService.syncSystem(testSystem, "/data", SyncLog.TriggerType.SCHEDULED);

        // Assert — 중복이라 saveAll에 빈 리스트가 전달됨
        verify(externalDataSyncRepository).saveAll(argThat(List::isEmpty));
    }

    @Test
    void retryFailedRecords_실패_레코드를_재처리한다() {
        // Arrange
        ExternalDataSync failed = ExternalDataSync.pending(testSystem, "REF-001", "{}")
                .markFailed("이전 오류");
        given(externalDataSyncRepository.findByStatusAndRetryCountLessThan(
                eq(ExternalDataSync.SyncStatus.FAILED), eq(3)))
                .willReturn(List.of(failed));
        given(externalDataSyncRepository.saveAll(anyList()))
                .willAnswer(inv -> inv.getArgument(0));

        // Act
        int retried = dataSyncService.retryFailedRecords();

        // Assert
        assertThat(retried).isEqualTo(1);
    }

    @Test
    void retryFailedRecords_처리할_레코드가_없으면_0을_반환한다() {
        // Arrange
        given(externalDataSyncRepository.findByStatusAndRetryCountLessThan(
                eq(ExternalDataSync.SyncStatus.FAILED), eq(3)))
                .willReturn(List.of());

        // Act
        int retried = dataSyncService.retryFailedRecords();

        // Assert
        assertThat(retried).isZero();
    }
}
