package com.kanban.backend.board.dto;

import java.util.List;

/** 카드에 붙일 라벨 id 전체 목록 (최대 2개). 빈 목록이나 null이면 라벨을 모두 뗀다. */
public record ChangeCardLabelRequest(
        List<String> labelIds
) {
}
