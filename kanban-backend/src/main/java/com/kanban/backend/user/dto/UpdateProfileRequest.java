package com.kanban.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 프로필 수정. 휴대폰은 비워 둘 수 있다 — 구글/네이버로 가입한 계정은 휴대폰이 없어서, 예전엔 이름만 바꾸려 해도
 * 휴대폰을 넣어야 했다. 이메일·비밀번호 계정은 아이디 찾기에 휴대폰을 쓰므로 UserService에서 다시 필수로 확인한다.
 */
public record UpdateProfileRequest(
        @NotBlank(message = "이름을 입력해주세요") @Size(max = 100, message = "이름은 100자 이하로 입력해주세요") String name,
        @Pattern(regexp = "^$|^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다")
        String phone
) {
}
