package com.kanban.backend.auth;

import com.kanban.backend.common.redis.RedisKeys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Refresh token을 Redis에 저장/조회/무효화.
 *
 * <p>토큰마다 키를 따로 둔다(RedisKeys.refreshTokenSession) — 기기(브라우저)마다 로그인이 따로 유지된다.
 * 예전에는 사용자당 키 하나(RedisKeys.refreshToken)에 덮어써서, 폰에서 로그인하면 PC의 로그인 연장이 끊겼다.
 * 키에는 토큰 원문 대신 SHA-256 해시를 쓴다.
 *
 * <p>배포 직후에도 예전 방식으로 저장된 토큰은 그대로 인정한다(만료되면 자연히 사라짐) — 배포했다고 모두가 로그아웃되지 않게.
 */
@Component
public class RefreshTokenStore {

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(Long userId, String refreshToken, long ttlMs) {
        redisTemplate.opsForValue().set(sessionKey(userId, refreshToken), "1", Duration.ofMillis(ttlMs));
    }

    public boolean isValid(Long userId, String refreshToken) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey(userId, refreshToken)))) {
            return true;
        }
        return Objects.equals(redisTemplate.opsForValue().get(RedisKeys.refreshToken(userId)), refreshToken);
    }

    /** 이 토큰 하나만 무효화 — 로그아웃, 토큰 재발급(새 토큰으로 교체) 때. 다른 기기의 로그인은 그대로. */
    public void revoke(Long userId, String refreshToken) {
        redisTemplate.delete(sessionKey(userId, refreshToken));
        if (Objects.equals(redisTemplate.opsForValue().get(RedisKeys.refreshToken(userId)), refreshToken)) {
            redisTemplate.delete(RedisKeys.refreshToken(userId));
        }
    }

    /** 이 사용자의 모든 기기 로그인을 무효화 — 비밀번호 재설정, 회원 탈퇴 때. */
    public void invalidate(Long userId) {
        List<String> keys = new ArrayList<>();
        keys.add(RedisKeys.refreshToken(userId));
        ScanOptions options = ScanOptions.scanOptions().match(RedisKeys.refreshTokenSessionPattern(userId)).count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            cursor.forEachRemaining(keys::add);
        }
        redisTemplate.delete(keys);
    }

    private static String sessionKey(Long userId, String refreshToken) {
        return RedisKeys.refreshTokenSession(userId, sha256(refreshToken));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
