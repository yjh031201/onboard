package com.kanban.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 비밀번호 재설정 1단계 — 이 이메일로 인증 코드를 보내 달라는 요청. */
public record PasswordResetCodeRequest(
        @NotBlank(message = "이메일을 입력해주세요") @Email(message = "이메일 형식이 올바르지 않습니다") String email
) {
}
