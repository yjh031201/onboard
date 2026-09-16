package com.kanban.backend.auth.dto;

import com.kanban.backend.user.User;

public record UserResponse(
        Long id,
        String email,
        String name,
        String phone,
        String role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getRole().name()
        );
    }
}
