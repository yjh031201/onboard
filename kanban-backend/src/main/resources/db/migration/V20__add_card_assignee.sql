-- 카드 담당자 — 카드에는 작성자 대신 담당자가 보인다. 지정 안 했으면 둘 다 NULL.
-- 이름은 작성자(created_by_name)처럼 카드에 복사해 두고, 회원 탈퇴 시 AccountDeletionService에서 바꾼다.
ALTER TABLE cards ADD COLUMN assignee_id BIGINT NULL;
ALTER TABLE cards ADD COLUMN assignee_name VARCHAR(100) NULL;
