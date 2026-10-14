package com.kanban.backend.comment;

import com.kanban.backend.board.Card;
import com.kanban.backend.board.CardRepository;
import com.kanban.backend.board.CardService;
import com.kanban.backend.comment.dto.CommentEvent;
import com.kanban.backend.comment.dto.CommentRequest;
import com.kanban.backend.comment.dto.CommentResponse;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.project.ProjectAccessService;
import com.kanban.backend.realtime.RealtimeChannels;
import com.kanban.backend.realtime.RealtimeEventPublisher;
import com.kanban.backend.timeline.TimelineEventType;
import com.kanban.backend.timeline.TimelineService;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRole;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카드 댓글. 읽기·쓰기는 프로젝트 멤버 누구나, 수정은 작성자 본인만, 삭제는 작성자 또는 OWNER/ADMIN.
 * 바뀔 때마다 그 카드의 댓글 전체를 /topic/projects/{id}/card-comments로 보내고,
 * 카드 앞면의 댓글 수도 바뀌므로 보드 스냅샷도 같이 보낸다.
 */
@Service
public class CardCommentService {

    private final CardCommentRepository commentRepository;
    private final CardRepository cardRepository;
    private final CardService cardService;
    private final ProjectAccessService projectAccessService;
    private final TimelineService timelineService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public CardCommentService(
            CardCommentRepository commentRepository,
            CardRepository cardRepository,
            CardService cardService,
            ProjectAccessService projectAccessService,
            TimelineService timelineService,
            RealtimeEventPublisher realtimeEventPublisher
    ) {
        this.commentRepository = commentRepository;
        this.cardRepository = cardRepository;
        this.cardService = cardService;
        this.projectAccessService = projectAccessService;
        this.timelineService = timelineService;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long projectId, Long cardId, User actor) {
        projectAccessService.requireMember(projectId, actor);
        findCard(projectId, cardId);
        return listInternal(cardId);
    }

    @Transactional
    public CommentResponse create(Long projectId, Long cardId, CommentRequest request, User actor) {
        projectAccessService.requireMember(projectId, actor);
        Card card = findCard(projectId, cardId);
        CardComment comment = commentRepository.save(
                new CardComment(projectId, cardId, actor.getId(), actor.getName(), request.content().strip())
        );

        timelineService.record(
                projectId,
                TimelineEventType.CARD_COMMENTED,
                "%s님이 '%s' 카드에 댓글을 남겼습니다".formatted(actor.getName(), card.getTitle()),
                actor,
                false
        );
        broadcast(projectId, cardId, actor);
        return CommentResponse.from(comment);
    }

    @Transactional
    public CommentResponse update(Long projectId, Long cardId, Long commentId, CommentRequest request, User actor) {
        projectAccessService.requireMember(projectId, actor);
        CardComment comment = findComment(projectId, cardId, commentId);
        if (!comment.getAuthorId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인이 쓴 댓글만 수정할 수 있습니다.");
        }

        String content = request.content().strip();
        if (!content.equals(comment.getContent())) {
            comment.edit(content);
            // 수정 시각(updated_at)을 바로 반영해야 응답과 브로드캐스트에 "(수정됨)"이 붙는다.
            commentRepository.flush();
            broadcast(projectId, cardId, actor);
        }
        return CommentResponse.from(comment);
    }

    @Transactional
    public void delete(Long projectId, Long cardId, Long commentId, User actor) {
        projectAccessService.requireMember(projectId, actor);
        CardComment comment = findComment(projectId, cardId, commentId);
        if (!comment.getAuthorId().equals(actor.getId())) {
            UserRole role = projectAccessService.myRoleOrNull(projectId, actor.getId());
            if (role == null || role == UserRole.MEMBER) {
                throw new ApiException(HttpStatus.FORBIDDEN, "본인이 쓴 댓글만 삭제할 수 있습니다.");
            }
        }

        commentRepository.delete(comment);
        commentRepository.flush();
        broadcast(projectId, cardId, actor);
    }

    private List<CommentResponse> listInternal(Long cardId) {
        return commentRepository.findAllByCardIdOrderByCreatedAtAscIdAsc(cardId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    private void broadcast(Long projectId, Long cardId, User actor) {
        realtimeEventPublisher.publish(
                RealtimeChannels.cardCommentEvents(projectId),
                new CommentEvent(cardId, listInternal(cardId))
        );
        cardService.broadcastSnapshot(projectId, "CARD_COMMENTED", actor.getName());
    }

    private Card findCard(Long projectId, Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "카드를 찾을 수 없습니다."));
        if (!card.getProjectId().equals(projectId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "카드를 찾을 수 없습니다.");
        }
        return card;
    }

    private CardComment findComment(Long projectId, Long cardId, Long commentId) {
        CardComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));
        if (!comment.getProjectId().equals(projectId) || !comment.getCardId().equals(cardId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다.");
        }
        return comment;
    }
}
