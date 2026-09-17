-- 이 프로젝트의 첫 Flyway 마이그레이션. 지금까지 Hibernate ddl-auto=update로
-- 만들어졌던 테이블들을 그대로 옮겨온 것 — 컬럼 정의는 각 엔티티 클래스와 맞춰서 관리할 것.

CREATE TABLE users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    name       VARCHAR(100) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'MEMBER',
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE project_settings (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    project_name VARCHAR(100) NOT NULL,
    description  TEXT,
    updated_by   BIGINT,
    updated_at   DATETIME     NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE schedules (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    title         VARCHAR(200) NOT NULL,
    content       TEXT,
    schedule_date DATE         NOT NULL,
    created_by    BIGINT       NOT NULL,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_schedule_date ON schedules (schedule_date);

CREATE TABLE project_files (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    file_name     VARCHAR(255)  NOT NULL,
    file_key      VARCHAR(500)  NOT NULL,
    file_size     BIGINT        NOT NULL,
    content_type  VARCHAR(100),
    uploaded_by   BIGINT        NOT NULL,
    created_at    DATETIME      NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
