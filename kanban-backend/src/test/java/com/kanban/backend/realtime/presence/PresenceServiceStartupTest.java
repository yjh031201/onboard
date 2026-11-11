package com.kanban.backend.realtime.presence;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kanban.backend.project.ProjectMemberRepository;
import com.kanban.backend.realtime.RealtimeEventPublisher;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

/** 서버가 뜰 때 지난 실행의 접속 기록을 비운다 — 강제 종료 뒤 "계속 온라인"으로 보이던 문제. */
class PresenceServiceStartupTest {

    @Test
    void 기동할_때_지난_접속_기록을_비운다() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        PresenceService service = new PresenceService(redis, mock(RealtimeEventPublisher.class), mock(ProjectMemberRepository.class));

        service.afterSingletonsInstantiated();

        verify(redis).delete(List.of("presence:sessions", "presence:refcount"));
    }

    @Test
    void Redis가_없어도_기동은_막지_않는다() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.delete(anyCollection())).thenThrow(new RedisConnectionFailureException("no redis"));
        PresenceService service = new PresenceService(redis, mock(RealtimeEventPublisher.class), mock(ProjectMemberRepository.class));

        assertThatCode(service::afterSingletonsInstantiated).doesNotThrowAnyException();
    }
}
