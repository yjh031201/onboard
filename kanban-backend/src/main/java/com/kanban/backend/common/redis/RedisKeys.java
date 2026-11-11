package com.kanban.backend.common.redis;

/**
 * 팀 전체가 공유하는 Redis 키 네이밍 컨벤션.
 * prefix 순서 고정: 도메인:대상:{id}. 새 키를 추가할 때도 이 형식을 따를 것.
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    public static String presenceBoard(Long boardId) {
        return "presence:board:" + boardId;
    }

    public static String presenceUser(Long userId) {
        return "presence:user:" + userId;
    }

    public static String wsSession(String sessionId) {
        return "ws:session:" + sessionId;
    }

    public static String cardLock(Long cardId) {
        return "lock:card:" + cardId;
    }

    public static String boardCardMovedChannel(Long boardId) {
        return "channel:board:" + boardId + ":card-moved";
    }

    /** 예전 방식(사용자당 하나) — 배포 전에 발급된 토큰을 확인·삭제할 때만 쓴다. */
    public static String refreshToken(Long userId) {
        return "refresh:" + userId;
    }

    /** 로그인 세션(기기)마다 하나 — tokenHash는 refresh token의 SHA-256. */
    public static String refreshTokenSession(Long userId, String tokenHash) {
        return "refresh:" + userId + ":" + tokenHash;
    }

    public static String refreshTokenSessionPattern(Long userId) {
        return "refresh:" + userId + ":*";
    }

    public static String integrationConnectState(String state) {
        return "integration:state:" + state;
    }

    /** 실패 횟수 제한 카운터 — scope는 "password-reset" 등, subject는 보통 이메일(소문자). */
    public static String attemptFailures(String scope, String subject) {
        return "attempt:" + scope + ":" + subject;
    }

    /** 비밀번호 재설정 인증 코드(해시) — 10분 동안만 남는다. */
    public static String passwordResetCode(String email) {
        return "password-reset:code:" + email;
    }
}
