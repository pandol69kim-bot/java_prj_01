-- V2: 외부 시스템 설정 테이블 생성
CREATE TABLE external_systems (
    id                BIGSERIAL    PRIMARY KEY,
    system_code       VARCHAR(50)  NOT NULL,
    name              VARCHAR(100) NOT NULL,
    base_url          TEXT         NOT NULL,
    api_key           VARCHAR(500),
    auth_type         VARCHAR(20)  NOT NULL DEFAULT 'NONE',
    timeout_seconds   INT          NOT NULL DEFAULT 30,
    retry_max_attempts INT         NOT NULL DEFAULT 3,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    description       VARCHAR(500),
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_external_systems_code UNIQUE (system_code),
    CONSTRAINT chk_auth_type CHECK (auth_type IN ('NONE', 'API_KEY', 'BEARER_TOKEN', 'BASIC')),
    CONSTRAINT chk_timeout   CHECK (timeout_seconds > 0),
    CONSTRAINT chk_retry     CHECK (retry_max_attempts >= 0)
);

CREATE INDEX idx_external_systems_code ON external_systems (system_code);
CREATE INDEX idx_external_systems_active ON external_systems (active);
