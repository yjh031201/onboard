package com.kanban.backend.integration;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationRepository extends JpaRepository<Integration, Long> {

    Optional<Integration> findByProjectIdAndProvider(Long projectId, IntegrationProvider provider);

    List<Integration> findAllByProjectId(Long projectId);

    void deleteByProjectIdAndProvider(Long projectId, IntegrationProvider provider);

    void deleteAllByProjectId(Long projectId);
}
