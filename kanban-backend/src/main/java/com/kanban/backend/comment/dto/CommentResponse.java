package com.kanban.backend.comment.dto;

import com.kanban.backend.comment.CardComment;
import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        Long cardId,
        Long authorId,
        String authorName,
        String content,
        LocalDateTime createdAt,
        boolean edited
) {
    public static CommentResponse from(CardComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getCardId(),
                comment.getAuthorId(),
                comment.getAuthorName(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.isEdited()
        );
    }
}
