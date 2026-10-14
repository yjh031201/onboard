package com.kanban.backend.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kanban.backend.board.dto.AttachRequest;
import com.kanban.backend.board.dto.ImportGithubIssuesRequest;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.integration.drive.DriveIntegrationService;
import com.kanban.backend.integration.drive.dto.DriveFileResponse;
import com.kanban.backend.integration.github.GithubIntegrationService;
import com.kanban.backend.integration.github.dto.GithubIssueDetail;
import com.kanban.backend.project.ProjectAccessService;
import com.kanban.backend.timeline.TimelineEventType;
import com.kanban.backend.timeline.TimelineService;
import com.kanban.backend.user.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

class CardAttachmentServiceTest {

    private static final Long PROJECT_ID = 1L;
    private static final String REPO = "team/onboard";

    private CardService cardService;
    private CardAttachmentRepository attachmentRepository;
    private GithubIntegrationService githubIntegrationService;
    private DriveIntegrationService driveIntegrationService;
    private TimelineService timelineService;
    private CardAttachmentService service;

    @BeforeEach
    void setUp() {
        cardService = mock(CardService.class);
        attachmentRepository = mock(CardAttachmentRepository.class);
        githubIntegrationService = mock(GithubIntegrationService.class);
        driveIntegrationService = mock(DriveIntegrationService.class);
        timelineService = mock(TimelineService.class);
        service = new CardAttachmentService(
                cardService,
                attachmentRepository,
                githubIntegrationService,
                driveIntegrationService,
                timelineService,
                mock(ProjectAccessService.class)
        );
        when(githubIntegrationService.linkedRepoFullName(PROJECT_ID)).thenReturn(REPO);
    }

    private User userWithId(Long id) {
        User user = new User("user" + id + "@example.com", "encoded", "사용자" + id, null);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Card cardWithId(Long id, Long createdById) {
        Card card = new Card(PROJECT_ID, "카드", "TODO", 0, List.of(), null, null, null, null, createdById, "작성자");
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }

    @Test
    void importGithubIssues_skipsAlreadyImportedAndNotifiesOnce() {
        User actor = userWithId(1L);
        when(attachmentRepository.existsByProjectIdAndKindAndExternalId(PROJECT_ID, AttachmentKind.GITHUB_ISSUE, REPO + "#1"))
                .thenReturn(true);
        when(githubIntegrationService.getIssue(PROJECT_ID, 2L))
                .thenReturn(new GithubIssueDetail(2, "로그인 버그", "  재현 방법\n1. 로그인  ", "https://github.com/team/onboard/issues/2"));
        when(cardService.createImported(eq(PROJECT_ID), eq("TODO"), anyString(), any(), eq(actor)))
                .thenReturn(cardWithId(20L, 1L));

        service.importGithubIssues(PROJECT_ID, new ImportGithubIssuesRequest("TODO", List.of(1L, 2L, 2L)), actor);

        verify(githubIntegrationService, never()).getIssue(PROJECT_ID, 1L);
        verify(cardService).createImported(PROJECT_ID, "TODO", "#2 로그인 버그", "재현 방법\n1. 로그인", actor);

        ArgumentCaptor<CardAttachment> saved = ArgumentCaptor.forClass(CardAttachment.class);
        verify(attachmentRepository).save(saved.capture());
        assertThat(saved.getValue().getCardId()).isEqualTo(20L);
        assertThat(saved.getValue().getExternalId()).isEqualTo(REPO + "#2");
        assertThat(saved.getValue().getUrl()).isEqualTo("https://github.com/team/onboard/issues/2");

        verify(timelineService, times(1))
                .record(eq(PROJECT_ID), eq(TimelineEventType.CARD_CREATED), anyString(), eq(actor), eq(true));
    }

    @Test
    void importGithubIssues_rejectsWhenEverythingWasImported() {
        User actor = userWithId(1L);
        when(attachmentRepository.existsByProjectIdAndKindAndExternalId(eq(PROJECT_ID), eq(AttachmentKind.GITHUB_ISSUE), anyString()))
                .thenReturn(true);

        assertThatThrownBy(() ->
                service.importGithubIssues(PROJECT_ID, new ImportGithubIssuesRequest("TODO", List.of(1L)), actor))
                .isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);
        verify(timelineService, never()).record(any(), any(), anyString(), any(), anyBoolean());
    }

    @Test
    void attach_driveFileUsesNameAndLinkFromDrive() {
        User actor = userWithId(2L);
        Card card = cardWithId(10L, 1L);
        when(cardService.requireCard(PROJECT_ID, 10L, actor)).thenReturn(card);
        when(driveIntegrationService.getFile(PROJECT_ID, "abc_123"))
                .thenReturn(new DriveFileResponse("abc_123", "기획서.docx", "https://drive.google.com/file/d/abc_123/view",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "2026-10-14T00:00:00Z"));

        service.attach(PROJECT_ID, 10L, new AttachRequest(AttachmentKind.DRIVE_FILE, " abc_123 "), actor);

        ArgumentCaptor<CardAttachment> saved = ArgumentCaptor.forClass(CardAttachment.class);
        verify(attachmentRepository).save(saved.capture());
        assertThat(saved.getValue().getTitle()).isEqualTo("기획서.docx");
        assertThat(saved.getValue().getUrl()).isEqualTo("https://drive.google.com/file/d/abc_123/view");
        assertThat(saved.getValue().getAttachedById()).isEqualTo(2L);
    }

    @Test
    void attach_rejectsWhenCardIsFull() {
        User actor = userWithId(1L);
        when(cardService.requireCard(PROJECT_ID, 10L, actor)).thenReturn(cardWithId(10L, 1L));
        when(attachmentRepository.countByCardId(10L)).thenReturn((long) CardAttachmentService.MAX_ATTACHMENTS_PER_CARD);

        assertThatThrownBy(() ->
                service.attach(PROJECT_ID, 10L, new AttachRequest(AttachmentKind.GITHUB_ISSUE, "3"), actor))
                .isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void detach_rejectsOtherMemberWhoCannotManageCard() {
        User other = userWithId(3L);
        Card card = cardWithId(10L, 1L);
        when(cardService.requireCard(PROJECT_ID, 10L, other)).thenReturn(card);
        when(cardService.isManageable(PROJECT_ID, card, other)).thenReturn(false);
        CardAttachment attachment = new CardAttachment(10L, PROJECT_ID, AttachmentKind.DRIVE_FILE, "abc", "파일",
                "https://drive.google.com/file/d/abc/view", null, 2L, "사용자2");
        when(attachmentRepository.findById(5L)).thenReturn(Optional.of(attachment));

        assertThatThrownBy(() -> service.detach(PROJECT_ID, 10L, 5L, other))
                .isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
        verify(attachmentRepository, never()).delete(any());
    }
}
