package com.kanban.backend.project;

import com.kanban.backend.project.dto.InvitationResponse;
import com.kanban.backend.user.User;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내가 받은 프로젝트 초대 — 메인화면 알림 벨과 "나에게 온 초대" 섹션이 쓴다.
 * 초대를 보내는 쪽(관리자)은 ProjectMemberController.invite()를 그대로 쓴다.
 */
@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private final ProjectMemberService projectMemberService;

    public InvitationController(ProjectMemberService projectMemberService) {
        this.projectMemberService = projectMemberService;
    }

    @GetMapping
    public ResponseEntity<List<InvitationResponse>> list(@AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(projectMemberService.listMyInvitations(actor));
    }

    @PostMapping("/{projectId}/accept")
    public ResponseEntity<InvitationResponse> accept(@PathVariable Long projectId, @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(projectMemberService.acceptInvitation(projectId, actor));
    }

    @PostMapping("/{projectId}/decline")
    public ResponseEntity<Void> decline(@PathVariable Long projectId, @AuthenticationPrincipal User actor) {
        projectMemberService.declineInvitation(projectId, actor);
        return ResponseEntity.noContent().build();
    }
}
