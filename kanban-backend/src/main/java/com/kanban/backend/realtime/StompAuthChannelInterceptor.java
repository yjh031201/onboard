package com.kanban.backend.realtime;

import com.kanban.backend.config.JwtTokenProvider;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRepository;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Authenticates the STOMP CONNECT frame using the same JWT the REST API
 * uses. Browsers can't attach an Authorization header to a WebSocket
 * upgrade request, so the token instead travels as a native STOMP header on
 * CONNECT — the frontend's STOMP client sends it there.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public StompAuthChannelInterceptor(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor.getFirstNativeHeader(AUTH_HEADER));
            User user = token == null
                    ? null
                    : jwtTokenProvider.parseUserId(token).flatMap(userRepository::findById).orElse(null);

            if (user == null) {
                throw new org.springframework.messaging.MessagingException("Invalid or missing token on STOMP CONNECT");
            }

            accessor.setUser(new StompPrincipal(user.getId(), user.getName()));
        }

        return message;
    }

    private String extractToken(String header) {
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return header.substring(BEARER_PREFIX.length());
    }
}
