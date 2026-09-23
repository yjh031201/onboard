package com.kanban.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record FindIdRequest(
        @NotBlank(message = "이름을 입력해주세요") String name,
        @NotBlank(message = "휴대폰 번호를 입력해주세요") String phone
) {
}
