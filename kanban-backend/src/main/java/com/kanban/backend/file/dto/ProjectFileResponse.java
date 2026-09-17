package com.kanban.backend.file.dto;

import com.kanban.backend.file.ProjectFile;
import java.time.LocalDateTime;

public record ProjectFileResponse(
        Long id,
        String fileName,
        long fileSize,
        String contentType,
        Long uploadedBy,
        LocalDateTime createdAt
) {
    public static ProjectFileResponse from(ProjectFile file) {
        return new ProjectFileResponse(
                file.getId(),
                file.getFileName(),
                file.getFileSize(),
                file.getContentType(),
                file.getUploadedBy(),
                file.getCreatedAt()
        );
    }
}
