-- 카드 라벨(com.kanban.backend.label.Label) — 설정 페이지에서 추가/편집/삭제한다.
-- id는 cards.label_id가 가리키는 문자열 키. 기존에 프론트에 하드코딩돼 있던 4개를 기본값으로 넣는다.

CREATE TABLE labels (
    id          VARCHAR(30) PRIMARY KEY,
    name        VARCHAR(20) NOT NULL,
    color       VARCHAR(7) NOT NULL,
    position    INT NOT NULL,
    created_at  DATETIME NOT NULL,
    updated_at  DATETIME NOT NULL
);

INSERT INTO labels (id, name, color, position, created_at, updated_at) VALUES
    ('bug', '버그', '#ef4444', 0, NOW(), NOW()),
    ('feature', '기능', '#6366f1', 1, NOW(), NOW()),
    ('design', '디자인', '#a855f7', 2, NOW(), NOW()),
    ('urgent', '긴급', '#f59e0b', 3, NOW(), NOW());
