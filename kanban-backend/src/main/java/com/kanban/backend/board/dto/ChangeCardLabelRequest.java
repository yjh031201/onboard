package com.kanban.backend.board.dto;

import jakarta.validation.constraints.Size;

/** labelId가 null이면 카드에서 라벨을 뗀다. */
public record ChangeCardLabelRequest(
        @Size(max = 30, message = "라벨 값이 올바르지 않습니다.") String labelId
) {
}
