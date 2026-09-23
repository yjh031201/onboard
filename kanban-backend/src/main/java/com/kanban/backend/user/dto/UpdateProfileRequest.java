package com.kanban.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateProfileRequest(
        @NotBlank(message = "이름을 입력해주세요") String name,
        @NotBlank(message = "휴대폰 번호를 입력해주세요")
        @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다")
        String phone
) {
}
