package com.kanban.backend.label;

import com.kanban.backend.board.CardService;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.label.dto.LabelRequest;
import com.kanban.backend.label.dto.LabelResponse;
import com.kanban.backend.project.ProjectAccessService;
import com.kanban.backend.realtime.RealtimeChannels;
import com.kanban.backend.realtime.RealtimeEventPublisher;
import com.kanban.backend.user.User;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LabelService {

    private final LabelRepository labelRepository;
    private final CardService cardService;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final ProjectAccessService projectAccessService;

    public LabelService(
            LabelRepository labelRepository,
            CardService cardService,
            RealtimeEventPublisher realtimeEventPublisher,
            ProjectAccessService projectAccessService
    ) {
        this.labelRepository = labelRepository;
        this.cardService = cardService;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.projectAccessService = projectAccessService;
    }

    @Transactional(readOnly = true)
    public List<LabelResponse> list(Long projectId, User actor) {
        projectAccessService.requireMember(projectId, actor);
        return listInternal(projectId);
    }

    @Transactional
    public LabelResponse create(Long projectId, LabelRequest request, User actor) {
        projectAccessService.requireMember(projectId, actor);
        List<Label> labels = labelRepository.findAllByProjectIdOrderByPositionAsc(projectId);
        int position = labels.isEmpty() ? 0 : labels.get(labels.size() - 1).getPosition() + 1;
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        Label label = labelRepository.save(new Label(id, projectId, request.name().trim(), request.color(), position));
        broadcastLabels(projectId);
        return LabelResponse.from(label);
    }

    @Transactional
    public LabelResponse update(Long projectId, String id, LabelRequest request, User actor) {
        projectAccessService.requireMember(projectId, actor);
        Label label = findOrThrow(projectId, id);
        label.update(request.name().trim(), request.color());
        broadcastLabels(projectId);
        return LabelResponse.from(label);
    }

    /** 라벨을 지우면 그 라벨이 붙어 있던 카드들은 라벨 없음으로 바뀐다. */
    @Transactional
    public void delete(Long projectId, String id, User actor) {
        projectAccessService.requireMember(projectId, actor);
        Label label = findOrThrow(projectId, id);
        labelRepository.delete(label);

        cardService.clearLabel(projectId, id, actor);
        broadcastLabels(projectId);
    }

    private List<LabelResponse> listInternal(Long projectId) {
        return labelRepository.findAllByProjectIdOrderByPositionAsc(projectId).stream()
                .map(LabelResponse::from)
                .toList();
    }

    private Label findOrThrow(Long projectId, String id) {
        Label label = labelRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "라벨을 찾을 수 없습니다."));
        if (!label.getProjectId().equals(projectId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "라벨을 찾을 수 없습니다.");
        }
        return label;
    }

    /** 변경 후 전체 목록을 /topic/projects/{id}/labels로 보낸다 — list()의 조회가 보류 중인 변경을 먼저 flush한다. */
    private void broadcastLabels(Long projectId) {
        realtimeEventPublisher.publish(RealtimeChannels.labelEvents(projectId), listInternal(projectId));
    }
}
