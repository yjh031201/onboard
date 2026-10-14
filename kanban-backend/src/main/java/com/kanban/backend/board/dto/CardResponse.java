package com.kanban.backend.board.dto;

import com.kanban.backend.board.Card;
import java.time.LocalDateTime;
import java.util.List;

public record CardResponse(
        Long id,
        String title,
        String description,
        String status,
        int position,
        List<String> labelIds,
        String customLabel,
        LocalDateTime dueAt,
        Long createdById,
        String createdByName,
        LocalDateTime createdAt
) {
    public static CardResponse from(Card card) {
        return new CardResponse(
                card.getId(),
                card.getTitle(),
                card.getDescription(),
                card.getStatus(),
                card.getPosition(),
                List.copyOf(card.getLabelIds()),
                card.getCustomLabel(),
                card.getDueAt(),
                card.getCreatedById(),
                card.getCreatedByName(),
                card.getCreatedAt()
        );
    }
}
