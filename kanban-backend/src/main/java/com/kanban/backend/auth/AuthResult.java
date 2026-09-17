package com.kanban.backend.auth;

import com.kanban.backend.auth.dto.AuthResponse;

/** AuthService 내부용 — 컨트롤러가 refreshToken은 쿠키로, body는 JSON으로 나눠 내려보낼 때 씀. */
public record AuthResult(AuthResponse body, String refreshToken) {
}
