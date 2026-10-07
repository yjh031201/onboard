package com.kanban.backend.settings;

import com.kanban.backend.settings.dto.ProjectSettingsResponse;
import com.kanban.backend.settings.dto.ProjectSettingsUpdateRequest;
import com.kanban.backend.settings.dto.TeamNameUpdateRequest;
import com.kanban.backend.user.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class ProjectSettingsController {

    private final ProjectSettingsService service;
    private final ProjectDeletionService deletionService;

    public ProjectSettingsController(ProjectSettingsService service, ProjectDeletionService deletionService) {
        this.service = service;
        this.deletionService = deletionService;
    }

    @GetMapping
    public ResponseEntity<ProjectSettingsResponse> get() {
        return ResponseEntity.ok(service.get());
    }

    @PutMapping
    public ResponseEntity<ProjectSettingsResponse> update(
            @Valid @RequestBody ProjectSettingsUpdateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(service.update(request, currentUser));
    }

    /** 팀원 페이지의 팀 이름 변경 (OWNER/ADMIN). */
    @PutMapping("/team-name")
    public ResponseEntity<ProjectSettingsResponse> renameTeam(
            @Valid @RequestBody TeamNameUpdateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(service.renameTeam(request.teamName(), currentUser));
    }

    /** 프로젝트를 읽기 전용으로 전환 (OWNER/ADMIN). */
    @PostMapping("/archive")
    public ResponseEntity<ProjectSettingsResponse> archive(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(service.archive(currentUser));
    }

    @PostMapping("/unarchive")
    public ResponseEntity<ProjectSettingsResponse> unarchive(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(service.unarchive(currentUser));
    }

    /** 프로젝트의 카드·일정·파일·타임라인·설정을 모두 지운다 (OWNER만). 되돌릴 수 없다. */
    @DeleteMapping("/project")
    public ResponseEntity<Void> deleteProject(@AuthenticationPrincipal User currentUser) {
        deletionService.deleteProject(currentUser);
        return ResponseEntity.noContent().build();
    }
}
