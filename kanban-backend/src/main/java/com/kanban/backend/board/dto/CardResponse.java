package com.kanban.backend.board.dto;

import com.kanban.backend.board.Card;
import com.kanban.backend.board.CardStatus;
import java.time.LocalDateTime;

public record CardResponse(
        Long id,
        String title,
        CardStatus status,
        int position,
        String createdByName,
        LocalDateTime createdAt
) {
    public static CardResponse from(Card card) {
        return new CardResponse(
                card.getId(),
                card.getTitle(),
                card.getStatus(),
                card.getPosition(),
                card.getCreatedByName(),
                card.getCreatedAt()
        );
    }
}
