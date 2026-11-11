package com.kanban.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 비밀번호 재설정 2단계 — 메일로 받은 6자리 코드와 새 비밀번호. */
public record PasswordResetConfirmRequest(
        @NotBlank(message = "이메일을 입력해주세요") @Email(message = "이메일 형식이 올바르지 않습니다") String email,
        @NotBlank(message = "인증 코드를 입력해주세요") @Pattern(regexp = "^\\s*\\d{6}\\s*$", message = "인증 코드는 숫자 6자리예요") String code,
        @NotBlank(message = "새 비밀번호를 입력해주세요") @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다") String newPassword
) {
}
