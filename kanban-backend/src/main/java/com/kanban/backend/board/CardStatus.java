package com.kanban.backend.board;

public enum CardStatus {
    TODO("할 일"),
    IN_PROGRESS("진행 중"),
    DONE("완료");

    private final String label;

    CardStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
