package com.kanban.backend.board.dto;

import com.kanban.backend.board.CardStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCardRequest(
        @NotBlank(message = "카드 제목을 입력해주세요.") String title,
        @NotNull(message = "카드 상태를 선택해주세요.") CardStatus status
) {
}
