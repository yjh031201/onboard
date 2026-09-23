-- 소셜 로그인 성공 후 access/refresh token을 1회용 코드로 교환하기 위한 테이블.
-- 코드 원본은 저장하지 않고 SHA-256 해시만 저장한다.

CREATE TABLE oauth_exchange_codes (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    code_hash   VARCHAR(64) NOT NULL,
    expires_at  DATETIME    NOT NULL,
    redeemed_at DATETIME    NULL,
    created_at  DATETIME    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_oauth_exchange_codes_code_hash (code_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
