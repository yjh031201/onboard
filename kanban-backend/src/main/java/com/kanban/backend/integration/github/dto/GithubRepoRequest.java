package com.kanban.backend.integration.github.dto;

import jakarta.validation.constraints.NotBlank;

public record GithubRepoRequest(
        @NotBlank(message = "저장소 소유자를 입력해주세요.") String owner,
        @NotBlank(message = "저장소 이름을 입력해주세요.") String repo
) {
}
