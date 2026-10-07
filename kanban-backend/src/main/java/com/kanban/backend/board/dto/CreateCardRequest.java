package com.kanban.backend.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/** labelIds·dueAt은 생략 가능 (라벨 없음 / 마감 없음). */
public record CreateCardRequest(
        @NotBlank(message = "카드 제목을 입력해주세요.") @Size(max = 200, message = "카드 제목은 200자 이하로 입력해주세요.") String title,
        @NotBlank(message = "컬럼을 선택해주세요.") String status,
        List<String> labelIds,
        LocalDateTime dueAt
) {
}
