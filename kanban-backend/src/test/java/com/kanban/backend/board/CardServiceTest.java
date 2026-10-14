package com.kanban.backend.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kanban.backend.board.dto.ChangeCardLabelRequest;
import com.kanban.backend.board.dto.UpdateCardRequest;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.label.Label;
import com.kanban.backend.label.LabelRepository;
import com.kanban.backend.project.ProjectAccessService;
import com.kanban.backend.project.ProjectMember;
import com.kanban.backend.realtime.RealtimeEventPublisher;
import com.kanban.backend.timeline.TimelineService;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRepository;
import com.kanban.backend.user.UserRole;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CardServiceTest {

    private static final Long PROJECT_ID = 1L;

    private CardRepository cardRepository;
    private LabelRepository labelRepository;
    private ProjectAccessService projectAccessService;
    private UserRepository userRepository;
    private CardService cardService;

    @BeforeEach
    void setUp() {
        cardRepository = mock(CardRepository.class);
        labelRepository = mock(LabelRepository.class);
        projectAccessService = mock(ProjectAccessService.class);
        userRepository = mock(UserRepository.class);
        cardService = new CardService(
                cardRepository,
                mock(BoardColumnRepository.class),
                labelRepository,
                mock(TimelineService.class),
                mock(RealtimeEventPublisher.class),
                projectAccessService,
                userRepository
        );
    }

    private User userWithId(Long id) {
        User user = new User("user@example.com", "encoded", "이름", "010-1111-1111");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    /** 1번 사용자가 만든 카드. */
    private Card existingCard() {
        Card card = new Card(PROJECT_ID, "로그인 화면", "TODO", 0, List.of(), null, null, null, null, 1L, "이름");
        ReflectionTestUtils.setField(card, "id", 10L);
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
        return card;
    }

    @Test
    void update_savesTrimmedDescription() {
        existingCard();
        User author = userWithId(1L);
        when(projectAccessService.requireMember(PROJECT_ID, author))
                .thenReturn(new ProjectMember(PROJECT_ID, 1L, UserRole.MEMBER));

        var response = cardService.update(
                PROJECT_ID, 10L, new UpdateCardRequest("로그인 화면", "  소셜 로그인 버튼 추가\n에러 문구 정리  ", null, null), author);

        assertThat(response.description()).isEqualTo("소셜 로그인 버튼 추가\n에러 문구 정리");
    }

    @Test
    void update_blankOrMissingDescriptionClearsIt() {
        Card card = existingCard();
        card.update("로그인 화면", "예전 설명", null);
        User author = userWithId(1L);
        when(projectAccessService.requireMember(PROJECT_ID, author))
                .thenReturn(new ProjectMember(PROJECT_ID, 1L, UserRole.MEMBER));

        assertThat(cardService.update(PROJECT_ID, 10L, new UpdateCardRequest("로그인 화면", "   ", null, null), author).description())
                .isNull();

        card.update("로그인 화면", "예전 설명", null);
        assertThat(cardService.update(PROJECT_ID, 10L, new UpdateCardRequest("로그인 화면", null, null, null), author).description())
                .isNull();
    }

    @Test
    void update_rejectsOtherMember() {
        existingCard();
        User other = userWithId(2L);
        when(projectAccessService.requireMember(PROJECT_ID, other))
                .thenReturn(new ProjectMember(PROJECT_ID, 2L, UserRole.MEMBER));
        when(projectAccessService.myRoleOrNull(PROJECT_ID, 2L)).thenReturn(UserRole.MEMBER);

        assertThatThrownBy(() -> cardService.update(
                PROJECT_ID, 10L, new UpdateCardRequest("로그인 화면", "남의 카드 설명", null, null), other))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("본인이 만든 카드만");
    }

    @Test
    void update_assignsProjectMember() {
        existingCard();
        User author = userWithId(1L);
        User assignee = userWithId(3L);
        ReflectionTestUtils.setField(assignee, "name", "담당자");
        when(projectAccessService.requireMember(PROJECT_ID, author))
                .thenReturn(new ProjectMember(PROJECT_ID, 1L, UserRole.MEMBER));
        when(projectAccessService.myRoleOrNull(PROJECT_ID, 3L)).thenReturn(UserRole.MEMBER);
        when(userRepository.findById(3L)).thenReturn(Optional.of(assignee));

        var response = cardService.update(PROJECT_ID, 10L, new UpdateCardRequest("로그인 화면", null, null, 3L), author);

        assertThat(response.assigneeId()).isEqualTo(3L);
        assertThat(response.assigneeName()).isEqualTo("담당자");
    }

    @Test
    void update_nullAssigneeClearsIt() {
        Card card = existingCard();
        card.assign(3L, "담당자");
        User author = userWithId(1L);
        when(projectAccessService.requireMember(PROJECT_ID, author))
                .thenReturn(new ProjectMember(PROJECT_ID, 1L, UserRole.MEMBER));

        var response = cardService.update(PROJECT_ID, 10L, new UpdateCardRequest("로그인 화면", null, null, null), author);

        assertThat(response.assigneeId()).isNull();
        assertThat(response.assigneeName()).isNull();
    }

    @Test
    void update_rejectsNonMemberAssignee() {
        existingCard();
        User author = userWithId(1L);
        when(projectAccessService.requireMember(PROJECT_ID, author))
                .thenReturn(new ProjectMember(PROJECT_ID, 1L, UserRole.MEMBER));
        when(projectAccessService.myRoleOrNull(PROJECT_ID, 99L)).thenReturn(null);

        assertThatThrownBy(() -> cardService.update(
                PROJECT_ID, 10L, new UpdateCardRequest("로그인 화면", null, null, 99L), author))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("프로젝트 멤버만 담당자로");
    }

    @Test
    void changeLabels_savesTrimmedCustomLabelWithEtcLabel() {
        existingCard();
        User author = userWithId(2L);
        when(projectAccessService.requireMember(PROJECT_ID, author))
                .thenReturn(new ProjectMember(PROJECT_ID, 2L, UserRole.MEMBER));
        Label etcLabel = new Label("etc1", PROJECT_ID, "기타", "#9ca3af", 0, true);
        when(labelRepository.existsByIdAndProjectId("etc1", PROJECT_ID)).thenReturn(true);
        when(labelRepository.findByProjectIdAndIsEtcTrue(PROJECT_ID)).thenReturn(Optional.of(etcLabel));

        var response = cardService.changeLabels(
                PROJECT_ID, 10L, new ChangeCardLabelRequest(List.of("etc1"), "  회의록  "), author);

        assertThat(response.labelIds()).containsExactly("etc1");
        assertThat(response.customLabel()).isEqualTo("회의록");
    }

    @Test
    void changeLabels_dropsCustomLabelWithoutEtcLabel() {
        Card card = existingCard();
        card.changeLabels(List.of("etc1"), "회의록");
        User author = userWithId(2L);
        when(projectAccessService.requireMember(PROJECT_ID, author))
                .thenReturn(new ProjectMember(PROJECT_ID, 2L, UserRole.MEMBER));
        when(labelRepository.existsByIdAndProjectId("bug", PROJECT_ID)).thenReturn(true);
        when(labelRepository.findByProjectIdAndIsEtcTrue(PROJECT_ID)).thenReturn(Optional.empty());

        var response = cardService.changeLabels(
                PROJECT_ID, 10L, new ChangeCardLabelRequest(List.of("bug"), "회의록"), author);

        assertThat(response.labelIds()).containsExactly("bug");
        assertThat(response.customLabel()).isNull();
    }
}
