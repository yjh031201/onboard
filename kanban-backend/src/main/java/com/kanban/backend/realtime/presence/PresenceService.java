package com.kanban.backend.realtime.presence;

import com.kanban.backend.realtime.RealtimeChannels;
import com.kanban.backend.realtime.RealtimeEventPublisher;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Tracks who's currently connected, backed by Redis so it stays correct
 * across multiple backend instances and across a user having several tabs
 * open at once (a user only goes offline once their last session closes).
 */
@Service
public class PresenceService {

    private static final String SESSIONS_KEY = "presence:sessions";
    private static final String REFCOUNT_KEY = "presence:refcount";
    private static final String NAMES_KEY = "presence:names";

    private final StringRedisTemplate redisTemplate;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public PresenceService(StringRedisTemplate redisTemplate, RealtimeEventPublisher realtimeEventPublisher) {
        this.redisTemplate = redisTemplate;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    public void connect(String sessionId, Long userId, String userName) {
        String userIdKey = String.valueOf(userId);

        redisTemplate.opsForHash().put(SESSIONS_KEY, sessionId, userIdKey);
        redisTemplate.opsForHash().put(NAMES_KEY, userIdKey, userName);
        Long sessionCount = redisTemplate.opsForHash().increment(REFCOUNT_KEY, userIdKey, 1);

        if (sessionCount != null && sessionCount == 1L) {
            broadcast(new PresenceEvent(userId, userName, PresenceStatus.ONLINE));
        }
    }

    public void disconnect(String sessionId) {
        Object userIdValue = redisTemplate.opsForHash().get(SESSIONS_KEY, sessionId);
        if (userIdValue == null) {
            return;
        }
        String userIdKey = userIdValue.toString();
        redisTemplate.opsForHash().delete(SESSIONS_KEY, sessionId);

        Long sessionCount = redisTemplate.opsForHash().increment(REFCOUNT_KEY, userIdKey, -1);
        if (sessionCount != null && sessionCount <= 0) {
            redisTemplate.opsForHash().delete(REFCOUNT_KEY, userIdKey);
            Object userName = redisTemplate.opsForHash().get(NAMES_KEY, userIdKey);
            broadcast(new PresenceEvent(
                    Long.valueOf(userIdKey),
                    userName == null ? null : userName.toString(),
                    PresenceStatus.OFFLINE
            ));
        }
    }

    public Set<Long> onlineUserIds() {
        Set<Object> keys = redisTemplate.opsForHash().keys(REFCOUNT_KEY);
        Set<Long> onlineIds = new HashSet<>();
        for (Object key : keys) {
            onlineIds.add(Long.valueOf(key.toString()));
        }
        return onlineIds;
    }

    private void broadcast(PresenceEvent event) {
        realtimeEventPublisher.publish(RealtimeChannels.PRESENCE_EVENTS, event);
    }
}
