package com.kanban.backend.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** 카드 내용 수정 — dueAt이 null이면 마감을 지운다. */
public record UpdateCardRequest(
        @NotBlank(message = "카드 제목을 입력해주세요.") @Size(max = 200, message = "카드 제목은 200자 이하로 입력해주세요.") String title,
        LocalDateTime dueAt
) {
}
