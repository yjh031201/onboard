package com.kanban.backend.timeline.dto;

/** Broadcast on /topic/timeline-deleted so every client drops the entry from its feed. */
public record TimelineEventDeleted(Long id) {
}
