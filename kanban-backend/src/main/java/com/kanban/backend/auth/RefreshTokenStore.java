package com.kanban.backend.auth;

import com.kanban.backend.common.redis.RedisKeys;
import java.time.Duration;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Refresh token을 Redis에 저장/조회/무효화.
 * 키는 RedisKeys.refreshToken(userId) 하나뿐이라 재발급 시 기존 값을 덮어써서
 * 자연스럽게 "가장 최근 로그인 세션만 유효" 정책이 된다.
 */
@Component
public class RefreshTokenStore {

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(Long userId, String refreshToken, long ttlMs) {
        redisTemplate.opsForValue().set(RedisKeys.refreshToken(userId), refreshToken, Duration.ofMillis(ttlMs));
    }

    public boolean isValid(Long userId, String refreshToken) {
        String stored = redisTemplate.opsForValue().get(RedisKeys.refreshToken(userId));
        return Objects.equals(stored, refreshToken);
    }

    public void invalidate(Long userId) {
        redisTemplate.delete(RedisKeys.refreshToken(userId));
    }
}
