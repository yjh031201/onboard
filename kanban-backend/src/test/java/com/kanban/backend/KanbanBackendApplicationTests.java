package com.kanban.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Loads the full Spring context to catch wiring mistakes (missing beans,
 * bad property bindings, etc.) — uses an in-memory H2 database via the
 * "test" profile so it doesn't require Docker/MySQL to be running.
 */
@SpringBootTest
@ActiveProfiles("test")
class KanbanBackendApplicationTests {

    @Test
    void contextLoads() {
    }
}
