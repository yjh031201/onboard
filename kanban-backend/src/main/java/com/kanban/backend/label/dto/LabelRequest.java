package com.kanban.backend.label.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LabelRequest(
        @NotBlank(message = "라벨 이름을 입력해주세요.")
        @Size(max = 20, message = "라벨 이름은 20자 이하로 입력해주세요.")
        String name,
        @NotBlank(message = "라벨 색상을 선택해주세요.")
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "라벨 색상 형식이 올바르지 않습니다.")
        String color
) {
}
