package com.kanban.backend.realtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "realtime")
public record RealtimeProperties(String allowedOrigin) {
}
