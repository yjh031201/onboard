package com.kanban.backend.project;

import com.kanban.backend.common.ApiException;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 프로젝트 범위 권한 체크 공통 헬퍼. 예전에 각 서비스가 전역 user.getRole()을 직접 보던 걸
 * 전부 이걸로 교체한다 — "이 프로젝트에서 이 사람 role이 뭔지"는 ProjectMember에만 있다.
 *
 * 초대는 보냈지만 아직 수락 안 한(PENDING) 사람은 멤버로 치지 않는다 — 보드/라벨/타임라인 등
 * 어떤 프로젝트 리소스에도 접근 못 하고, 초대를 수락(InvitationController)해야 비로소 멤버가 된다.
 */
@Service
public class ProjectAccessService {

    private final ProjectMemberRepository projectMemberRepository;

    public ProjectAccessService(ProjectMemberRepository projectMemberRepository) {
        this.projectMemberRepository = projectMemberRepository;
    }

    /** 이 프로젝트의 (수락한) 멤버인지 확인하고 그 멤버십을 반환 — 아니면 403. */
    public ProjectMember requireMember(Long projectId, User user) {
        return projectMemberRepository.findByProjectIdAndUserIdAndStatus(projectId, user.getId(), InviteStatus.ACCEPTED)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "이 프로젝트의 멤버가 아닙니다."));
    }

    /** 이 프로젝트에서 OWNER/ADMIN인지 확인 — 아니면 403. */
    public ProjectMember requireAdmin(Long projectId, User user) {
        ProjectMember member = requireMember(projectId, user);
        if (member.getRole() == UserRole.MEMBER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "이 작업은 프로젝트의 소유자/관리자만 할 수 있습니다.");
        }
        return member;
    }

    /** 이 프로젝트에서 내 role — (수락한) 멤버가 아니면 null (화면 표시용, 예외를 던지지 않음). */
    public UserRole myRoleOrNull(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserIdAndStatus(projectId, userId, InviteStatus.ACCEPTED)
                .map(ProjectMember::getRole)
                .orElse(null);
    }
}
