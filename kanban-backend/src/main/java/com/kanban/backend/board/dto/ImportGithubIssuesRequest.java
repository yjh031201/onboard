package com.kanban.backend.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 연결된 저장소의 이슈 번호들을 status 컬럼에 카드로 만든다. 이미 가져온 이슈는 건너뛴다. */
public record ImportGithubIssuesRequest(
        @NotBlank(message = "카드를 넣을 컬럼을 선택해주세요.") String status,
        @NotEmpty(message = "가져올 이슈를 선택해주세요.")
        @Size(max = 20, message = "이슈는 한 번에 20개까지 가져올 수 있습니다.")
        List<Long> issueNumbers
) {
}
