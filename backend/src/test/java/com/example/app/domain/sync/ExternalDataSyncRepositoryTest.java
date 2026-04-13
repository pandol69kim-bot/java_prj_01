package com.example.app.domain.sync;

import com.example.app.domain.externalsystem.ExternalSystem;
import com.example.app.domain.externalsystem.ExternalSystemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ExternalDataSyncRepositoryTest {

    @Autowired
    private ExternalDataSyncRepository syncRepository;

    @Autowired
    private ExternalSystemRepository systemRepository;

    private ExternalSystem testSystem;

    @BeforeEach
    void setUp() {
        testSystem = systemRepository.save(
                ExternalSystem.of("TEST_SYS", "테스트 시스템",
                        "https://api.test.com", ExternalSystem.AuthType.API_KEY, "key-123")
        );
    }

    @Test
    void pendingSync를_저장하고_조회한다() {
        // Arrange
        ExternalDataSync sync = ExternalDataSync.pending(testSystem, "REF-001", "{\"data\":1}");

        // Act
        ExternalDataSync saved = syncRepository.save(sync);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(ExternalDataSync.SyncStatus.PENDING);
        assertThat(saved.getExternalRefId()).isEqualTo("REF-001");
    }

    @Test
    void 시스템ID와_refId로_조회한다() {
        // Arrange
        syncRepository.save(ExternalDataSync.pending(testSystem, "REF-002", "{}"));

        // Act
        Optional<ExternalDataSync> found = syncRepository
                .findByExternalSystemIdAndExternalRefId(testSystem.getId(), "REF-002");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getExternalRefId()).isEqualTo("REF-002");
    }

    @Test
    void PENDING_상태이고_재시도횟수_미만인_항목을_조회한다() {
        // Arrange
        syncRepository.save(ExternalDataSync.pending(testSystem, "REF-003", "{}"));

        // Act
        List<ExternalDataSync> pendingList = syncRepository
                .findByStatusAndRetryCountLessThan(ExternalDataSync.SyncStatus.PENDING, 3);

        // Assert
        assertThat(pendingList).hasSize(1);
    }

    @Test
    void 시스템별_상태_카운트를_반환한다() {
        // Arrange
        syncRepository.save(ExternalDataSync.pending(testSystem, "REF-004", "{}"));
        syncRepository.save(ExternalDataSync.pending(testSystem, "REF-005", "{}"));

        // Act
        long count = syncRepository.countBySystemIdAndStatus(
                testSystem.getId(), ExternalDataSync.SyncStatus.PENDING);

        // Assert
        assertThat(count).isEqualTo(2);
    }
}
