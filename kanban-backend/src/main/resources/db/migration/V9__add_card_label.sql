-- 카드에 붙인 라벨(설정 페이지의 라벨 id: bug/feature/design/urgent 등). 라벨 없는 카드는 NULL.

ALTER TABLE cards ADD COLUMN label_id VARCHAR(30) NULL;
