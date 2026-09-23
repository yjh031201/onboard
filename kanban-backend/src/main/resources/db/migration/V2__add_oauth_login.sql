-- 구글/네이버 소셜 로그인 지원. 소셜 전용 계정은 비밀번호가 없어서 nullable로 바꾸고,
-- 로그인 수단(provider)과 그 제공자 쪽 사용자 id(provider_id)를 저장한다.

ALTER TABLE users MODIFY COLUMN password VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE users ADD COLUMN provider_id VARCHAR(255) NULL;

-- provider_id가 NULL인 LOCAL 계정끼리는 유니크 제약에 안 걸림(MySQL은 NULL을 서로 다른 값으로 취급).
ALTER TABLE users ADD UNIQUE KEY uk_users_provider_provider_id (provider, provider_id);
