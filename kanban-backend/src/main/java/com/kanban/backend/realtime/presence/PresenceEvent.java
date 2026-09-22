package com.kanban.backend.realtime.presence;

public record PresenceEvent(Long userId, String userName, PresenceStatus status) {
}
