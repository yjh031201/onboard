package com.kanban.backend.schedule;

import com.kanban.backend.schedule.dto.ScheduleCreateRequest;
import com.kanban.backend.schedule.dto.ScheduleResponse;
import com.kanban.backend.schedule.dto.ScheduleUpdateRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService service;

    public ScheduleController(ScheduleService service) {
        this.service = service;
    }

    /** 캘린더 월별 조회 — 예: /api/schedules?year=2026&month=9 */
    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> getMonth(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(service.getMonth(year, month));
    }

    @PostMapping
    public ResponseEntity<ScheduleResponse> create(
            @Valid @RequestBody ScheduleCreateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, currentUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleUpdateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(service.update(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        service.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
