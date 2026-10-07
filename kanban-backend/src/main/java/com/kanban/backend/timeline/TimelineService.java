package com.kanban.backend.timeline;

import com.kanban.backend.common.ApiException;
import com.kanban.backend.integration.slack.SlackIntegrationService;
import com.kanban.backend.project.ProjectAccessService;
import com.kanban.backend.realtime.RealtimeChannels;
import com.kanban.backend.realtime.RealtimeEventPublisher;
import com.kanban.backend.timeline.dto.TimelineEventDeleted;
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
    private final ProjectAccessService projectAccessService;
    private final SlackIntegrationService slackIntegrationService;

    public TimelineService(
            TimelineEventRepository timelineEventRepository,
            RealtimeEventPublisher realtimeEventPublisher,
            ProjectAccessService projectAccessService,
            SlackIntegrationService slackIntegrationService
    ) {
        this.timelineEventRepository = timelineEventRepository;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.projectAccessService = projectAccessService;
        this.slackIntegrationService = slackIntegrationService;
    }

    /** Persists a timeline entry and broadcasts it live to everyone on /topic/projects/{projectId}/timeline. */
    @Transactional
    public TimelineEventResponse record(Long projectId, TimelineEventType type, String message, User actor, boolean notified) {
        projectAccessService.requireMember(projectId, actor);

        TimelineEvent saved = timelineEventRepository.save(
                new TimelineEvent(projectId, type, message, actor.getId(), actor.getName(), notified)
        );

        TimelineEventResponse response = TimelineEventResponse.from(saved);
        realtimeEventPublisher.publish(RealtimeChannels.timelineEvents(projectId), response);
        if (notified) {
            slackIntegrationService.notifyIfConnected(message);
        }
        return response;
    }

    /** Deletes a timeline entry and tells every client to drop it via /topic/projects/{projectId}/timeline-deleted. */
    @Transactional
    public void delete(Long projectId, Long eventId, User actor) {
        projectAccessService.requireMember(projectId, actor);

        TimelineEvent event = timelineEventRepository.findById(eventId)
                .filter(e -> e.getProjectId().equals(projectId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "타임라인 기록을 찾을 수 없습니다."));
        if (!isManageable(projectId, event, actor)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인의 활동 기록만 삭제할 수 있습니다.");
        }

        timelineEventRepository.delete(event);
        realtimeEventPublisher.publish(RealtimeChannels.timelineDeletedEvents(projectId), new TimelineEventDeleted(eventId));
    }

    @Transactional(readOnly = true)
    public List<TimelineEventResponse> recent(Long projectId, User actor, int limit) {
        projectAccessService.requireMember(projectId, actor);

        return timelineEventRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId, PageRequest.of(0, limit))
                .stream()
                .map(TimelineEventResponse::from)
                .toList();
    }

    /** 기록된 행동을 한 본인이거나, 해당 프로젝트에서 관리자(OWNER/ADMIN)면 삭제 가능. */
    private boolean isManageable(Long projectId, TimelineEvent event, User actor) {
        if (event.getActorId().equals(actor.getId())) {
            return true;
        }
        UserRole myRole = projectAccessService.myRoleOrNull(projectId, actor.getId());
        return myRole != null && myRole != UserRole.MEMBER;
    }
}
