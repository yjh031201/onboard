-- "기타" 라벨(labels.id = 'etc')을 붙인 카드에 직접 적는 라벨 글자. 안 적었으면 NULL이고 화면에는 라벨 이름이 보인다.
-- 길이 제한(20자)은 요청 DTO에서 검사.
ALTER TABLE cards ADD COLUMN custom_label VARCHAR(20) NULL;

-- 직접 입력은 '기타' 라벨에 딸린 기능이라 그 라벨이 항상 있어야 한다 — V12 이후 설정에서 지웠으면 다시 넣는다.
INSERT INTO labels (id, name, color, position, created_at, updated_at)
SELECT 'etc', '기타', '#9ca3af', t.next_position, NOW(), NOW()
FROM (
    SELECT COALESCE(MAX(position), -1) + 1 AS next_position,
           COALESCE(SUM(id = 'etc'), 0) AS existing
    FROM labels
) t
WHERE t.existing = 0;
