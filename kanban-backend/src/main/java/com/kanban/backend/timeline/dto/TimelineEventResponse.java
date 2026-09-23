package com.kanban.backend.timeline.dto;

import com.kanban.backend.timeline.TimelineEvent;
import com.kanban.backend.timeline.TimelineEventType;
import java.time.LocalDateTime;

public record TimelineEventResponse(
        Long id,
        TimelineEventType type,
        String message,
        String actorName,
        boolean notified,
        LocalDateTime createdAt
) {
    public static TimelineEventResponse from(TimelineEvent event) {
        return new TimelineEventResponse(
                event.getId(),
                event.getType(),
                event.getMessage(),
                event.getActorName(),
                event.isNotified(),
                event.getCreatedAt()
        );
    }
}
