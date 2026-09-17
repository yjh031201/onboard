package com.kanban.backend.file;

public record StoredFile(String fileKey, long size, String contentType) {
}
