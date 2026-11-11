package com.kanban.backend.integration.github.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GithubRepoRequest(
        @NotBlank(message = "저장소 소유자를 입력해주세요.") @Size(max = 100, message = "저장소 소유자 이름이 너무 길어요.") String owner,
        @NotBlank(message = "저장소 이름을 입력해주세요.") @Size(max = 100, message = "저장소 이름이 너무 길어요.") String repo
) {
}
