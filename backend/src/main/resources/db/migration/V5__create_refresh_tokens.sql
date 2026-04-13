-- V5: 리프레시 토큰 저장 테이블
CREATE TABLE refresh_tokens (
    id         BIGSERIAL     PRIMARY KEY,
    username   VARCHAR(50)   NOT NULL,
    token      VARCHAR(1000) NOT NULL,
    expires_at TIMESTAMP     NOT NULL,
    created_at TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_refresh_tokens_username UNIQUE (username),
    CONSTRAINT uq_refresh_tokens_token    UNIQUE (token)
);

CREATE INDEX idx_refresh_tokens_username ON refresh_tokens (username);
CREATE INDEX idx_refresh_tokens_token    ON refresh_tokens (token);
CREATE INDEX idx_refresh_tokens_expires  ON refresh_tokens (expires_at);
