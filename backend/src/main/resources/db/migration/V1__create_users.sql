-- V1: 사용자 테이블 생성
CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL,
    email      VARCHAR(100) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER',
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT chk_users_role    CHECK (role IN ('ROLE_ADMIN', 'ROLE_USER'))
);

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email    ON users (email);

-- 초기 관리자 계정 (비밀번호는 애플리케이션 시작 시 별도 설정)
-- password: 'admin1234' BCrypt 인코딩 값 (운영 환경에서 반드시 교체)
INSERT INTO users (username, email, password, role)
VALUES ('admin', 'admin@example.com',
        '$2b$10$6BChRMr1u/HOKJ5j/g4u4OpeGWCxAys5D2jQeCSrgeFSKrOPUvFjO',
        'ROLE_ADMIN');
