package com.kanban.backend.auth.dto;

import com.kanban.backend.user.User;

public record UserResponse(
        Long id,
        String email,
        String name,
        String phone,
        String role,
        boolean isGuest,
        /** 소셜 로그인 전용 계정은 false — 프론트가 비밀번호 확인이 가능한 계정인지 구분할 때 쓴다. */
        boolean hasPassword
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getRole().name(),
                user.isGuest(),
                user.getPassword() != null
        );
    }
}
