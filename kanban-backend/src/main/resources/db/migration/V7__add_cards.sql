-- 칸반 보드 카드 기능(com.kanban.backend.board.Card)에 필요한 테이블.
-- status는 CardStatus enum(TODO/IN_PROGRESS/DONE)을 문자열로 저장(@Enumerated(STRING)).

CREATE TABLE cards (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    position        INT NOT NULL,
    created_by_id   BIGINT NOT NULL,
    created_by_name VARCHAR(100) NOT NULL,
    created_at      DATETIME NOT NULL,
    updated_at      DATETIME NOT NULL
);
