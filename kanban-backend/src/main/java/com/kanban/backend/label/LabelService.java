package com.kanban.backend.label;

import com.kanban.backend.board.CardService;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.label.dto.LabelRequest;
import com.kanban.backend.label.dto.LabelResponse;
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

    public LabelService(
            LabelRepository labelRepository,
            CardService cardService,
            RealtimeEventPublisher realtimeEventPublisher
    ) {
        this.labelRepository = labelRepository;
        this.cardService = cardService;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<LabelResponse> list() {
        return labelRepository.findAllByOrderByPositionAsc().stream()
                .map(LabelResponse::from)
                .toList();
    }

    @Transactional
    public LabelResponse create(LabelRequest request) {
        List<Label> labels = labelRepository.findAllByOrderByPositionAsc();
        int position = labels.isEmpty() ? 0 : labels.get(labels.size() - 1).getPosition() + 1;
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        Label label = labelRepository.save(new Label(id, request.name().trim(), request.color(), position));
        broadcastLabels();
        return LabelResponse.from(label);
    }

    @Transactional
    public LabelResponse update(String id, LabelRequest request) {
        Label label = findOrThrow(id);
        label.update(request.name().trim(), request.color());
        broadcastLabels();
        return LabelResponse.from(label);
    }

    /** 라벨을 지우면 그 라벨이 붙어 있던 카드들은 라벨 없음으로 바뀐다. */
    @Transactional
    public void delete(String id, User actor) {
        Label label = findOrThrow(id);
        labelRepository.delete(label);

        cardService.clearLabel(id, actor);
        broadcastLabels();
    }

    private Label findOrThrow(String id) {
        return labelRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "라벨을 찾을 수 없습니다."));
    }

    /** 변경 후 전체 목록을 /topic/labels로 보낸다 — list()의 조회가 보류 중인 변경을 먼저 flush한다. */
    private void broadcastLabels() {
        realtimeEventPublisher.publish(RealtimeChannels.LABEL_EVENTS, list());
    }
}
