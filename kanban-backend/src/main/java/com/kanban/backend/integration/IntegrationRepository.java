package com.kanban.backend.integration;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationRepository extends JpaRepository<Integration, Long> {

    Optional<Integration> findByProvider(IntegrationProvider provider);

    void deleteByProvider(IntegrationProvider provider);
}
