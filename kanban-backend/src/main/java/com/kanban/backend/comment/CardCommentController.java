package com.kanban.backend.comment;

import com.kanban.backend.comment.dto.CommentRequest;
import com.kanban.backend.comment.dto.CommentResponse;
import com.kanban.backend.user.User;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/cards/{cardId}/comments")
public class CardCommentController {

    private final CardCommentService commentService;

    public CardCommentController(CardCommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> list(
            @PathVariable Long projectId,
            @PathVariable Long cardId,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(commentService.list(projectId, cardId, actor));
    }

    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @PathVariable Long projectId,
            @PathVariable Long cardId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.create(projectId, cardId, request, actor));
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentResponse> update(
            @PathVariable Long projectId,
            @PathVariable Long cardId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(commentService.update(projectId, cardId, commentId, request, actor));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long projectId,
            @PathVariable Long cardId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal User actor
    ) {
        commentService.delete(projectId, cardId, commentId, actor);
        return ResponseEntity.noContent().build();
    }
}
