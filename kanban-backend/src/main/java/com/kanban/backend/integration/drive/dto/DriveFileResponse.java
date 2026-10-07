package com.kanban.backend.integration.drive.dto;

public record DriveFileResponse(
        String id,
        String name,
        String webViewLink,
        String mimeType,
        String modifiedTime
) {
}
