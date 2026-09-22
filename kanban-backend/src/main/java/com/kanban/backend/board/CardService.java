package com.kanban.backend.board;

import com.kanban.backend.board.dto.BoardEvent;
import com.kanban.backend.board.dto.CardResponse;
import com.kanban.backend.board.dto.CreateCardRequest;
import com.kanban.backend.board.dto.MoveCardRequest;
import com.kanban.backend.common.ApiException;
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
    private final TimelineService timelineService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public CardService(
            CardRepository cardRepository,
            TimelineService timelineService,
            RealtimeEventPublisher realtimeEventPublisher
    ) {
        this.cardRepository = cardRepository;
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
        int position = cardRepository.findAllByStatusOrderByPositionAsc(request.status()).size();
        Card card = cardRepository.save(
                new Card(request.title(), request.status(), position, actor.getId(), actor.getName())
        );

        timelineService.record(
                TimelineEventType.CARD_CREATED,
                "%s님이 '%s' 카드를 %s에 추가했습니다".formatted(actor.getName(), card.getTitle(), request.status().label()),
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

        CardStatus previousStatus = card.getStatus();
        CardStatus targetStatus = request.status();

        if (previousStatus == targetStatus) {
            reorderWithinColumn(card, targetStatus, request.position());
        } else {
            moveAcrossColumns(card, previousStatus, targetStatus, request.position());
        }

        if (previousStatus != targetStatus) {
            timelineService.record(
                    TimelineEventType.CARD_MOVED,
                    "%s님이 '%s' 카드를 %s → %s로 이동했습니다"
                            .formatted(actor.getName(), card.getTitle(), previousStatus.label(), targetStatus.label()),
                    actor,
                    true
            );
        }
        broadcastSnapshot("CARD_MOVED", actor.getName());

        return CardResponse.from(card);
    }

    private void reorderWithinColumn(Card card, CardStatus status, int requestedPosition) {
        List<Card> column = new ArrayList<>(cardRepository.findAllByStatusOrderByPositionAsc(status));
        column.removeIf(c -> c.getId().equals(card.getId()));

        int targetIndex = clamp(requestedPosition, column.size());
        column.add(targetIndex, card);
        renumber(column);
    }

    private void moveAcrossColumns(Card card, CardStatus previousStatus, CardStatus targetStatus, int requestedPosition) {
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
