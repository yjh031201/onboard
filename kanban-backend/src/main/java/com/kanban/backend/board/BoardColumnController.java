package com.kanban.backend.board;

import com.kanban.backend.board.dto.ColumnOrderRequest;
import com.kanban.backend.board.dto.ColumnRequest;
import com.kanban.backend.board.dto.ColumnResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 칸반 보드 컬럼 관리 — 팀원 누구나 추가/이름 변경/삭제할 수 있다. */
@RestController
@RequestMapping("/api/columns")
public class BoardColumnController {

    private final BoardColumnService columnService;

    public BoardColumnController(BoardColumnService columnService) {
        this.columnService = columnService;
    }

    @GetMapping
    public ResponseEntity<List<ColumnResponse>> list() {
        return ResponseEntity.ok(columnService.list());
    }

    @PostMapping
    public ResponseEntity<ColumnResponse> create(@Valid @RequestBody ColumnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(columnService.create(request));
    }

    /** "/order"가 "/{id}"보다 구체적인 경로라 먼저 매칭된다. */
    @PutMapping("/order")
    public ResponseEntity<List<ColumnResponse>> reorder(@Valid @RequestBody ColumnOrderRequest request) {
        return ResponseEntity.ok(columnService.reorder(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ColumnResponse> update(@PathVariable String id, @Valid @RequestBody ColumnRequest request) {
        return ResponseEntity.ok(columnService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        columnService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
