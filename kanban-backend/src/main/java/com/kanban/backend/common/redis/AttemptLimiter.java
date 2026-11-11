package com.kanban.backend.common.redis;

import com.kanban.backend.common.ApiException;
import java.time.Duration;
import java.util.Locale;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 비밀번호 추측(무차별 대입)을 막기 위한 실패 횟수 제한. 대상(보통 이메일)별로 실패를 세고,
 * 창(window) 안에서 maxFailures번 실패하면 창이 끝날 때까지 429로 막는다. 성공하면 카운터를 지운다.
 *
 * Redis 카운터라 서버가 여러 대여도 같은 기준으로 센다. 이메일 기준이라 남의 계정을 일부러 잠그는 것도
 * 가능하지만, 잠금은 창(15분)이 지나면 저절로 풀리므로 계정 탈취를 막는 쪽을 택했다.
 */
@Component
public class AttemptLimiter {

    private final StringRedisTemplate redisTemplate;

    public AttemptLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 이미 한도를 넘었으면 429. 시도 전에 부른다. */
    public void checkAllowed(String scope, String subject, int maxFailures) {
        String value = redisTemplate.opsForValue().get(key(scope, subject));
        if (value != null && Integer.parseInt(value) >= maxFailures) {
            Long ttlSeconds = redisTemplate.getExpire(key(scope, subject));
            long minutes = ttlSeconds == null || ttlSeconds <= 0 ? 1 : (ttlSeconds + 59) / 60;
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "시도 횟수를 초과했어요. " + minutes + "분 뒤에 다시 시도해 주세요.");
        }
    }

    /** 실패 1회 기록 — 첫 실패 때 창(window)이 시작된다. */
    public void recordFailure(String scope, String subject, Duration window) {
        String key = key(scope, subject);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, window);
        }
    }

    public void reset(String scope, String subject) {
        redisTemplate.delete(key(scope, subject));
    }

    private static String key(String scope, String subject) {
        return RedisKeys.attemptFailures(scope, subject.trim().toLowerCase(Locale.ROOT));
    }
}
