package com.kanban.backend.auth.oauth2;

import com.kanban.backend.user.AuthProvider;
import java.util.Map;

/** 구글/네이버마다 다른 사용자 정보 응답 모양을 하나의 형태로 맞춰주는 어댑터. */
record OAuthUserInfo(AuthProvider provider, String providerId, String email, String name, boolean emailVerified) {

    static OAuthUserInfo of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "google" -> ofGoogle(attributes);
            case "naver" -> ofNaver(attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 로그인 제공자입니다: " + registrationId);
        };
    }

    private static OAuthUserInfo ofGoogle(Map<String, Object> attributes) {
        boolean verified = Boolean.TRUE.equals(attributes.get("email_verified"));
        return new OAuthUserInfo(
                AuthProvider.GOOGLE,
                String.valueOf(attributes.get("sub")),
                (String) attributes.get("email"),
                (String) attributes.get("name"),
                verified
        );
    }

    @SuppressWarnings("unchecked")
    private static OAuthUserInfo ofNaver(Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        return new OAuthUserInfo(
                AuthProvider.NAVER,
                String.valueOf(response.get("id")),
                (String) response.get("email"),
                (String) response.get("name"),
                // 네이버는 이메일 제공에 동의한 시점에 이미 자체적으로 인증된 이메일만 내려줌.
                true
        );
    }
}
