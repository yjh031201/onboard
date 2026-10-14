package com.kanban.backend.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank(message = "프로젝트 이름을 입력해주세요.")
        @Size(max = 100, message = "프로젝트 이름은 100자 이하로 입력해주세요.")
        String name,
        @Size(max = 2000, message = "설명은 2000자 이하로 입력해주세요.")
        String description
) {
}
