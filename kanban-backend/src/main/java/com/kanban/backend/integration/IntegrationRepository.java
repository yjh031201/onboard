package com.kanban.backend.integration;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationRepository extends JpaRepository<Integration, Long> {

    Optional<Integration> findByProjectIdAndProvider(Long projectId, IntegrationProvider provider);

    List<Integration> findAllByProjectId(Long projectId);

    void deleteByProjectIdAndProvider(Long projectId, IntegrationProvider provider);

    void deleteAllByProjectId(Long projectId);

    /** 회원 탈퇴 시 — 그 사람 계정으로 받은 토큰이므로, 어느 프로젝트에 연결했든 연동째 지운다. */
    void deleteAllByConnectedById(Long connectedById);
}
