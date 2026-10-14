-- "기타" 라벨에 붙인 카드마다 직접 적는 라벨 글자. 안 적었으면 NULL이고 화면에는 라벨 이름이 보인다.
-- 길이 제한(20자)은 요청 DTO에서 검사.
ALTER TABLE cards ADD COLUMN custom_label VARCHAR(20) NULL;

-- "기타" 라벨은 프로젝트마다 랜덤 id로 시드되어 있어(ProjectService.seedDefaults) 고정 id로 찾을 수 없다.
-- 이 플래그로 프로젝트별 "기타" 라벨을 찾는다. 이미 시드된 프로젝트의 "기타" 라벨은 이름으로 식별해 백필한다.
ALTER TABLE labels ADD COLUMN is_etc BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE labels SET is_etc = TRUE WHERE name = '기타';
