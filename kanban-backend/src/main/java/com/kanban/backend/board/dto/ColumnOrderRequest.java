package com.kanban.backend.board.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** 새 순서대로 나열한 전체 컬럼 id — 빠지거나 중복된 id가 있으면 거절한다. */
public record ColumnOrderRequest(
        @NotEmpty(message = "컬럼 순서를 보내주세요.") List<String> columnIds
) {
}
