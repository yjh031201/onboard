package com.kanban.backend.timeline;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A single line of team activity history — "누가 무엇을 했다" — persisted so
 * the 타임라인 page has history beyond whatever clients happen to be
 * connected when it happens, and broadcast live over WebSocket as it's created.
 */
@Entity
@Table(name = "timeline_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TimelineEventType type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "actor_name", nullable = false, length = 100)
    private String actorName;

    @Column(nullable = false)
    private boolean notified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public TimelineEvent(Long projectId, TimelineEventType type, String message, Long actorId, String actorName, boolean notified) {
        this.projectId = projectId;
        this.type = type;
        this.message = message;
        this.actorId = actorId;
        this.actorName = actorName;
        this.notified = notified;
    }

    // isDeletableBy(User)는 프로젝트별 권한 체크로 바뀌어서 TimelineService.isManageable(...)로 이동했다
    // (전역 User.role이 아니라 ProjectMember.role을 봐야 하기 때문).

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
