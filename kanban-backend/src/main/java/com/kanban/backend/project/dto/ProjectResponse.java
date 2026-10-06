package com.kanban.backend.project.dto;

import com.kanban.backend.project.Project;
import com.kanban.backend.user.UserRole;
import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        /** 조회하는 사람이 이 프로젝트에서 가진 role. */
        UserRole myRole,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProjectResponse from(Project project, UserRole myRole) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                myRole,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
