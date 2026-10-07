package com.kanban.backend.timeline;

import com.kanban.backend.common.ApiException;
import com.kanban.backend.integration.slack.SlackIntegrationService;
import com.kanban.backend.realtime.RealtimeChannels;
import com.kanban.backend.realtime.RealtimeEventPublisher;
import com.kanban.backend.timeline.dto.TimelineEventResponse;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRole;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TimelineService {

    private final TimelineEventRepository timelineEventRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final SlackIntegrationService slackIntegrationService;

    public TimelineService(
            TimelineEventRepository timelineEventRepository,
            RealtimeEventPublisher realtimeEventPublisher,
            SlackIntegrationService slackIntegrationService
    ) {
        this.timelineEventRepository = timelineEventRepository;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.slackIntegrationService = slackIntegrationService;
    }

    /** Persists a timeline entry and broadcasts it live to everyone on /topic/timeline. */
    @Transactional
    public TimelineEventResponse record(TimelineEventType type, String message, User actor, boolean notified) {
        TimelineEvent saved = timelineEventRepository.save(
                new TimelineEvent(type, message, actor.getId(), actor.getName(), notified)
        );

        TimelineEventResponse response = TimelineEventResponse.from(saved);
        realtimeEventPublisher.publish(RealtimeChannels.TIMELINE_EVENTS, response);
        if (notified) {
            slackIntegrationService.notifyIfConnected(message);
        }
        return response;
    }

    @Transactional(readOnly = true)
    public List<TimelineEventResponse> recent(int limit) {
        return timelineEventRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .stream()
                .map(TimelineEventResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long id, User actor) {
        requireAdmin(actor);
        if (!timelineEventRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "타임라인 항목을 찾을 수 없습니다.");
        }
        timelineEventRepository.deleteById(id);
    }

    private void requireAdmin(User user) {
        if (user.getRole() == UserRole.MEMBER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "타임라인 항목은 관리자만 삭제할 수 있습니다.");
        }
    }
}
