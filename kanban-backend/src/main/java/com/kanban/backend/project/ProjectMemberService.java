package com.kanban.backend.project;

import com.kanban.backend.common.ApiException;
import com.kanban.backend.project.dto.InviteMemberRequest;
import com.kanban.backend.project.dto.ProjectMemberResponse;
import com.kanban.backend.project.dto.UpdateMemberRoleRequest;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 프로젝트별 멤버 목록/초대/권한변경 — 예전에 UserController가 전역 role로 하던 걸 프로젝트 범위로 좁힌 것. */
@Service
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectAccessService projectAccessService;
    private final UserRepository userRepository;

    public ProjectMemberService(
            ProjectMemberRepository projectMemberRepository,
            ProjectAccessService projectAccessService,
            UserRepository userRepository
    ) {
        this.projectMemberRepository = projectMemberRepository;
        this.projectAccessService = projectAccessService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> list(Long projectId, User actor) {
        projectAccessService.requireMember(projectId, actor);
        return projectMemberRepository.findAllByProjectId(projectId).stream()
                .map(member -> ProjectMemberResponse.from(member, findUser(member.getUserId())))
                .toList();
    }

    /** 이미 가입된 사용자를 이 프로젝트의 멤버로 추가 (= 초대). OWNER/ADMIN만. */
    @Transactional
    public ProjectMemberResponse invite(Long projectId, InviteMemberRequest request, User actor) {
        projectAccessService.requireAdmin(projectId, actor);
        User target = findUser(request.userId());

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, target.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 이 프로젝트의 멤버입니다.");
        }

        ProjectMember member = projectMemberRepository.save(
                new ProjectMember(projectId, target.getId(), request.role())
        );
        return ProjectMemberResponse.from(member, target);
    }

    /** 멤버 권한 변경 — OWNER/ADMIN만. */
    @Transactional
    public ProjectMemberResponse changeRole(Long projectId, Long userId, UpdateMemberRoleRequest request, User actor) {
        projectAccessService.requireAdmin(projectId, actor);
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "이 프로젝트의 멤버가 아닙니다."));

        member.changeRole(request.role());
        return ProjectMemberResponse.from(member, findUser(userId));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }
}
