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
}
