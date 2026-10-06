package com.kanban.backend.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 *
 * Board/label/column/timeline channels are per-project
 * ("realtime:projects:{id}:board" -> "/topic/projects/{id}/board"), so the
 * topic is derived from the channel name instead of a fixed lookup table —
 * see {@link RealtimeRedisConfig} for the matching pattern subscription.
 * Presence is the one remaining global channel/topic pair.
 */
@Component
public class RealtimeRedisSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RealtimeRedisSubscriber.class);

    private static final Pattern PROJECT_CHANNEL = Pattern.compile("^realtime:projects:(\\d+):(.+)$");

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public RealtimeRedisSubscriber(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String topic = resolveTopic(channel);
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

    private String resolveTopic(String channel) {
        if (channel.equals(RealtimeChannels.PRESENCE_EVENTS)) {
            return RealtimeChannels.PRESENCE_TOPIC;
        }
        Matcher matcher = PROJECT_CHANNEL.matcher(channel);
        if (!matcher.matches()) {
            return null;
        }
        String projectId = matcher.group(1);
        String suffix = matcher.group(2); // board | labels | columns | timeline | timeline-deleted
        return "/topic/projects/" + projectId + "/" + suffix;
    }
}
