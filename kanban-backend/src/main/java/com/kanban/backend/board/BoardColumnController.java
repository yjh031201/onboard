package com.kanban.backend.board;

import com.kanban.backend.board.dto.ColumnOrderRequest;
import com.kanban.backend.board.dto.ColumnRequest;
import com.kanban.backend.board.dto.ColumnResponse;
import com.kanban.backend.user.User;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 칸반 보드 컬럼 관리 — 이 프로젝트의 멤버 누구나 추가/이름 변경/삭제할 수 있다. */
@RestController
@RequestMapping("/api/projects/{projectId}/board-columns")
public class BoardColumnController {

    private final BoardColumnService columnService;

    public BoardColumnController(BoardColumnService columnService) {
        this.columnService = columnService;
    }

    @GetMapping
    public ResponseEntity<List<ColumnResponse>> list(@PathVariable Long projectId, @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(columnService.list(projectId, actor));
    }

    @PostMapping
    public ResponseEntity<ColumnResponse> create(
            @PathVariable Long projectId,
            @Valid @RequestBody ColumnRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(columnService.create(projectId, request, actor));
    }

    /** "/order"가 "/{id}"보다 구체적인 경로라 먼저 매칭된다. */
    @PutMapping("/order")
    public ResponseEntity<List<ColumnResponse>> reorder(
            @PathVariable Long projectId,
            @Valid @RequestBody ColumnOrderRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(columnService.reorder(projectId, request, actor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ColumnResponse> update(
            @PathVariable Long projectId,
            @PathVariable String id,
            @Valid @RequestBody ColumnRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(columnService.update(projectId, id, request, actor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long projectId,
            @PathVariable String id,
            @AuthenticationPrincipal User actor
    ) {
        columnService.delete(projectId, id, actor);
        return ResponseEntity.noContent().build();
    }
}
