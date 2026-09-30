package com.kanban.backend.label.dto;

import com.kanban.backend.label.Label;

public record LabelResponse(
        String id,
        String name,
        String color
) {
    public static LabelResponse from(Label label) {
        return new LabelResponse(label.getId(), label.getName(), label.getColor());
    }
}
