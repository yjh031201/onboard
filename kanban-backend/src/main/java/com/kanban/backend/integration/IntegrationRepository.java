package com.kanban.backend.integration;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationRepository extends JpaRepository<Integration, Long> {

    Optional<Integration> findByProvider(IntegrationProvider provider);

    void deleteByProvider(IntegrationProvider provider);

    /** 회원 탈퇴 시 — 그 사람 계정으로 받은 토큰이므로 연동째 지운다. */
    void deleteAllByConnectedById(Long connectedById);
}
