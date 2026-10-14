package com.kanban.backend.realtime.presence;

import com.kanban.backend.project.InviteStatus;
import com.kanban.backend.project.ProjectAccessService;
import com.kanban.backend.project.ProjectMember;
import com.kanban.backend.project.ProjectMemberRepository;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRepository;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/presence")
public class PresenceController {

    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectAccessService projectAccessService;
    private final PresenceService presenceService;

    public PresenceController(
            UserRepository userRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectAccessService projectAccessService,
            PresenceService presenceService
    ) {
        this.userRepository = userRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectAccessService = projectAccessService;
        this.presenceService = presenceService;
    }

    /**
     * 이 프로젝트 멤버(수락 완료)만의 접속 현황 — 위젯의 초기 렌더용(이후는 /topic/projects/{id}/presence로 받음).
     * 예전엔 가입된 전체 사용자를 돌려줘서 다른 프로젝트 멤버까지 섞여 보였는데, 이제 이 프로젝트의
     * project_members(ACCEPTED)만 본다.
     */
    @GetMapping
    public ResponseEntity<List<PresenceEvent>> roster(@PathVariable Long projectId, @AuthenticationPrincipal User actor) {
        projectAccessService.requireMember(projectId, actor);

        Set<Long> onlineUserIds = presenceService.onlineUserIds();
        List<ProjectMember> members = projectMemberRepository.findAllByProjectIdAndStatus(projectId, InviteStatus.ACCEPTED);

        List<PresenceEvent> roster = members.stream()
                .map(member -> toPresenceEvent(member, onlineUserIds))
                .toList();

        return ResponseEntity.ok(roster);
    }

    private PresenceEvent toPresenceEvent(ProjectMember member, Set<Long> onlineUserIds) {
        User user = userRepository.findById(member.getUserId()).orElse(null);
        String name = user == null ? "알 수 없음" : user.getName();
        PresenceStatus status = onlineUserIds.contains(member.getUserId()) ? PresenceStatus.ONLINE : PresenceStatus.OFFLINE;
        return new PresenceEvent(member.getUserId(), name, status);
    }
}
