-- 카드 설명 — 제목만으로 부족한 세부 내용을 적는다. 없으면 NULL. 길이 제한(2000자)은 UpdateCardRequest에서 검사.
ALTER TABLE cards ADD COLUMN description TEXT NULL;
