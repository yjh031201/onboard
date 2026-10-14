package com.kanban.backend.board.dto;

import com.kanban.backend.board.AttachmentKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** ref: GITHUB_ISSUE면 이슈 번호("12"), DRIVE_FILE이면 Drive 파일 id. 제목·주소는 서버가 직접 조회한다. */
public record AttachRequest(
        @NotNull(message = "첨부할 종류를 선택해주세요.") AttachmentKind kind,
        @NotBlank(message = "첨부할 항목을 선택해주세요.") @Size(max = 200) String ref
) {
}
