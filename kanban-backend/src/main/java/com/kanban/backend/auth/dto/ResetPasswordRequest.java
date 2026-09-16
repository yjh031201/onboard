package com.kanban.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 이메일 발송 없이 이름+이메일 일치 여부로 본인 확인 후 바로 새 비밀번호를 설정하는 방식.
 * (포트폴리오/데모용 간소화된 플로우 — 실서비스라면 이메일 인증 링크나 SMS 인증코드를 추가할 것)
 */
public record ResetPasswordRequest(
        @NotBlank(message = "이름을 입력해주세요") String name,
        @NotBlank(message = "이메일을 입력해주세요") @Email(message = "이메일 형식이 올바르지 않습니다") String email,
        @NotBlank(message = "새 비밀번호를 입력해주세요") @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다") String newPassword
) {
}
