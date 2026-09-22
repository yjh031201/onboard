package com.kanban.backend.board.dto;

import java.util.List;

/**
 * Broadcast on every card create/move as a full snapshot of the board.
 * Simpler and more robust than diff-based patches for a board this size,
 * and it means a client that reconnects mid-drag still converges to the
 * same state as everyone else on the very next event.
 */
public record BoardEvent(
        String type,
        List<CardResponse> cards,
        String actorName
) {
}
