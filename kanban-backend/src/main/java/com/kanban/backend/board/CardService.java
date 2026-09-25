package com.kanban.backend.board;

import com.kanban.backend.board.dto.BoardEvent;
import com.kanban.backend.board.dto.CardResponse;
import com.kanban.backend.board.dto.ChangeCardLabelRequest;
import com.kanban.backend.board.dto.CreateCardRequest;
import com.kanban.backend.board.dto.MoveCardRequest;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.label.LabelRepository;
import com.kanban.backend.realtime.RealtimeChannels;
import com.kanban.backend.realtime.RealtimeEventPublisher;
import com.kanban.backend.timeline.TimelineEventType;
import com.kanban.backend.timeline.TimelineService;
import com.kanban.backend.user.User;
import java.util.ArrayList;
import java.util.List;
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
        requireExistingLabel(request.labelId());
        int position = cardRepository.findAllByStatusOrderByPositionAsc(column.getId()).size();
        Card card = cardRepository.save(
                new Card(request.title(), column.getId(), position, request.labelId(), actor.getId(), actor.getName())
        );

        timelineService.record(
                TimelineEventType.CARD_CREATED,
                "%s님이 '%s' 카드를 %s에 추가했습니다".formatted(actor.getName(), card.getTitle(), column.getName()),
                actor,
                false
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

    @Transactional
    public CardResponse changeLabel(Long cardId, ChangeCardLabelRequest request, User actor) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "카드를 찾을 수 없습니다."));
        requireExistingLabel(request.labelId());

        card.changeLabel(request.labelId());
        broadcastSnapshot("CARD_LABEL_CHANGED", actor.getName());

        return CardResponse.from(card);
    }

    /** 라벨이 삭제될 때 호출 — 그 라벨이 붙어 있던 카드들을 라벨 없음으로 바꾼다. */
    @Transactional
    public void clearLabel(String labelId, User actor) {
        List<Card> cards = cardRepository.findAllByLabelId(labelId);
        if (cards.isEmpty()) {
            return;
        }
        cards.forEach(card -> card.changeLabel(null));
        broadcastSnapshot("CARD_LABEL_CHANGED", actor.getName());
    }

    @Transactional
    public void delete(Long cardId, User actor) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "카드를 찾을 수 없습니다."));
        if (!card.isDeletableBy(actor)) {
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

    /** null(라벨 없음)은 허용, 그 외에는 설정에 있는 라벨이어야 한다. */
    private void requireExistingLabel(String labelId) {
        if (labelId != null && !labelRepository.existsById(labelId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "존재하지 않는 라벨입니다.");
        }
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
