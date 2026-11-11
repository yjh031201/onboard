package com.kanban.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "이름을 입력해주세요") @Size(max = 100, message = "이름은 100자 이하로 입력해주세요") String name,
        @NotBlank(message = "이메일을 입력해주세요") @Email(message = "이메일 형식이 올바르지 않습니다")
        @Size(max = 255, message = "이메일이 너무 길어요") String email,
        @NotBlank(message = "비밀번호를 입력해주세요") @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다") String password,
        @NotBlank(message = "휴대폰 번호를 입력해주세요")
        @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다")
        String phone
) {
}
