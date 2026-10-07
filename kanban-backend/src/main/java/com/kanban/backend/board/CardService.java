package com.kanban.backend.board;

import com.kanban.backend.board.dto.BoardEvent;
import com.kanban.backend.board.dto.CardResponse;
import com.kanban.backend.board.dto.ChangeCardLabelRequest;
import com.kanban.backend.board.dto.CreateCardRequest;
import com.kanban.backend.board.dto.MoveCardRequest;
import com.kanban.backend.board.dto.UpdateCardRequest;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.label.Label;
import com.kanban.backend.label.LabelRepository;
import com.kanban.backend.realtime.RealtimeChannels;
import com.kanban.backend.realtime.RealtimeEventPublisher;
import com.kanban.backend.timeline.TimelineEventType;
import com.kanban.backend.timeline.TimelineService;
import com.kanban.backend.user.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final BoardColumnRepository columnRepository;
    private final LabelRepository labelRepository;
    private final TimelineService timelineService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public CardService(
            CardRepository cardRepository,
            BoardColumnRepository columnRepository,
            LabelRepository labelRepository,
            TimelineService timelineService,
            RealtimeEventPublisher realtimeEventPublisher
    ) {
        this.cardRepository = cardRepository;
        this.columnRepository = columnRepository;
        this.labelRepository = labelRepository;
        this.timelineService = timelineService;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<CardResponse> list() {
        return cardRepository.findAllByOrderByStatusAscPositionAsc().stream()
                .map(CardResponse::from)
                .toList();
    }

    @Transactional
    public CardResponse create(CreateCardRequest request, User actor) {
        BoardColumn column = findColumn(request.status());
        List<String> labelIds = validLabelIds(request.labelIds());
        String customLabel = normalizeCustomLabel(labelIds, request.customLabel());
        int position = cardRepository.findAllByStatusOrderByPositionAsc(column.getId()).size();
        Card card = cardRepository.save(new Card(
                request.title().trim(), column.getId(), position, labelIds, customLabel, request.dueAt(),
                actor.getId(), actor.getName()
        ));

        timelineService.record(
                TimelineEventType.CARD_CREATED,
                "%s님이 '%s' 카드를 %s에 추가했습니다".formatted(actor.getName(), card.getTitle(), column.getName()),
                actor,
                true
        );
        broadcastSnapshot("CARD_CREATED", actor.getName());

        return CardResponse.from(card);
    }

    @Transactional
    public CardResponse move(Long cardId, MoveCardRequest request, User actor) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "카드를 찾을 수 없습니다."));

        String previousStatus = card.getStatus();
        BoardColumn targetColumn = findColumn(request.status());
        String targetStatus = targetColumn.getId();
        boolean sameColumn = previousStatus.equals(targetStatus);

        if (sameColumn) {
            reorderWithinColumn(card, targetStatus, request.position());
        } else {
            moveAcrossColumns(card, previousStatus, targetStatus, request.position());
        }

        if (!sameColumn) {
            String previousName = columnRepository.findById(previousStatus).map(BoardColumn::getName).orElse(previousStatus);
            timelineService.record(
                    TimelineEventType.CARD_MOVED,
                    "%s님이 '%s' 카드를 %s → %s로 이동했습니다"
                            .formatted(actor.getName(), card.getTitle(), previousName, targetColumn.getName()),
                    actor,
                    true
            );
        }
        broadcastSnapshot("CARD_MOVED", actor.getName());

        return CardResponse.from(card);
    }

    /** 제목·설명·마감 수정 — 삭제와 같은 권한(작성자 또는 관리자). */
    @Transactional
    public CardResponse update(Long cardId, UpdateCardRequest request, User actor) {
        Card card = findCard(cardId);
        if (!card.isManageableBy(actor)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인이 만든 카드만 수정할 수 있습니다.");
        }

        card.update(request.title().trim(), normalizeDescription(request.description()), request.dueAt());
        timelineService.record(
                TimelineEventType.CARD_UPDATED,
                "%s님이 '%s' 카드를 수정했습니다".formatted(actor.getName(), card.getTitle()),
                actor,
                false
        );
        broadcastSnapshot("CARD_UPDATED", actor.getName());

        return CardResponse.from(card);
    }

    @Transactional
    public CardResponse changeLabels(Long cardId, ChangeCardLabelRequest request, User actor) {
        Card card = findCard(cardId);
        List<String> labelIds = validLabelIds(request.labelIds());
        String customLabel = normalizeCustomLabel(labelIds, request.customLabel());
        if (labelIds.equals(card.getLabelIds()) && Objects.equals(customLabel, card.getCustomLabel())) {
            return CardResponse.from(card);
        }

        card.changeLabels(labelIds, customLabel);
        String labelNames = labelIds.isEmpty()
                ? "없음"
                : String.join(", ", labelIds.stream()
                        .map(id -> Label.ETC_ID.equals(id) && customLabel != null
                                ? customLabel
                                : labelRepository.findById(id).map(Label::getName).orElse(id))
                        .toList());
        timelineService.record(
                TimelineEventType.CARD_LABEL_CHANGED,
                "%s님이 '%s' 카드의 라벨을 %s(으)로 바꿨습니다".formatted(actor.getName(), card.getTitle(), labelNames),
                actor,
                true
        );
        broadcastSnapshot("CARD_LABEL_CHANGED", actor.getName());

        return CardResponse.from(card);
    }

    /** 라벨이 삭제될 때 호출 — 그 라벨이 붙어 있던 카드들에서 떼어낸다. */
    @Transactional
    public void clearLabel(String labelId, User actor) {
        List<Card> cards = cardRepository.findAllWithLabel(labelId);
        if (cards.isEmpty()) {
            return;
        }
        cards.forEach(card -> card.removeLabel(labelId));
        broadcastSnapshot("CARD_LABEL_CHANGED", actor.getName());
    }

    @Transactional
    public void delete(Long cardId, User actor) {
        Card card = findCard(cardId);
        if (!card.isManageableBy(actor)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인이 만든 카드만 삭제할 수 있습니다.");
        }

        String status = card.getStatus();
        cardRepository.delete(card);

        List<Card> column = new ArrayList<>(cardRepository.findAllByStatusOrderByPositionAsc(status));
        column.removeIf(c -> c.getId().equals(cardId));
        renumber(column);

        broadcastSnapshot("CARD_DELETED", actor.getName());
    }

    private BoardColumn findColumn(String columnId) {
        return columnRepository.findById(columnId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "존재하지 않는 컬럼입니다."));
    }

    private Card findCard(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "카드를 찾을 수 없습니다."));
    }

    /** 앞뒤 공백을 떼고, 내용이 없으면 설명 없음(null)으로 저장한다. */
    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.strip();
    }

    /** null은 라벨 없음. 중복은 합치고, 최대 개수와 존재 여부를 검사한다. */
    private List<String> validLabelIds(List<String> requested) {
        if (requested == null) {
            return List.of();
        }
        List<String> labelIds = requested.stream().distinct().toList();
        if (labelIds.size() > Card.MAX_LABELS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "라벨은 최대 %d개까지 붙일 수 있습니다.".formatted(Card.MAX_LABELS));
        }
        for (String labelId : labelIds) {
            if (labelId == null || !labelRepository.existsById(labelId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "존재하지 않는 라벨입니다.");
            }
        }
        return labelIds;
    }

    /** "기타" 라벨에 직접 적은 글자 — 기타 라벨이 안 붙어 있거나 내용이 없으면 null. */
    private String normalizeCustomLabel(List<String> labelIds, String customLabel) {
        if (customLabel == null || customLabel.isBlank() || !labelIds.contains(Label.ETC_ID)) {
            return null;
        }
        return customLabel.strip();
    }

    private void reorderWithinColumn(Card card, String status, int requestedPosition) {
        List<Card> column = new ArrayList<>(cardRepository.findAllByStatusOrderByPositionAsc(status));
        column.removeIf(c -> c.getId().equals(card.getId()));

        int targetIndex = clamp(requestedPosition, column.size());
        column.add(targetIndex, card);
        renumber(column);
    }

    private void moveAcrossColumns(Card card, String previousStatus, String targetStatus, int requestedPosition) {
        List<Card> previousColumn = new ArrayList<>(cardRepository.findAllByStatusOrderByPositionAsc(previousStatus));
        previousColumn.removeIf(c -> c.getId().equals(card.getId()));
        renumber(previousColumn);

        List<Card> targetColumn = new ArrayList<>(cardRepository.findAllByStatusOrderByPositionAsc(targetStatus));
        int targetIndex = clamp(requestedPosition, targetColumn.size());
        targetColumn.add(targetIndex, card);

        card.moveTo(targetStatus, targetIndex);
        renumber(targetColumn);
    }

    private void renumber(List<Card> cards) {
        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).reposition(i);
        }
    }

    private int clamp(int requested, int size) {
        return Math.max(0, Math.min(requested, size));
    }

    private void broadcastSnapshot(String eventType, String actorName) {
        BoardEvent event = new BoardEvent(eventType, list(), actorName);
        realtimeEventPublisher.publish(RealtimeChannels.BOARD_EVENTS, event);
    }
}
