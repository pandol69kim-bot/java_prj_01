-- V6: admin 계정 비밀번호 수정 및 누락 컬럼 보완
-- 기존 V1 INSERT의 BCrypt 해시가 admin1234와 불일치하는 문제 수정
-- 신규 해시: admin1234 (BCryptPasswordEncoder cost=10, 검증 완료)

UPDATE users
SET password   = '$2a$10$hePhStQldwwrZrUBTNHiF.ZIs0GN2wufH8mI4MoUEBwr6bro4C4Xm',
    enabled    = true,
    updated_at = NOW()
WHERE username = 'admin';

-- admin 계정이 없는 경우(V1 INSERT 실패 등) 재삽입
INSERT INTO users (username, email, password, role, enabled, created_at, updated_at)
SELECT 'admin',
       'admin@example.com',
       '$2a$10$hePhStQldwwrZrUBTNHiF.ZIs0GN2wufH8mI4MoUEBwr6bro4C4Xm',
       'ROLE_ADMIN',
       true,
       NOW(),
       NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');
