package com.kanban.backend.board;

import com.kanban.backend.board.dto.AttachRequest;
import com.kanban.backend.board.dto.CardResponse;
import com.kanban.backend.board.dto.ImportGithubIssuesRequest;
import com.kanban.backend.user.User;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/cards")
public class CardAttachmentController {

    private final CardAttachmentService cardAttachmentService;

    public CardAttachmentController(CardAttachmentService cardAttachmentService) {
        this.cardAttachmentService = cardAttachmentService;
    }

    @PostMapping("/{cardId}/attachments")
    public ResponseEntity<CardResponse> attach(
            @PathVariable Long projectId,
            @PathVariable Long cardId,
            @Valid @RequestBody AttachRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(cardAttachmentService.attach(projectId, cardId, request, actor));
    }

    @DeleteMapping("/{cardId}/attachments/{attachmentId}")
    public ResponseEntity<CardResponse> detach(
            @PathVariable Long projectId,
            @PathVariable Long cardId,
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(cardAttachmentService.detach(projectId, cardId, attachmentId, actor));
    }

    @PostMapping("/import/github-issues")
    public ResponseEntity<List<CardResponse>> importGithubIssues(
            @PathVariable Long projectId,
            @Valid @RequestBody ImportGithubIssuesRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cardAttachmentService.importGithubIssues(projectId, request, actor));
    }
}
