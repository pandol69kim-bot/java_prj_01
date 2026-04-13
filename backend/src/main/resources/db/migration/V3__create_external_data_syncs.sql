-- V3: 외부 데이터 동기화 레코드 테이블 생성
CREATE TABLE external_data_syncs (
    id                   BIGSERIAL    PRIMARY KEY,
    external_system_id   BIGINT       NOT NULL REFERENCES external_systems(id),
    external_ref_id      VARCHAR(200) NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    raw_payload          TEXT,
    processed_payload    TEXT,
    error_message        VARCHAR(1000),
    retry_count          INT          NOT NULL DEFAULT 0,
    synced_at            TIMESTAMP,
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_sync_status CHECK (status IN ('PENDING','PROCESSING','COMPLETED','FAILED','SKIPPED')),
    CONSTRAINT chk_retry_count CHECK (retry_count >= 0)
);

CREATE INDEX idx_eds_system_id_status  ON external_data_syncs (external_system_id, status);
CREATE INDEX idx_eds_external_ref_id   ON external_data_syncs (external_ref_id);
CREATE INDEX idx_eds_synced_at         ON external_data_syncs (synced_at);
CREATE INDEX idx_eds_created_at        ON external_data_syncs (created_at);
