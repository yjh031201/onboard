package com.kanban.backend.label;

import com.kanban.backend.label.dto.LabelRequest;
import com.kanban.backend.label.dto.LabelResponse;
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

/** 카드 라벨 관리 — 이 프로젝트의 멤버 누구나 추가/편집/삭제할 수 있다. */
@RestController
@RequestMapping("/api/projects/{projectId}/labels")
public class LabelController {

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @GetMapping
    public ResponseEntity<List<LabelResponse>> list(@PathVariable Long projectId, @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(labelService.list(projectId, actor));
    }

    @PostMapping
    public ResponseEntity<LabelResponse> create(
            @PathVariable Long projectId,
            @Valid @RequestBody LabelRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(labelService.create(projectId, request, actor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LabelResponse> update(
            @PathVariable Long projectId,
            @PathVariable String id,
            @Valid @RequestBody LabelRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(labelService.update(projectId, id, request, actor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long projectId,
            @PathVariable String id,
            @AuthenticationPrincipal User actor
    ) {
        labelService.delete(projectId, id, actor);
        return ResponseEntity.noContent().build();
    }
}
