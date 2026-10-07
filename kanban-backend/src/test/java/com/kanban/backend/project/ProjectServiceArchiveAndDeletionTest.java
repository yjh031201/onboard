package com.kanban.backend.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kanban.backend.board.BoardColumnRepository;
import com.kanban.backend.board.CardRepository;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.label.LabelRepository;
import com.kanban.backend.realtime.RealtimeEventPublisher;
import com.kanban.backend.timeline.TimelineEventRepository;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRole;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** 프로젝트 보관/삭제 — 멀티 프로젝트 전환 이후: 권한은 전역 role이 아니라 이 프로젝트에서의 ProjectMember.role로 본다. */
class ProjectServiceArchiveAndDeletionTest {

    private static final Long PROJECT_ID = 1L;

    private ProjectRepository projectRepository;
    private ProjectMemberRepository projectMemberRepository;
    private ProjectAccessService projectAccessService;
    private CardRepository cardRepository;
    private BoardColumnRepository boardColumnRepository;
    private LabelRepository labelRepository;
    private TimelineEventRepository timelineEventRepository;
    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        projectMemberRepository = mock(ProjectMemberRepository.class);
        projectAccessService = mock(ProjectAccessService.class);
        cardRepository = mock(CardRepository.class);
        boardColumnRepository = mock(BoardColumnRepository.class);
        labelRepository = mock(LabelRepository.class);
        timelineEventRepository = mock(TimelineEventRepository.class);
        projectService = new ProjectService(
                projectRepository,
                projectMemberRepository,
                projectAccessService,
                boardColumnRepository,
                labelRepository,
                cardRepository,
                timelineEventRepository,
                mock(RealtimeEventPublisher.class)
        );
    }

    private User userWithId(Long id) {
        User user = new User("user@example.com", "encoded", "이름", "010-1111-1111");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Project existingProject() {
        Project project = new Project("우리 프로젝트", "설명");
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        return project;
    }

    @Test
    void archive_adminMakesProjectReadOnly() {
        Project project = existingProject();
        User admin = userWithId(1L);
        ProjectMember membership = new ProjectMember(PROJECT_ID, 1L, UserRole.ADMIN);
        when(projectAccessService.requireAdmin(PROJECT_ID, admin)).thenReturn(membership);

        var response = projectService.archive(PROJECT_ID, admin);

        assertThat(response.archived()).isTrue();
        assertThat(response.archivedAt()).isNotNull();
        assertThat(project.isArchived()).isTrue();
    }

    @Test
    void archive_rejectsMember() {
        existingProject();
        User member = userWithId(2L);
        when(projectAccessService.requireAdmin(eq(PROJECT_ID), any()))
                .thenThrow(new ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "이 작업은 프로젝트의 소유자/관리자만 할 수 있습니다."));

        assertThatThrownBy(() -> projectService.archive(PROJECT_ID, member))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("소유자/관리자만");
    }

    @Test
    void unarchive_restoresProject() {
        Project project = existingProject();
        project.archive();
        User admin = userWithId(1L);
        when(projectAccessService.requireAdmin(PROJECT_ID, admin))
                .thenReturn(new ProjectMember(PROJECT_ID, 1L, UserRole.OWNER));

        var response = projectService.unarchive(PROJECT_ID, admin);

        assertThat(response.archived()).isFalse();
        assertThat(response.archivedAt()).isNull();
    }

    @Test
    void delete_ownerWipesProjectScopedData() {
        existingProject();
        User owner = userWithId(1L);
        when(projectAccessService.requireOwner(PROJECT_ID, owner))
                .thenReturn(new ProjectMember(PROJECT_ID, 1L, UserRole.OWNER));

        projectService.delete(PROJECT_ID, owner);

        verify(cardRepository).deleteAllByProjectId(PROJECT_ID);
        verify(boardColumnRepository).deleteAllByProjectId(PROJECT_ID);
        verify(labelRepository).deleteAllByProjectId(PROJECT_ID);
        verify(timelineEventRepository).deleteAllByProjectId(PROJECT_ID);
        verify(projectMemberRepository).deleteAllByProjectId(PROJECT_ID);
        verify(projectRepository).deleteById(PROJECT_ID);
    }

    @Test
    void delete_rejectsAdmin() {
        existingProject();
        User admin = userWithId(2L);
        when(projectAccessService.requireOwner(eq(PROJECT_ID), any()))
                .thenThrow(new ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "이 작업은 프로젝트의 소유자만 할 수 있습니다."));

        assertThatThrownBy(() -> projectService.delete(PROJECT_ID, admin))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("소유자만");
        verify(cardRepository, never()).deleteAllByProjectId(any());
    }
}
