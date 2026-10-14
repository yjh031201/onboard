-- 멀티 프로젝트 지원: 한 사람이 여러 Project를 가질 수 있게 되면서,
-- cards/board_columns/labels/timeline_events 전부 project_id로 구분해야 한다.
-- 기존 데이터는 전부 새로 만든 프로젝트 #1로 백필되고, 가입된 모든 유저가 그 프로젝트의
-- 멤버(기존 role 유지)로 들어간다. project_settings는 Project가 흡수했으므로 마지막에 drop.

-- 1) projects / project_members
CREATE TABLE projects (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE project_members (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    project_id BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    role       VARCHAR(20) NOT NULL,
    joined_at  DATETIME    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_members_project_user (project_id, user_id),
    CONSTRAINT fk_project_members_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_project_members_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_project_members_user ON project_members (user_id);

-- 2) 게스트 식별 플래그 (Phase 5에서 사용 — 지금 같이 넣어서 마이그레이션 왕복을 줄인다)
ALTER TABLE users ADD COLUMN is_guest BOOLEAN NOT NULL DEFAULT FALSE;

-- 3) 기존 데이터를 위한 프로젝트 #1 — project_settings에 값이 있으면 그걸 쓰고, 없으면 기본값.
INSERT INTO projects (name, description, created_at, updated_at)
SELECT
    COALESCE((SELECT project_name FROM project_settings LIMIT 1), '기본 프로젝트'),
    (SELECT description FROM project_settings LIMIT 1),
    NOW(),
    NOW();

SET @default_project_id = LAST_INSERT_ID();

-- 4) 가입된 모든 유저를 프로젝트 #1의 멤버로 백필 (기존 전역 role 유지)
INSERT INTO project_members (project_id, user_id, role, joined_at)
SELECT @default_project_id, id, role, NOW() FROM users;

-- 5) cards / board_columns / labels / timeline_events에 project_id 추가 + 백필 + NOT NULL + FK
ALTER TABLE board_columns ADD COLUMN project_id BIGINT NULL;
UPDATE board_columns SET project_id = @default_project_id;
ALTER TABLE board_columns MODIFY COLUMN project_id BIGINT NOT NULL;
ALTER TABLE board_columns ADD CONSTRAINT fk_board_columns_project FOREIGN KEY (project_id) REFERENCES projects (id);
CREATE INDEX idx_board_columns_project ON board_columns (project_id);

ALTER TABLE labels ADD COLUMN project_id BIGINT NULL;
UPDATE labels SET project_id = @default_project_id;
ALTER TABLE labels MODIFY COLUMN project_id BIGINT NOT NULL;
ALTER TABLE labels ADD CONSTRAINT fk_labels_project FOREIGN KEY (project_id) REFERENCES projects (id);
CREATE INDEX idx_labels_project ON labels (project_id);

ALTER TABLE cards ADD COLUMN project_id BIGINT NULL;
UPDATE cards SET project_id = @default_project_id;
ALTER TABLE cards MODIFY COLUMN project_id BIGINT NOT NULL;
ALTER TABLE cards ADD CONSTRAINT fk_cards_project FOREIGN KEY (project_id) REFERENCES projects (id);
CREATE INDEX idx_cards_project ON cards (project_id);

ALTER TABLE timeline_events ADD COLUMN project_id BIGINT NULL;
UPDATE timeline_events SET project_id = @default_project_id;
ALTER TABLE timeline_events MODIFY COLUMN project_id BIGINT NOT NULL;
ALTER TABLE timeline_events ADD CONSTRAINT fk_timeline_events_project FOREIGN KEY (project_id) REFERENCES projects (id);
CREATE INDEX idx_timeline_events_project ON timeline_events (project_id);

-- 6) project_settings는 Project가 흡수했으므로 더 이상 필요 없음.
DROP TABLE project_settings;
