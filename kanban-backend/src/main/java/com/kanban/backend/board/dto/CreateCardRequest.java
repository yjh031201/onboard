package com.kanban.backend.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * description·labelIds·customLabel·dueAt·assigneeId는 생략 가능 (설명 없음 / 라벨 없음 / 마감 없음 / 담당자 없음).
 * customLabel은 "기타" 라벨을 붙였을 때만 쓰인다. assigneeId는 이 프로젝트 멤버의 userId.
 * description을 같이 받아서, 새 카드 창에서 설명까지 한 번에 저장한다 (만든 뒤 따로 수정하지 않게).
 */
public record CreateCardRequest(
        @NotBlank(message = "카드 제목을 입력해주세요.") @Size(max = 200, message = "카드 제목은 200자 이하로 입력해주세요.") String title,
        @NotBlank(message = "컬럼을 선택해주세요.") String status,
        @Size(max = 2000, message = "카드 설명은 2000자 이하로 입력해주세요.") String description,
        List<String> labelIds,
        @Size(max = 20, message = "라벨 이름은 20자 이하로 입력해주세요.") String customLabel,
        LocalDateTime dueAt,
        Long assigneeId
) {
}
