package com.kanban.backend.realtime.presence;

import com.kanban.backend.realtime.StompPrincipal;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Bridges STOMP session lifecycle to {@link PresenceService}. A session
 * connects/disconnects per browser tab, so PresenceService (not this class)
 * decides whether that actually flips the user's overall online/offline state.
 */
@Component
public class PresenceEventListener {

    private final PresenceService presenceService;

    public PresenceEventListener(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @EventListener
    public void handleConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        if (sessionId != null && event.getUser() instanceof StompPrincipal principal) {
            presenceService.connect(sessionId, principal.userId(), principal.userName());
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        if (event.getSessionId() != null) {
            presenceService.disconnect(event.getSessionId());
        }
    }
}
