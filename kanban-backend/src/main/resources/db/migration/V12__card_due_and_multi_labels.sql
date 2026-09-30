-- 1) 카드 마감 일시 — 대시보드 캘린더에도 표시된다. 마감이 없는 카드는 NULL.
ALTER TABLE cards ADD COLUMN due_at DATETIME NULL;

-- 2) 카드 하나에 라벨을 최대 2개까지 — cards.label_id(1개) 대신 연결 테이블로 옮긴다.
--    Card.labelIds(@ElementCollection + @OrderColumn)가 이 테이블을 쓴다. 개수 제한은 CardService에서 검사.
CREATE TABLE card_labels (
    card_id   BIGINT      NOT NULL,
    position  INT         NOT NULL,
    label_id  VARCHAR(30) NOT NULL,
    PRIMARY KEY (card_id, position),
    CONSTRAINT fk_card_labels_card FOREIGN KEY (card_id) REFERENCES cards (id) ON DELETE CASCADE
);

INSERT INTO card_labels (card_id, position, label_id)
SELECT id, 0, label_id FROM cards WHERE label_id IS NOT NULL;

ALTER TABLE cards DROP COLUMN label_id;

-- 3) 처음부터 쓸 수 있는 "기타" 라벨. 같은 이름이 이미 있으면 넣지 않는다.
INSERT INTO labels (id, name, color, position, created_at, updated_at)
SELECT 'etc', '기타', '#9ca3af', t.next_position, NOW(), NOW()
FROM (
    SELECT COALESCE(MAX(position), -1) + 1 AS next_position,
           COALESCE(SUM(id = 'etc' OR name = '기타'), 0) AS existing
    FROM labels
) t
WHERE t.existing = 0;
