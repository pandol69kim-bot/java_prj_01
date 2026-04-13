-- V4: 동기화 실행 이력 테이블 생성
CREATE TABLE sync_logs (
    id                 BIGSERIAL   PRIMARY KEY,
    external_system_id BIGINT      NOT NULL REFERENCES external_systems(id),
    status             VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    trigger_type       VARCHAR(20) NOT NULL,
    triggered_at       TIMESTAMP   NOT NULL DEFAULT NOW(),
    completed_at       TIMESTAMP,
    total_count        INT         NOT NULL DEFAULT 0,
    success_count      INT         NOT NULL DEFAULT 0,
    fail_count         INT         NOT NULL DEFAULT 0,
    error_summary      VARCHAR(2000),

    CONSTRAINT chk_sync_log_status CHECK (status IN ('RUNNING','COMPLETED','PARTIAL_FAILED','FAILED')),
    CONSTRAINT chk_trigger_type    CHECK (trigger_type IN ('SCHEDULED','MANUAL','API')),
    CONSTRAINT chk_counts          CHECK (success_count + fail_count <= total_count)
);

CREATE INDEX idx_sync_logs_system_id    ON sync_logs (external_system_id);
CREATE INDEX idx_sync_logs_triggered_at ON sync_logs (triggered_at);
CREATE INDEX idx_sync_logs_status       ON sync_logs (status);
