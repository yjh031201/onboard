package com.kanban.backend.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Relays every message received on a realtime Redis channel to the matching
 * STOMP topic, for whichever WebSocket clients happen to be connected to
 * this instance. This is the read side of the Redis pub-sub backplane.
 */
@Component
public class RealtimeRedisSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RealtimeRedisSubscriber.class);

    private static final Map<String, String> CHANNEL_TO_TOPIC = Map.of(
            RealtimeChannels.BOARD_EVENTS, RealtimeChannels.BOARD_TOPIC,
            RealtimeChannels.PRESENCE_EVENTS, RealtimeChannels.PRESENCE_TOPIC,
            RealtimeChannels.TIMELINE_EVENTS, RealtimeChannels.TIMELINE_TOPIC
    );

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public RealtimeRedisSubscriber(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String topic = CHANNEL_TO_TOPIC.get(channel);
        if (topic == null) {
            return;
        }

        try {
            Object payload = objectMapper.readValue(message.getBody(), Object.class);
            messagingTemplate.convertAndSend(topic, payload);
        } catch (IOException e) {
            log.warn("Failed to relay realtime event from channel {} to {}", channel, topic, e);
        }
    }
}
