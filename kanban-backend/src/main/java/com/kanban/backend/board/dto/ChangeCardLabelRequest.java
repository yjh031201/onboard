package com.kanban.backend.board.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 카드에 붙일 라벨 id 전체 목록 (최대 2개). 빈 목록이나 null이면 라벨을 모두 뗀다.
 * customLabel은 "기타" 라벨에 직접 적은 글자 — 기타 라벨이 목록에 없으면 무시된다.
 */
public record ChangeCardLabelRequest(
        List<String> labelIds,
        @Size(max = 20, message = "라벨 이름은 20자 이하로 입력해주세요.") String customLabel
) {
}
