package com.kanban.backend.settings;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectSettingsRepository extends JpaRepository<ProjectSettings, Long> {

    Optional<ProjectSettings> findTopByOrderByIdAsc();
}
