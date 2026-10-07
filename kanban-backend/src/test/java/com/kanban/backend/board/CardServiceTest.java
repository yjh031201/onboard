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
import com.kanban.backend.realtime.RealtimeEventPublisher;
import com.kanban.backend.timeline.TimelineService;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRole;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CardServiceTest {

    private CardRepository cardRepository;
    private LabelRepository labelRepository;
    private CardService cardService;

    @BeforeEach
    void setUp() {
        cardRepository = mock(CardRepository.class);
        labelRepository = mock(LabelRepository.class);
        cardService = new CardService(
                cardRepository,
                mock(BoardColumnRepository.class),
                labelRepository,
                mock(TimelineService.class),
                mock(RealtimeEventPublisher.class)
        );
    }

    private User userWithRole(Long id, UserRole role) {
        User user = new User("user@example.com", "encoded", "이름", "010-1111-1111");
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "role", role);
        return user;
    }

    /** 1번 사용자가 만든 카드. */
    private Card existingCard() {
        Card card = new Card("로그인 화면", "TODO", 0, List.of(), null, null, 1L, "이름");
        ReflectionTestUtils.setField(card, "id", 10L);
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
        return card;
    }

    @Test
    void update_savesTrimmedDescription() {
        existingCard();

        var response = cardService.update(
                10L, new UpdateCardRequest("로그인 화면", "  소셜 로그인 버튼 추가\n에러 문구 정리  ", null), userWithRole(1L, UserRole.MEMBER));

        assertThat(response.description()).isEqualTo("소셜 로그인 버튼 추가\n에러 문구 정리");
    }

    @Test
    void update_blankOrMissingDescriptionClearsIt() {
        Card card = existingCard();
        card.update("로그인 화면", "예전 설명", null);
        User author = userWithRole(1L, UserRole.MEMBER);

        assertThat(cardService.update(10L, new UpdateCardRequest("로그인 화면", "   ", null), author).description())
                .isNull();

        card.update("로그인 화면", "예전 설명", null);
        assertThat(cardService.update(10L, new UpdateCardRequest("로그인 화면", null, null), author).description())
                .isNull();
    }

    @Test
    void update_rejectsOtherMember() {
        existingCard();

        assertThatThrownBy(() -> cardService.update(
                10L, new UpdateCardRequest("로그인 화면", "남의 카드 설명", null), userWithRole(2L, UserRole.MEMBER)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("본인이 만든 카드만");
    }

    @Test
    void changeLabels_savesTrimmedCustomLabelWithEtcLabel() {
        existingCard();
        when(labelRepository.existsById(Label.ETC_ID)).thenReturn(true);

        var response = cardService.changeLabels(
                10L, new ChangeCardLabelRequest(List.of(Label.ETC_ID), "  회의록  "), userWithRole(2L, UserRole.MEMBER));

        assertThat(response.labelIds()).containsExactly(Label.ETC_ID);
        assertThat(response.customLabel()).isEqualTo("회의록");
    }

    @Test
    void changeLabels_dropsCustomLabelWithoutEtcLabel() {
        Card card = existingCard();
        card.changeLabels(List.of(Label.ETC_ID), "회의록");
        when(labelRepository.existsById("bug")).thenReturn(true);

        var response = cardService.changeLabels(
                10L, new ChangeCardLabelRequest(List.of("bug"), "회의록"), userWithRole(2L, UserRole.MEMBER));

        assertThat(response.labelIds()).containsExactly("bug");
        assertThat(response.customLabel()).isNull();
    }
}
