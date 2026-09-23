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

    public static String refreshToken(Long userId) {
        return "refresh:" + userId;
    }
}
