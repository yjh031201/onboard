package com.kanban.backend.realtime.presence;

import com.kanban.backend.project.InviteStatus;
import com.kanban.backend.project.ProjectMember;
import com.kanban.backend.project.ProjectMemberRepository;
import com.kanban.backend.realtime.RealtimeChannels;
import com.kanban.backend.realtime.RealtimeEventPublisher;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Tracks who's currently connected, backed by Redis so it stays correct
 * across multiple backend instances and across a user having several tabs
 * open at once (a user only goes offline once their last session closes).
 *
 * "온라인인지 아닌지" 자체는 사용자 단위로 전역이지만(여러 탭/여러 프로젝트를 동시에 봐도 한 사람),
 * 그 변화를 알려주는 건 그 사람이 속한 프로젝트들에만 해야 한다 — 그래서 온/오프라인 전환이
 * 생기면 ProjectMemberRepository로 그 사람이 (수락해서) 속한 프로젝트를 전부 찾아서 각각의
 * 프로젝트 presence 토픽에 따로 쏴 준다.
 */
@Service
public class PresenceService implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(PresenceService.class);

    private static final String SESSIONS_KEY = "presence:sessions";
    private static final String REFCOUNT_KEY = "presence:refcount";
    private static final String NAMES_KEY = "presence:names";

    private final StringRedisTemplate redisTemplate;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final ProjectMemberRepository projectMemberRepository;

    public PresenceService(
            StringRedisTemplate redisTemplate,
            RealtimeEventPublisher realtimeEventPublisher,
            ProjectMemberRepository projectMemberRepository
    ) {
        this.redisTemplate = redisTemplate;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.projectMemberRepository = projectMemberRepository;
    }

    /**
     * 서버가 뜰 때 지난번 실행의 접속 기록을 비운다.
     *
     * <p>서버가 정상 종료되면 세션마다 disconnect가 불려 기록이 정리되지만, 강제 종료(장애·메모리 부족·kill)되면
     * 불리지 않아 접속 수가 0으로 돌아오지 못하고 그 사람은 계속 "온라인"으로 보였다.
     * 웹 서버가 접속을 받기 시작하기 전(빈 생성이 끝난 직후)에 부르므로, 다시 붙은 사람의 새 기록은 지우지 않는다.
     * 백엔드가 한 대라는 전제다 — 여러 대로 늘리면 인스턴스별로 기록을 나눠야 한다.
     */
    @Override
    public void afterSingletonsInstantiated() {
        try {
            redisTemplate.delete(List.of(SESSIONS_KEY, REFCOUNT_KEY));
        } catch (RuntimeException e) {
            // Redis 없이 뜨는 테스트(CI) 등에서는 비울 기록도 없다 — 서버 기동을 막지 않는다.
            log.warn("지난 접속 기록을 비우지 못했습니다: {}", e.getMessage());
        }
    }

    public void connect(String sessionId, Long userId, String userName) {
        String userIdKey = String.valueOf(userId);

        redisTemplate.opsForHash().put(SESSIONS_KEY, sessionId, userIdKey);
        redisTemplate.opsForHash().put(NAMES_KEY, userIdKey, userName);
        Long sessionCount = redisTemplate.opsForHash().increment(REFCOUNT_KEY, userIdKey, 1);

        if (sessionCount != null && sessionCount == 1L) {
            broadcastToMyProjects(new PresenceEvent(userId, userName, PresenceStatus.ONLINE));
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
            broadcastToMyProjects(new PresenceEvent(
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

    private void broadcastToMyProjects(PresenceEvent event) {
        List<ProjectMember> memberships = projectMemberRepository.findAllByUserIdAndStatus(event.userId(), InviteStatus.ACCEPTED);
        for (ProjectMember membership : memberships) {
            realtimeEventPublisher.publish(RealtimeChannels.presenceEvents(membership.getProjectId()), event);
        }
    }
}
