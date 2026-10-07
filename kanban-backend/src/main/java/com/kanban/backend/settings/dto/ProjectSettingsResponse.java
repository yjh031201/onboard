package com.kanban.backend.settings.dto;

import com.kanban.backend.settings.ProjectSettings;
import java.time.LocalDateTime;

public record ProjectSettingsResponse(
        Long id,
        String projectName,
        String description,
        String teamName,
        Long updatedBy,
        LocalDateTime updatedAt,
        boolean archived,
        LocalDateTime archivedAt
) {
    public static ProjectSettingsResponse from(ProjectSettings settings) {
        return new ProjectSettingsResponse(
                settings.getId(),
                settings.getProjectName(),
                settings.getDescription(),
                settings.getTeamName() == null ? "" : settings.getTeamName(),
                settings.getUpdatedBy(),
                settings.getUpdatedAt(),
                settings.isArchived(),
                settings.getArchivedAt()
        );
    }

    /** 아직 아무도 설정을 저장하지 않은 초기 상태. */
    public static ProjectSettingsResponse empty() {
        return new ProjectSettingsResponse(null, "", "", "", null, null, false, null);
    }
}
