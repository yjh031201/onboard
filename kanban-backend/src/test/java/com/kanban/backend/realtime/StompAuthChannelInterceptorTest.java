package com.kanban.backend.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kanban.backend.config.JwtTokenProvider;
import com.kanban.backend.project.ProjectAccessService;
import com.kanban.backend.user.UserRepository;
import com.kanban.backend.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

/** 실시간 구독은 그 프로젝트의 멤버만 — 아니면 그 SUBSCRIBE만 버린다(연결은 유지). */
class StompAuthChannelInterceptorTest {

    private ProjectAccessService projectAccessService;
    private StompAuthChannelInterceptor interceptor;
    private final MessageChannel channel = mock(MessageChannel.class);

    @BeforeEach
    void setUp() {
        projectAccessService = mock(ProjectAccessService.class);
        interceptor = new StompAuthChannelInterceptor(mock(JwtTokenProvider.class), mock(UserRepository.class), projectAccessService);
        when(projectAccessService.myRoleOrNull(1L, 10L)).thenReturn(UserRole.MEMBER);
        when(projectAccessService.myRoleOrNull(2L, 10L)).thenReturn(null);
    }

    private Message<byte[]> subscribe(String destination, StompPrincipal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSubscriptionId("sub-0");
        if (user != null) {
            accessor.setUser(user);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void 멤버는_프로젝트_토픽을_구독할_수_있다() {
        assertThat(interceptor.preSend(subscribe("/topic/projects/1/board", new StompPrincipal(10L, "나")), channel)).isNotNull();
    }

    @Test
    void 멤버가_아니면_구독이_버려진다() {
        assertThat(interceptor.preSend(subscribe("/topic/projects/2/board", new StompPrincipal(10L, "나")), channel)).isNull();
    }

    @Test
    void 로그인_정보가_없거나_프로젝트_토픽이_아니면_버려진다() {
        assertThat(interceptor.preSend(subscribe("/topic/projects/1/board", null), channel)).isNull();
        assertThat(interceptor.preSend(subscribe("/topic/board", new StompPrincipal(10L, "나")), channel)).isNull();
        assertThat(interceptor.preSend(subscribe("/topic/projects/1/../2/board", new StompPrincipal(10L, "나")), channel)).isNull();
    }
}
