package com.kanban.backend.integration;

import com.kanban.backend.common.ApiException;
import com.kanban.backend.common.redis.RedisKeys;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * OAuth state 파라미터(CSRF 방지 + 콜백 시점에 "누가 연동을 시작했는지" 복원) 1회용 저장소.
 * 콜백은 우리 SPA가 아니라 외부 서비스가 브라우저를 직접 리다이렉트시켜서 치는 요청이라
 * Authorization 헤더가 없다 — /connect 호출 시점(JWT 있음)에 발급한 state로 사용자를 되찾는다.
 */
@Component
public class IntegrationStateStore {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    public IntegrationStateStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String issue(Long userId) {
        String state = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(RedisKeys.integrationConnectState(state), String.valueOf(userId), TTL);
        return state;
    }

    public Long redeem(String state) {
        String key = RedisKeys.integrationConnectState(state);
        String userId = redisTemplate.opsForValue().get(key);
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 연동 요청입니다.");
        }
        redisTemplate.delete(key);
        return Long.valueOf(userId);
    }
}
