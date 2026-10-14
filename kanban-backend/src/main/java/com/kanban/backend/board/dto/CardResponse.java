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
        Long assigneeId,
        String assigneeName,
        Long createdById,
        String createdByName,
        LocalDateTime createdAt,
        /** 붙인 GitHub 이슈·Drive 파일, 붙인 순서대로. */
        List<CardAttachmentResponse> attachments
) {
    public static CardResponse from(Card card, List<CardAttachmentResponse> attachments) {
        return new CardResponse(
                card.getId(),
                card.getTitle(),
                card.getDescription(),
                card.getStatus(),
                card.getPosition(),
                List.copyOf(card.getLabelIds()),
                card.getCustomLabel(),
                card.getDueAt(),
                card.getAssigneeId(),
                card.getAssigneeName(),
                card.getCreatedById(),
                card.getCreatedByName(),
                card.getCreatedAt(),
                List.copyOf(attachments)
        );
    }
}
