package com.kanban.backend.integration.github.dto;

public record GithubIssueResponse(
        long number,
        String title,
        String state,
        String htmlUrl,
        String authorLogin,
        String createdAt
) {
}
