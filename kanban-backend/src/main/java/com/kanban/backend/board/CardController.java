package com.kanban.backend.board;

import com.kanban.backend.board.dto.CardResponse;
import com.kanban.backend.board.dto.ChangeCardLabelRequest;
import com.kanban.backend.board.dto.CreateCardRequest;
import com.kanban.backend.board.dto.MoveCardRequest;
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
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping
    public ResponseEntity<List<CardResponse>> list() {
        return ResponseEntity.ok(cardService.list());
    }

    @PostMapping
    public ResponseEntity<CardResponse> create(
            @Valid @RequestBody CreateCardRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.create(request, actor));
    }

    @PatchMapping("/{id}/move")
    public ResponseEntity<CardResponse> move(
            @PathVariable Long id,
            @Valid @RequestBody MoveCardRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(cardService.move(id, request, actor));
    }

    @PatchMapping("/{id}/label")
    public ResponseEntity<CardResponse> changeLabel(
            @PathVariable Long id,
            @Valid @RequestBody ChangeCardLabelRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(cardService.changeLabel(id, request, actor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User actor) {
        cardService.delete(id, actor);
        return ResponseEntity.noContent().build();
    }
}
