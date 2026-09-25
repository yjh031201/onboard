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

/** 카드 라벨 관리 — 팀원 누구나 추가/편집/삭제할 수 있다. */
@RestController
@RequestMapping("/api/labels")
public class LabelController {

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @GetMapping
    public ResponseEntity<List<LabelResponse>> list() {
        return ResponseEntity.ok(labelService.list());
    }

    @PostMapping
    public ResponseEntity<LabelResponse> create(@Valid @RequestBody LabelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(labelService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LabelResponse> update(@PathVariable String id, @Valid @RequestBody LabelRequest request) {
        return ResponseEntity.ok(labelService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, @AuthenticationPrincipal User actor) {
        labelService.delete(id, actor);
        return ResponseEntity.noContent().build();
    }
}
