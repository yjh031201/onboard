package com.kanban.backend.project;

import com.kanban.backend.common.ApiException;
import com.kanban.backend.project.dto.InvitationResponse;
import com.kanban.backend.project.dto.InviteMemberRequest;
import com.kanban.backend.project.dto.ProjectMemberResponse;
import com.kanban.backend.project.dto.UpdateMemberRoleRequest;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로젝트별 멤버 목록/초대/권한변경 — 예전에 UserController가 전역 role로 하던 걸 프로젝트 범위로 좁힌 것.
 * 초대(invite)는 바로 멤버가 되는 게 아니라 PENDING 상태로 생기고, 초대받은 사람이 직접
 * accept/decline(acceptInvite/declineInvite)해야 한다 — 그 전까지는 이 프로젝트의 어떤 기능도
 * 쓸 수 없고(ProjectAccessService), 팀원 목록(list)에도 보이지 않는다.
 */
@Service
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessService projectAccessService;
    private final UserRepository userRepository;

    public ProjectMemberService(
            ProjectMemberRepository projectMemberRepository,
            ProjectRepository projectRepository,
            ProjectAccessService projectAccessService,
            UserRepository userRepository
    ) {
        this.projectMemberRepository = projectMemberRepository;
        this.projectRepository = projectRepository;
        this.projectAccessService = projectAccessService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> list(Long projectId, User actor) {
        projectAccessService.requireMember(projectId, actor);
        return projectMemberRepository.findAllByProjectIdAndStatus(projectId, InviteStatus.ACCEPTED).stream()
                .map(member -> ProjectMemberResponse.from(member, findUser(member.getUserId())))
                .toList();
    }

    /** 이미 가입된 사용자에게 이 프로젝트 초대를 보낸다(= PENDING 멤버십 생성). OWNER/ADMIN만. */
    @Transactional
    public ProjectMemberResponse invite(Long projectId, InviteMemberRequest request, User actor) {
        projectAccessService.requireAdmin(projectId, actor);
        User target = findUser(request.userId());

        projectMemberRepository.findByProjectIdAndUserId(projectId, target.getId()).ifPresent(existing -> {
            String message = existing.getStatus() == InviteStatus.ACCEPTED
                    ? "이미 이 프로젝트의 멤버입니다."
                    : "이미 초대를 보냈어요. 상대방의 수락을 기다려주세요.";
            throw new ApiException(HttpStatus.CONFLICT, message);
        });

        ProjectMember member = projectMemberRepository.save(
                new ProjectMember(projectId, target.getId(), request.role(), InviteStatus.PENDING)
        );
        return ProjectMemberResponse.from(member, target);
    }

    /** 멤버 권한 변경 — OWNER/ADMIN만, 이미 수락한 멤버만 대상. */
    @Transactional
    public ProjectMemberResponse changeRole(Long projectId, Long userId, UpdateMemberRoleRequest request, User actor) {
        projectAccessService.requireAdmin(projectId, actor);
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserIdAndStatus(projectId, userId, InviteStatus.ACCEPTED)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "이 프로젝트의 멤버가 아닙니다."));

        member.changeRole(request.role());
        return ProjectMemberResponse.from(member, findUser(userId));
    }

    /** 내가 받은(아직 수락/거절 안 한) 초대 전체 — 프로젝트를 가리지 않고 모은다. */
    @Transactional(readOnly = true)
    public List<InvitationResponse> listMyInvitations(User actor) {
        return projectMemberRepository.findAllByUserIdAndStatus(actor.getId(), InviteStatus.PENDING).stream()
                .map(member -> InvitationResponse.from(member, findProject(member.getProjectId())))
                .toList();
    }

    /** 초대 수락 — PENDING -> ACCEPTED. 본인 초대만. */
    @Transactional
    public InvitationResponse acceptInvitation(Long projectId, User actor) {
        ProjectMember member = findPendingInvitation(projectId, actor);
        member.accept();
        return InvitationResponse.from(member, findProject(projectId));
    }

    /** 초대 거절 — 행 자체를 지운다. 다시 초대받으려면 관리자가 새로 초대해야 한다. 본인 초대만. */
    @Transactional
    public void declineInvitation(Long projectId, User actor) {
        ProjectMember member = findPendingInvitation(projectId, actor);
        projectMemberRepository.delete(member);
    }

    private ProjectMember findPendingInvitation(Long projectId, User actor) {
        return projectMemberRepository.findByProjectIdAndUserIdAndStatus(projectId, actor.getId(), InviteStatus.PENDING)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "받은 초대를 찾을 수 없습니다."));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."));
    }
}
