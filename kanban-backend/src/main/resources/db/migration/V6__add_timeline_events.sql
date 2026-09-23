-- 팀 활동 타임라인 기능(com.kanban.backend.timeline.TimelineEvent)에 필요한 테이블.
-- type은 TimelineEventType enum을 문자열로 저장(@Enumerated(STRING)).

CREATE TABLE timeline_events (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    type        VARCHAR(30) NOT NULL,
    message     VARCHAR(500) NOT NULL,
    actor_id    BIGINT NOT NULL,
    actor_name  VARCHAR(100) NOT NULL,
    notified    BOOLEAN NOT NULL,
    created_at  DATETIME NOT NULL
);
