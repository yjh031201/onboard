package com.kanban.backend.project;

import com.kanban.backend.project.dto.CreateProjectRequest;
import com.kanban.backend.project.dto.ProjectResponse;
import com.kanban.backend.project.dto.UpdateProjectRequest;
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

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /** 내가 멤버인 프로젝트 목록 — 메인화면(프로젝트 선택 화면)이 쓴다. */
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> list(@AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(projectService.listMine(actor));
    }

    /** 새 프로젝트 생성 — 게스트는 불가(서비스에서 검증). 만든 사람이 바로 OWNER가 된다. */
    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request, actor));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> get(@PathVariable Long id, @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(projectService.get(id, actor));
    }

    /** 프로젝트 이름/설명 수정 — OWNER/ADMIN만 (설정 페이지 "일반" 섹션이 쓴다). */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(projectService.update(id, request, actor));
    }

    /** 프로젝트를 읽기 전용으로 전환 — OWNER/ADMIN만. */
    @PostMapping("/{id}/archive")
    public ResponseEntity<ProjectResponse> archive(@PathVariable Long id, @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(projectService.archive(id, actor));
    }

    @PostMapping("/{id}/unarchive")
    public ResponseEntity<ProjectResponse> unarchive(@PathVariable Long id, @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(projectService.unarchive(id, actor));
    }

    /**
     * 프로젝트를 영구 삭제 — OWNER만. 이 프로젝트의 카드·컬럼·라벨·타임라인·멤버십을 전부 지운다
     * (팀원 계정 자체와, 아직 프로젝트별로 나뉘지 않은 일정·파일은 남는다).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User actor) {
        projectService.delete(id, actor);
        return ResponseEntity.noContent().build();
    }
}
