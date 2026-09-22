package com.kanban.backend.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes realtime events onto a Redis channel. Every app instance
 * subscribes to the same channels (see {@link RealtimeRedisSubscriber}),
 * so this is the single write path other services use instead of talking
 * to the STOMP broker directly — that's what makes the fan-out work across
 * more than one backend instance.
 */
@Component
public class RealtimeEventPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RealtimeEventPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(String channel, Object payload) {
        try {
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize realtime event for channel " + channel, e);
        }
    }
}
