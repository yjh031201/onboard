package com.kanban.backend.timeline;

import com.kanban.backend.realtime.RealtimeChannels;
import com.kanban.backend.realtime.RealtimeEventPublisher;
import com.kanban.backend.timeline.dto.TimelineEventResponse;
import com.kanban.backend.user.User;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TimelineService {

    private final TimelineEventRepository timelineEventRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public TimelineService(TimelineEventRepository timelineEventRepository, RealtimeEventPublisher realtimeEventPublisher) {
        this.timelineEventRepository = timelineEventRepository;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    /** Persists a timeline entry and broadcasts it live to everyone on /topic/timeline. */
    @Transactional
    public TimelineEventResponse record(TimelineEventType type, String message, User actor, boolean notified) {
        TimelineEvent saved = timelineEventRepository.save(
                new TimelineEvent(type, message, actor.getId(), actor.getName(), notified)
        );

        TimelineEventResponse response = TimelineEventResponse.from(saved);
        realtimeEventPublisher.publish(RealtimeChannels.TIMELINE_EVENTS, response);
        return response;
    }

    @Transactional(readOnly = true)
    public List<TimelineEventResponse> recent(int limit) {
        return timelineEventRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .stream()
                .map(TimelineEventResponse::from)
                .toList();
    }
}
