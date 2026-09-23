package com.kanban.backend;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * Loads the full Spring context to catch wiring mistakes (missing beans,
 * bad property bindings, etc.) — uses an in-memory H2 database via the
 * "test" profile so it doesn't require Docker/MySQL to be running.
 * RedissonClient는 실제 Redis 없이는 기동 시 연결을 시도하다 실패하므로 mock으로 대체.
 */
@SpringBootTest
@ActiveProfiles("test")
class KanbanBackendApplicationTests {

    @MockBean
    private RedissonClient redissonClient;

    @Test
    void contextLoads() {
    }
}
