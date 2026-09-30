package com.kanban.backend.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCardRequest(
        @NotBlank(message = "카드 제목을 입력해주세요.") String title,
        @NotBlank(message = "컬럼을 선택해주세요.") String status,
        @Size(max = 30, message = "라벨 값이 올바르지 않습니다.") String labelId
) {
}
