package com.kanban.backend.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresInMs, UserResponse user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInMs / 1000, user);
    }
}
