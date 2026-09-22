package com.kanban.backend.board.dto;

import com.kanban.backend.board.CardStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MoveCardRequest(
        @NotNull(message = "이동할 상태를 선택해주세요.") CardStatus status,
        @Min(value = 0, message = "위치는 0 이상이어야 합니다.") int position
) {
}
