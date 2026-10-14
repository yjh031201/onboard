-- 카드 댓글 — 카드 상세 창 아래에서 팀원끼리 이야기를 나눈다.
-- 작성자 이름은 카드 작성자(created_by_name)처럼 복사해 두고, 회원 탈퇴 시 AccountDeletionService에서 바꾼다.
-- 카드·프로젝트가 지워질 때 댓글은 CardService.delete / ProjectService.delete에서 같이 지운다. 길이 제한(1000자)은 요청 DTO에서 검사.
CREATE TABLE card_comments (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT NOT NULL,
    card_id     BIGINT NOT NULL,
    author_id   BIGINT NOT NULL,
    author_name VARCHAR(100) NOT NULL,
    content     TEXT NOT NULL,
    created_at  DATETIME NOT NULL,
    updated_at  DATETIME NOT NULL
);

CREATE INDEX idx_card_comments_card ON card_comments (card_id);
CREATE INDEX idx_card_comments_project ON card_comments (project_id);
