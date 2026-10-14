package com.kanban.backend.project;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    Optional<ProjectMember> findByProjectIdAndUserIdAndStatus(Long projectId, Long userId, InviteStatus status);

    List<ProjectMember> findAllByProjectId(Long projectId);

    List<ProjectMember> findAllByProjectIdAndStatus(Long projectId, InviteStatus status);

    List<ProjectMember> findAllByUserId(Long userId);

    List<ProjectMember> findAllByUserIdAndStatus(Long userId, InviteStatus status);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    void deleteAllByProjectId(Long projectId);

    /** 회원 탈퇴 시 — 수락한 멤버십과 아직 답하지 않은(PENDING) 초대를 모두 지운다. */
    void deleteAllByUserId(Long userId);
}
