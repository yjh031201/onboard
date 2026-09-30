-- 칸반 보드 컬럼(com.kanban.backend.board.BoardColumn) — 설정 페이지에서 추가/이름 변경/삭제한다.
-- 기존에 CardStatus enum으로 고정돼 있던 3개를 같은 id로 넣어서, cards.status 값은 그대로 컬럼 id로 쓴다.

CREATE TABLE board_columns (
    id          VARCHAR(20) PRIMARY KEY,
    name        VARCHAR(30) NOT NULL,
    color       VARCHAR(7) NOT NULL,
    position    INT NOT NULL,
    created_at  DATETIME NOT NULL,
    updated_at  DATETIME NOT NULL
);

INSERT INTO board_columns (id, name, color, position, created_at, updated_at) VALUES
    ('TODO', '할 일', '#94a3b8', 0, NOW(), NOW()),
    ('IN_PROGRESS', '진행 중', '#f59e0b', 1, NOW(), NOW()),
    ('DONE', '완료', '#10b981', 2, NOW(), NOW());
