package com.kanban.backend.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kanban.backend.board.Card;
import com.kanban.backend.board.CardRepository;
import com.kanban.backend.board.CardService;
import com.kanban.backend.comment.dto.CommentRequest;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.project.ProjectAccessService;
import com.kanban.backend.realtime.RealtimeEventPublisher;
import com.kanban.backend.timeline.TimelineService;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRole;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CardCommentServiceTest {

    private static final Long PROJECT_ID = 1L;
    private static final Long CARD_ID = 10L;

    private CardCommentRepository commentRepository;
    private CardRepository cardRepository;
    private ProjectAccessService projectAccessService;
    private CardCommentService commentService;

    @BeforeEach
    void setUp() {
        commentRepository = mock(CardCommentRepository.class);
        cardRepository = mock(CardRepository.class);
        projectAccessService = mock(ProjectAccessService.class);
        commentService = new CardCommentService(
                commentRepository,
                cardRepository,
                mock(CardService.class),
                projectAccessService,
                mock(TimelineService.class),
                mock(RealtimeEventPublisher.class)
        );

        Card card = new Card(PROJECT_ID, "로그인 화면", "TODO", 0, List.of(), null, null, null, null, 1L, "이름");
        ReflectionTestUtils.setField(card, "id", CARD_ID);
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
    }

    private User userWithId(Long id) {
        User user = new User("user" + id + "@example.com", "encoded", "사용자" + id, null);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    /** 1번 사용자가 쓴 댓글. */
    private CardComment existingComment() {
        CardComment comment = new CardComment(PROJECT_ID, CARD_ID, 1L, "사용자1", "원래 내용");
        ReflectionTestUtils.setField(comment, "id", 100L);
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        return comment;
    }

    @Test
    void create_savesTrimmedContentAsActor() {
        User author = userWithId(2L);
        when(commentRepository.save(any(CardComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = commentService.create(PROJECT_ID, CARD_ID, new CommentRequest("  확인했어요  "), author);

        assertThat(response.content()).isEqualTo("확인했어요");
        assertThat(response.authorId()).isEqualTo(2L);
        assertThat(response.authorName()).isEqualTo("사용자2");
    }

    @Test
    void create_rejectsCardOfAnotherProject() {
        assertThatThrownBy(() -> commentService.create(2L, CARD_ID, new CommentRequest("내용"), userWithId(1L)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("카드를 찾을 수 없습니다");
    }

    @Test
    void update_allowsOnlyAuthor() {
        existingComment();

        assertThatThrownBy(() -> commentService.update(
                PROJECT_ID, CARD_ID, 100L, new CommentRequest("남의 댓글"), userWithId(2L)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("본인이 쓴 댓글만 수정");

        var response = commentService.update(PROJECT_ID, CARD_ID, 100L, new CommentRequest("고친 내용"), userWithId(1L));
        assertThat(response.content()).isEqualTo("고친 내용");
    }

    @Test
    void delete_rejectsOtherMember() {
        CardComment comment = existingComment();
        when(projectAccessService.myRoleOrNull(PROJECT_ID, 2L)).thenReturn(UserRole.MEMBER);

        assertThatThrownBy(() -> commentService.delete(PROJECT_ID, CARD_ID, 100L, userWithId(2L)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("본인이 쓴 댓글만 삭제");
        verify(commentRepository, never()).delete(comment);
    }

    @Test
    void delete_allowsAdmin() {
        CardComment comment = existingComment();
        when(projectAccessService.myRoleOrNull(PROJECT_ID, 3L)).thenReturn(UserRole.ADMIN);

        commentService.delete(PROJECT_ID, CARD_ID, 100L, userWithId(3L));

        verify(commentRepository).delete(comment);
    }
}
