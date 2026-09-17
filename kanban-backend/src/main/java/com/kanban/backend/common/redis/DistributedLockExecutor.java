package com.kanban.backend.common.redis;

import com.kanban.backend.common.ApiException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 카드 이동처럼 동시 수정을 막아야 하는 구간에서 쓰는 Redisson 기반 분산 락 헬퍼.
 * 사용 예: lockExecutor.execute(RedisKeys.cardLock(cardId), () -> cardService.move(...));
 */
@Component
public class DistributedLockExecutor {

    private static final long DEFAULT_WAIT_SECONDS = 3;
    private static final long DEFAULT_LEASE_SECONDS = 5;

    private final RedissonClient redissonClient;

    public DistributedLockExecutor(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public <T> T execute(String lockKey, Supplier<T> action) {
        return execute(lockKey, DEFAULT_WAIT_SECONDS, DEFAULT_LEASE_SECONDS, action);
    }

    public <T> T execute(String lockKey, long waitSeconds, long leaseSeconds, Supplier<T> action) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired;
        try {
            acquired = lock.tryLock(waitSeconds, leaseSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "잠시 후 다시 시도해주세요.");
        }

        if (!acquired) {
            throw new ApiException(HttpStatus.CONFLICT, "다른 사용자가 같은 작업을 처리 중입니다.");
        }

        try {
            return action.get();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
