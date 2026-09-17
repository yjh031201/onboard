package com.kanban.backend.auth.dto;

/** refresh token은 여기 안 실림 — httpOnly 쿠키(AuthController.setRefreshCookie)로만 내려간다. */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    public static AuthResponse of(String accessToken, long expiresInMs, UserResponse user) {
        return new AuthResponse(accessToken, "Bearer", expiresInMs / 1000, user);
    }
}
