package com.kanban.backend.file.dto;

import com.kanban.backend.file.ProjectFile;
import java.time.LocalDateTime;

public record ProjectFileResponse(
        Long id,
        String fileName,
        long fileSize,
        String contentType,
        Long uploadedBy,
        String uploaderName,
        LocalDateTime createdAt
) {
    /** uploaderName은 탈퇴 등으로 사용자를 못 찾으면 null. */
    public static ProjectFileResponse from(ProjectFile file, String uploaderName) {
        return new ProjectFileResponse(
                file.getId(),
                file.getFileName(),
                file.getFileSize(),
                file.getContentType(),
                file.getUploadedBy(),
                uploaderName,
                file.getCreatedAt()
        );
    }
}
