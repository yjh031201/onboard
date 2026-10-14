package com.kanban.backend.integration.github.dto;

/** 소유자의 저장소 하나 — 저장소 입력칸 아래 목록에 보여 준다. description은 없으면 null. */
public record GithubRepoResponse(
        String name,
        boolean isPrivate,
        String description
) {
}
