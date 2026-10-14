package com.kanban.backend.integration.github.dto;

/** 이슈 하나의 상세 — 카드로 가져올 때 본문(body)까지 필요해서 목록용 GithubIssueResponse와 따로 둔다. */
public record GithubIssueDetail(
        long number,
        String title,
        String body,
        String htmlUrl
) {
}
