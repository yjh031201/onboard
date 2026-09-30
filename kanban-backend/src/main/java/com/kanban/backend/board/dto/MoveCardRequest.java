package com.kanban.backend.board.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record MoveCardRequest(
        @NotBlank(message = "이동할 컬럼을 선택해주세요.") String status,
        @Min(value = 0, message = "위치는 0 이상이어야 합니다.") int position
) {
}
