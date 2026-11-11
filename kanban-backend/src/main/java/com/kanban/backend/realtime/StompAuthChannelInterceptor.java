package com.kanban.backend.realtime;

import com.kanban.backend.config.JwtTokenProvider;
import com.kanban.backend.project.ProjectAccessService;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRepository;
import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 *
 * <p>SUBSCRIBE도 검사한다 — 로그인만 했다고 아무 프로젝트의 토픽(/topic/projects/{id}/...)을 구독해
 * 다른 팀의 카드·댓글 변경을 받아 볼 수 없게, 그 프로젝트의 (수락한) 멤버일 때만 구독을 받아 준다.
 * 거절할 때는 예외 대신 그 SUBSCRIBE만 버린다 — 예외를 던지면 STOMP 연결 전체가 끊겨 정상 구독까지 사라진다.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Pattern PROJECT_TOPIC = Pattern.compile("^/topic/projects/(\\d+)/[\\w-]+$");

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final ProjectAccessService projectAccessService;

    public StompAuthChannelInterceptor(
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            ProjectAccessService projectAccessService
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.projectAccessService = projectAccessService;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor.getFirstNativeHeader(AUTH_HEADER));
            User user = token == null
                    ? null
                    : jwtTokenProvider.parseUserId(token).flatMap(userRepository::findById).orElse(null);

            if (user == null) {
                throw new org.springframework.messaging.MessagingException("Invalid or missing token on STOMP CONNECT");
            }

            accessor.setUser(new StompPrincipal(user.getId(), user.getName()));
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) && !canSubscribe(accessor.getUser(), accessor.getDestination())) {
            log.debug("구독 거절: user={}, destination={}", accessor.getUser(), accessor.getDestination());
            return null; // 이 SUBSCRIBE만 버린다 — 구독이 등록되지 않는다.
        }

        return message;
    }

    /** 프로젝트 토픽만 구독할 수 있고, 그 프로젝트의 멤버여야 한다. 그 밖의 목적지는 쓰지 않으므로 받지 않는다. */
    private boolean canSubscribe(Principal principal, String destination) {
        if (!(principal instanceof StompPrincipal user) || destination == null) {
            return false;
        }
        Matcher matcher = PROJECT_TOPIC.matcher(destination);
        if (!matcher.matches()) {
            return false;
        }
        Long projectId = Long.valueOf(matcher.group(1));
        return projectAccessService.myRoleOrNull(projectId, user.userId()) != null;
    }

    private String extractToken(String header) {
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return header.substring(BEARER_PREFIX.length());
    }
}
