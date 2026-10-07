package com.kanban.backend.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kanban.backend.board.CardRepository;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.file.FileStorageService;
import com.kanban.backend.file.ProjectFile;
import com.kanban.backend.file.ProjectFileRepository;
import com.kanban.backend.realtime.RealtimeEventPublisher;
import com.kanban.backend.schedule.ScheduleRepository;
import com.kanban.backend.timeline.TimelineEventRepository;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRole;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

class ProjectArchiveAndDeletionTest {

    private ProjectSettingsRepository settingsRepository;
    private RealtimeEventPublisher publisher;
    private ProjectSettingsService settingsService;

    @BeforeEach
    void setUp() {
        settingsRepository = mock(ProjectSettingsRepository.class);
        publisher = mock(RealtimeEventPublisher.class);
        settingsService = new ProjectSettingsService(settingsRepository, publisher);
        when(settingsRepository.save(any(ProjectSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private User userWithRole(UserRole role) {
        User user = new User("user@example.com", "encoded", "이름", "010-1111-1111");
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "role", role);
        return user;
    }

    private ProjectSettings existingSettings(boolean archived) {
        ProjectSettings settings = new ProjectSettings();
        settings.update("우리 프로젝트", "설명", 1L);
        if (archived) {
            settings.archive(1L);
        }
        when(settingsRepository.findTopByOrderByIdAsc()).thenReturn(Optional.of(settings));
        return settings;
    }

    @Test
    void archive_adminMakesProjectReadOnly() {
        existingSettings(false);

        var response = settingsService.archive(userWithRole(UserRole.ADMIN));

        assertThat(response.archived()).isTrue();
        assertThat(response.archivedAt()).isNotNull();
        assertThat(settingsService.isArchived()).isTrue();
    }

    @Test
    void archive_worksEvenWhenSettingsWereNeverSaved() {
        when(settingsRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());

        var response = settingsService.archive(userWithRole(UserRole.OWNER));

        assertThat(response.archived()).isTrue();
        assertThat(response.projectName()).isNotBlank();
    }

    @Test
    void archive_rejectsMember() {
        existingSettings(false);

        assertThatThrownBy(() -> settingsService.archive(userWithRole(UserRole.MEMBER)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("관리자만");
    }

    @Test
    void renameTeam_adminChangesTeamNameAndKeepsProjectName() {
        existingSettings(false);

        var response = settingsService.renameTeam("  새 팀 이름  ", userWithRole(UserRole.ADMIN));

        assertThat(response.teamName()).isEqualTo("새 팀 이름");
        assertThat(response.projectName()).isEqualTo("우리 프로젝트");
    }

    @Test
    void renameTeam_worksEvenWhenSettingsWereNeverSaved() {
        when(settingsRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());

        var response = settingsService.renameTeam("새 팀 이름", userWithRole(UserRole.OWNER));

        assertThat(response.teamName()).isEqualTo("새 팀 이름");
        assertThat(response.projectName()).isNotBlank();
    }

    @Test
    void renameTeam_rejectsMember() {
        existingSettings(false);

        assertThatThrownBy(() -> settingsService.renameTeam("새 팀 이름", userWithRole(UserRole.MEMBER)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("관리자만");
    }

    @Test
    void unarchive_restoresProject() {
        existingSettings(true);

        var response = settingsService.unarchive(userWithRole(UserRole.ADMIN));

        assertThat(response.archived()).isFalse();
        assertThat(response.archivedAt()).isNull();
    }

    @Test
    void guard_blocksWritesOnlyWhileArchived() {
        ArchiveGuardInterceptor guard = new ArchiveGuardInterceptor(settingsService);
        existingSettings(true);

        assertThatThrownBy(() -> guard.preHandle(request("POST", "/api/cards"), new MockHttpServletResponse(), new Object()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.LOCKED);
        assertThatThrownBy(() -> guard.preHandle(request("PUT", "/api/settings"), new MockHttpServletResponse(), new Object()))
                .isInstanceOf(ApiException.class);

        // 조회, 그리고 보관 해제·삭제·로그인·계정 관리는 보관 중에도 된다.
        for (String[] allowed : new String[][] {
                {"GET", "/api/cards"},
                {"POST", "/api/settings/unarchive"},
                {"DELETE", "/api/settings/project"},
                {"POST", "/api/auth/login"},
                {"PATCH", "/api/users/me"},
        }) {
            assertThat(guard.preHandle(request(allowed[0], allowed[1]), new MockHttpServletResponse(), new Object()))
                    .as(allowed[0] + " " + allowed[1])
                    .isTrue();
        }

        existingSettings(false);
        assertThat(guard.preHandle(request("POST", "/api/cards"), new MockHttpServletResponse(), new Object())).isTrue();
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }

    @Test
    void deleteProject_ownerWipesEverythingIncludingStoredFiles() {
        CardRepository cards = mock(CardRepository.class);
        ScheduleRepository schedules = mock(ScheduleRepository.class);
        ProjectFileRepository files = mock(ProjectFileRepository.class);
        FileStorageService storage = mock(FileStorageService.class);
        TimelineEventRepository timeline = mock(TimelineEventRepository.class);
        when(files.findAll()).thenReturn(List.of(
                new ProjectFile("a.txt", "key-a", 1L, "text/plain", 1L),
                new ProjectFile("b.txt", "key-b", 1L, "text/plain", 1L)
        ));
        // 파일 하나를 못 지워도 나머지 삭제는 끝까지 간다.
        doThrow(new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다.")).when(storage).delete("key-a");
        ProjectDeletionService deletionService = new ProjectDeletionService(
                cards, schedules, files, storage, timeline, settingsRepository, publisher);

        assertThatCode(() -> deletionService.deleteProject(userWithRole(UserRole.OWNER))).doesNotThrowAnyException();

        verify(cards).deleteAll();
        verify(schedules).deleteAll();
        verify(files).deleteAll();
        verify(timeline).deleteAll();
        verify(settingsRepository).deleteAll();
        verify(storage).delete("key-a");
        verify(storage).delete("key-b");
    }

    @Test
    void deleteProject_rejectsAdmin() {
        CardRepository cards = mock(CardRepository.class);
        ProjectDeletionService deletionService = new ProjectDeletionService(
                cards, mock(ScheduleRepository.class), mock(ProjectFileRepository.class),
                mock(FileStorageService.class), mock(TimelineEventRepository.class), settingsRepository, publisher);

        assertThatThrownBy(() -> deletionService.deleteProject(userWithRole(UserRole.ADMIN)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("소유자만");
        verify(cards, never()).deleteAll();
    }
}
