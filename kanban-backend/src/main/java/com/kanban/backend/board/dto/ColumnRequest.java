package com.kanban.backend.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ColumnRequest(
        @NotBlank(message = "컬럼 이름을 입력해주세요.")
        @Size(max = 30, message = "컬럼 이름은 30자 이하로 입력해주세요.")
        String name,
        @NotBlank(message = "컬럼 색상을 선택해주세요.")
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "컬럼 색상 형식이 올바르지 않습니다.")
        String color
) {
}
