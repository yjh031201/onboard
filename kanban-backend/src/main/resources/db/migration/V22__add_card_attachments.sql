-- 카드에 붙인 외부 항목 — GitHub 이슈(가져온 카드의 원본 포함)와 Google Drive 파일 링크.
-- 제목·주소는 첨부할 때 서버가 GitHub/Drive API에서 직접 받아 저장한다(클라이언트가 보낸 주소를 믿지 않음).
-- external_id: GitHub 이슈는 "owner/repo#번호", Drive 파일은 파일 id.
-- 카드가 지워지면 같이 지워진다(card_labels와 같은 방식).
CREATE TABLE card_attachments (
    id               BIGINT        AUTO_INCREMENT PRIMARY KEY,
    card_id          BIGINT        NOT NULL,
    project_id       BIGINT        NOT NULL,
    kind             VARCHAR(20)   NOT NULL,
    external_id      VARCHAR(200)  NOT NULL,
    title            VARCHAR(300)  NOT NULL,
    url              VARCHAR(1000) NOT NULL,
    mime_type        VARCHAR(150)  NULL,
    attached_by_id   BIGINT        NOT NULL,
    attached_by_name VARCHAR(100)  NOT NULL,
    created_at       DATETIME      NOT NULL,
    UNIQUE KEY uk_card_attachments_item (card_id, kind, external_id),
    KEY idx_card_attachments_project (project_id, kind, external_id),
    CONSTRAINT fk_card_attachments_card FOREIGN KEY (card_id) REFERENCES cards (id) ON DELETE CASCADE
) ENGINE=InnoDB;
