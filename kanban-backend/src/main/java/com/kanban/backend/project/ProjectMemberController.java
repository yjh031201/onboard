package com.kanban.backend.project;

import com.kanban.backend.project.dto.InviteMemberRequest;
import com.kanban.backend.project.dto.ProjectMemberResponse;
import com.kanban.backend.project.dto.UpdateMemberRoleRequest;
import com.kanban.backend.user.User;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/members")
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    public ProjectMemberController(ProjectMemberService projectMemberService) {
        this.projectMemberService = projectMemberService;
    }

    @GetMapping
    public ResponseEntity<List<ProjectMemberResponse>> list(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(projectMemberService.list(projectId, actor));
    }

    /** 이미 가입된 사용자(검색은 그대로 전역 /api/users/search 사용)를 이 프로젝트 멤버로 추가. */
    @PostMapping
    public ResponseEntity<ProjectMemberResponse> invite(
            @PathVariable Long projectId,
            @Valid @RequestBody InviteMemberRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectMemberService.invite(projectId, request, actor));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<ProjectMemberResponse> changeRole(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(projectMemberService.changeRole(projectId, userId, request, actor));
    }
}
