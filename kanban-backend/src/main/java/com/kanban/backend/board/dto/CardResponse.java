package com.kanban.backend.board.dto;

import com.kanban.backend.board.Card;
import java.time.LocalDateTime;

public record CardResponse(
        Long id,
        String title,
        String status,
        int position,
        String labelId,
        Long createdById,
        String createdByName,
        LocalDateTime createdAt
) {
    public static CardResponse from(Card card) {
        return new CardResponse(
                card.getId(),
                card.getTitle(),
                card.getStatus(),
                card.getPosition(),
                card.getLabelId(),
                card.getCreatedById(),
                card.getCreatedByName(),
                card.getCreatedAt()
        );
    }
}
