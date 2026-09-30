package com.kanban.backend.board.dto;

import com.kanban.backend.board.BoardColumn;

public record ColumnResponse(
        String id,
        String name,
        String color
) {
    public static ColumnResponse from(BoardColumn column) {
        return new ColumnResponse(column.getId(), column.getName(), column.getColor());
    }
}
