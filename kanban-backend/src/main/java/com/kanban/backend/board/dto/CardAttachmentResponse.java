package com.kanban.backend.board.dto;

import com.kanban.backend.board.AttachmentKind;
import com.kanban.backend.board.CardAttachment;

public record CardAttachmentResponse(
        Long id,
        AttachmentKind kind,
        String externalId,
        String title,
        String url,
        String mimeType,
        Long attachedById,
        String attachedByName
) {
    public static CardAttachmentResponse from(CardAttachment attachment) {
        return new CardAttachmentResponse(
                attachment.getId(),
                attachment.getKind(),
                attachment.getExternalId(),
                attachment.getTitle(),
                attachment.getUrl(),
                attachment.getMimeType(),
                attachment.getAttachedById(),
                attachment.getAttachedByName()
        );
    }
}
