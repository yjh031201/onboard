package com.kanban.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kanban.backend.auth.RefreshTokenStore;
import com.kanban.backend.auth.oauth2.OAuthExchangeCodeRepository;
import com.kanban.backend.board.CardAttachmentRepository;
import com.kanban.backend.board.CardRepository;
import com.kanban.backend.comment.CardCommentRepository;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.file.FileStorageService;
import com.kanban.backend.file.ProjectFile;
import com.kanban.backend.file.ProjectFileRepository;
import com.kanban.backend.integration.IntegrationRepository;
import com.kanban.backend.project.InviteStatus;
import com.kanban.backend.project.Project;
import com.kanban.backend.project.ProjectMember;
import com.kanban.backend.project.ProjectMemberRepository;
import com.kanban.backend.project.ProjectRepository;
import com.kanban.backend.project.ProjectService;
import com.kanban.backend.schedule.ScheduleRepository;
import com.kanban.backend.timeline.TimelineEvent;
import com.kanban.backend.timeline.TimelineEventRepository;
import com.kanban.backend.timeline.TimelineEventType;
import com.kanban.backend.user.dto.DeleteAccountRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class AccountDeletionServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long PROJECT_ID = 10L;
    private static final String PASSWORD = "password1234";

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserRepository userRepository;
    private ProjectRepository projectRepository;
    private ProjectMemberRepository projectMemberRepository;
    private ProjectService projectService;
    private ProjectFileRepository projectFileRepository;
    private FileStorageService fileStorageService;
    private ScheduleRepository scheduleRepository;
    private IntegrationRepository integrationRepository;
    private CardRepository cardRepository;
    private TimelineEventRepository timelineEventRepository;
    private RefreshTokenStore refreshTokenStore;
    private AccountDeletionService accountDeletionService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        projectRepository = mock(ProjectRepository.class);
        projectMemberRepository = mock(ProjectMemberRepository.class);
        projectService = mock(ProjectService.class);
        projectFileRepository = mock(ProjectFileRepository.class);
        fileStorageService = mock(FileStorageService.class);
        scheduleRepository = mock(ScheduleRepository.class);
        integrationRepository = mock(IntegrationRepository.class);
        cardRepository = mock(CardRepository.class);
        timelineEventRepository = mock(TimelineEventRepository.class);
        refreshTokenStore = mock(RefreshTokenStore.class);
        accountDeletionService = new AccountDeletionService(
                userRepository,
                passwordEncoder,
                projectRepository,
                projectMemberRepository,
                projectService,
                projectFileRepository,
                fileStorageService,
                scheduleRepository,
                integrationRepository,
                mock(OAuthExchangeCodeRepository.class),
                cardRepository,
                mock(CardAttachmentRepository.class),
                mock(CardCommentRepository.class),
                timelineEventRepository,
                refreshTokenStore
        );
    }

    private User localUser() {
        User user = new User("user@example.com", passwordEncoder.encode(PASSWORD), "홍길동", "010-1111-1111");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private User socialUser() {
        User user = new User("social@example.com", "홍길동", AuthProvider.GOOGLE, "google-123");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    /** USER_ID가 OWNER인 프로젝트 하나를 만들고, 같이 있는 다른 수락 멤버들을 지정한다. */
    private void ownsProjectWith(ProjectMember... others) {
        ProjectMember mine = new ProjectMember(PROJECT_ID, USER_ID, UserRole.OWNER);
        when(projectMemberRepository.findAllByUserId(USER_ID)).thenReturn(List.of(mine));

        List<ProjectMember> accepted = new ArrayList<>(List.of(others));
        accepted.add(mine);
        when(projectMemberRepository.findAllByProjectIdAndStatus(PROJECT_ID, InviteStatus.ACCEPTED))
                .thenReturn(accepted);
    }

    @Test
    void delete_wrongPassword_rejectsAndDeletesNothing() {
        User user = localUser();

        assertThatThrownBy(() -> accountDeletionService.delete(user, new DeleteAccountRequest("wrong-password")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("비밀번호");

        verify(userRepository, never()).deleteById(anyLong());
    }

    @Test
    void delete_missingPassword_rejectsForLocalAccount() {
        User user = localUser();

        assertThatThrownBy(() -> accountDeletionService.delete(user, null))
                .isInstanceOf(ApiException.class);

        verify(userRepository, never()).deleteById(anyLong());
    }

    @Test
    void delete_socialAccount_needsNoPassword() {
        accountDeletionService.delete(socialUser(), null);

        verify(userRepository).deleteById(USER_ID);
    }

    @Test
    void delete_soleMemberProject_isDeletedWithAccount() {
        User user = localUser();
        ownsProjectWith();

        accountDeletionService.delete(user, new DeleteAccountRequest(PASSWORD));

        verify(projectService).delete(PROJECT_ID, user);
        verify(projectMemberRepository).deleteAllByUserId(USER_ID);
        verify(userRepository).deleteById(USER_ID);
        verify(refreshTokenStore).invalidate(USER_ID);
    }

    @Test
    void delete_onlyOwnerOfSharedProject_isBlocked() {
        User user = localUser();
        ownsProjectWith(new ProjectMember(PROJECT_ID, OTHER_USER_ID, UserRole.MEMBER));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(new Project("우리 프로젝트", "설명")));

        assertThatThrownBy(() -> accountDeletionService.delete(user, new DeleteAccountRequest(PASSWORD)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("우리 프로젝트");

        verify(projectService, never()).delete(anyLong(), any());
        verify(userRepository, never()).deleteById(anyLong());
    }

    @Test
    void delete_sharedProjectWithAnotherOwner_leavesProjectAlone() {
        User user = localUser();
        ownsProjectWith(new ProjectMember(PROJECT_ID, OTHER_USER_ID, UserRole.OWNER));

        accountDeletionService.delete(user, new DeleteAccountRequest(PASSWORD));

        verify(projectService, never()).delete(anyLong(), any());
        verify(projectMemberRepository).deleteAllByUserId(USER_ID);
        verify(userRepository).deleteById(USER_ID);
    }

    @Test
    void delete_removesOwnFilesSchedulesAndIntegrations() {
        User user = localUser();
        ProjectFile file = new ProjectFile(PROJECT_ID, "회의록.pdf", "stored-key", 100L, "application/pdf", USER_ID);
        when(projectFileRepository.findAllByUploadedBy(USER_ID)).thenReturn(List.of(file));

        accountDeletionService.delete(user, new DeleteAccountRequest(PASSWORD));

        verify(projectFileRepository).deleteAll(List.of(file));
        verify(fileStorageService).delete("stored-key");
        verify(scheduleRepository).deleteAllByCreatedBy(USER_ID);
        verify(integrationRepository).deleteAllByConnectedById(USER_ID);
    }

    @Test
    void delete_keepsTeamRecordsButRemovesName() {
        User user = localUser();
        TimelineEvent event = new TimelineEvent(
                PROJECT_ID, TimelineEventType.CARD_CREATED, "홍길동님이 '로그인' 카드를 할 일에 추가했습니다",
                USER_ID, "홍길동", false);
        when(timelineEventRepository.findAllByActorId(USER_ID)).thenReturn(List.of(event));

        accountDeletionService.delete(user, new DeleteAccountRequest(PASSWORD));

        verify(cardRepository).renameCreator(USER_ID, AccountDeletionService.WITHDRAWN_USER_NAME);
        assertThat(event.getActorName()).isEqualTo("탈퇴한 사용자");
        assertThat(event.getMessage()).isEqualTo("탈퇴한 사용자님이 '로그인' 카드를 할 일에 추가했습니다");
    }
}
