package com.kanban.backend.settings.dto;

import com.kanban.backend.settings.ProjectSettings;
import java.time.LocalDateTime;

public record ProjectSettingsResponse(
        Long id,
        String projectName,
        String description,
        Long updatedBy,
        LocalDateTime updatedAt
) {
    public static ProjectSettingsResponse from(ProjectSettings settings) {
        return new ProjectSettingsResponse(
                settings.getId(),
                settings.getProjectName(),
                settings.getDescription(),
                settings.getUpdatedBy(),
                settings.getUpdatedAt()
        );
    }

    /** 아직 아무도 설정을 저장하지 않은 초기 상태. */
    public static ProjectSettingsResponse empty() {
        return new ProjectSettingsResponse(null, "", "", null, null);
    }
}
