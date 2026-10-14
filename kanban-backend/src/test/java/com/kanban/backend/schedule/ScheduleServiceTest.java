package com.kanban.backend.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kanban.backend.common.ApiException;
import com.kanban.backend.project.ProjectAccessService;
import com.kanban.backend.project.ProjectMember;
import com.kanban.backend.schedule.dto.ScheduleCreateRequest;
import com.kanban.backend.schedule.dto.ScheduleUpdateRequest;
import com.kanban.backend.user.AuthProvider;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRole;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

class ScheduleServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 9, 24);
    private static final Long PROJECT_ID = 1L;
    private static final Long OTHER_PROJECT_ID = 2L;

    private ScheduleRepository repository;
    private ProjectAccessService projectAccessService;
    private ScheduleService service;

    @BeforeEach
    void setUp() {
        repository = mock(ScheduleRepository.class);
        projectAccessService = mock(ProjectAccessService.class);
        service = new ScheduleService(repository, projectAccessService);
        when(repository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /** 이 프로젝트에서 role을 가진 (수락한) 멤버. 권한은 전역 users.role이 아니라 ProjectMember.role로 본다. */
    private User memberWithRole(long id, UserRole role) {
        // feature/infra와 develop 양쪽에 모두 있는 생성자를 사용 (일반 가입 생성자는 브랜치마다 인자가 다름)
        User user = new User("u" + id + "@example.com", "이름" + id, AuthProvider.GOOGLE, "google-" + id);
        ReflectionTestUtils.setField(user, "id", id);
        when(projectAccessService.requireMember(PROJECT_ID, user)).thenReturn(new ProjectMember(PROJECT_ID, id, role));
        return user;
    }

    private Schedule scheduleOwnedBy(long ownerId) {
        Schedule schedule = new Schedule(PROJECT_ID, "회의", null, ScheduleCategory.MEETING, DATE, DATE,
                LocalTime.of(10, 0), LocalTime.of(11, 0), "#6366f1", ownerId);
        ReflectionTestUtils.setField(schedule, "id", 1L);
        return schedule;
    }

    private ScheduleUpdateRequest updateRequest() {
        return new ScheduleUpdateRequest("마감", "내용", ScheduleCategory.DEADLINE, DATE, DATE, null, null, "#10b981");
    }

    @Test
    void create_savesPeriodCategoryTimeAndColor() {
        var request = new ScheduleCreateRequest("회의", null, ScheduleCategory.MEETING, DATE, DATE.plusDays(2),
                LocalTime.of(10, 0), LocalTime.of(11, 30), "#f59e0b");

        var response = service.create(PROJECT_ID, request, memberWithRole(1L, UserRole.MEMBER));

        assertThat(response.category()).isEqualTo(ScheduleCategory.MEETING);
        assertThat(response.startDate()).isEqualTo(DATE);
        assertThat(response.endDate()).isEqualTo(DATE.plusDays(2));
        assertThat(response.startTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(response.endTime()).isEqualTo(LocalTime.of(11, 30));
        assertThat(response.color()).isEqualTo("#f59e0b");
        assertThat(response.createdBy()).isEqualTo(1L);
    }

    @Test
    void create_rejectsEndTimeBeforeStartTimeOnSameDay() {
        var request = new ScheduleCreateRequest("회의", null, ScheduleCategory.MEETING, DATE, DATE,
                LocalTime.of(12, 0), LocalTime.of(9, 0), "#6366f1");

        assertThatThrownBy(() -> service.create(PROJECT_ID, request, memberWithRole(1L, UserRole.MEMBER)))
                .isInstanceOf(ApiException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void create_allowsEarlierEndTimeWhenPeriodSpansDays() {
        // 24일 18:00 ~ 25일 09:00 같은 기간 일정은 정상
        var request = new ScheduleCreateRequest("워크숍", null, ScheduleCategory.EVENT, DATE, DATE.plusDays(1),
                LocalTime.of(18, 0), LocalTime.of(9, 0), "#6366f1");

        var response = service.create(PROJECT_ID, request, memberWithRole(1L, UserRole.MEMBER));

        assertThat(response.endDate()).isEqualTo(DATE.plusDays(1));
    }

    @Test
    void create_rejectsEndDateBeforeStartDate() {
        var request = new ScheduleCreateRequest("회의", null, ScheduleCategory.MEETING, DATE, DATE.minusDays(1),
                null, null, "#6366f1");

        assertThatThrownBy(() -> service.create(PROJECT_ID, request, memberWithRole(1L, UserRole.MEMBER)))
                .isInstanceOf(ApiException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void update_allowsAuthor() {
        when(repository.findById(1L)).thenReturn(Optional.of(scheduleOwnedBy(1L)));

        var response = service.update(PROJECT_ID, 1L, updateRequest(), memberWithRole(1L, UserRole.MEMBER));

        assertThat(response.title()).isEqualTo("마감");
        assertThat(response.category()).isEqualTo(ScheduleCategory.DEADLINE);
        assertThat(response.startTime()).isNull();
    }

    @Test
    void update_allowsAdminOnOthersSchedule() {
        when(repository.findById(1L)).thenReturn(Optional.of(scheduleOwnedBy(1L)));

        var response = service.update(PROJECT_ID, 1L, updateRequest(), memberWithRole(2L, UserRole.ADMIN));

        assertThat(response.title()).isEqualTo("마감");
    }

    @Test
    void update_rejectsOtherMember() {
        when(repository.findById(1L)).thenReturn(Optional.of(scheduleOwnedBy(1L)));

        assertThatThrownBy(() -> service.update(PROJECT_ID, 1L, updateRequest(), memberWithRole(2L, UserRole.MEMBER)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void create_savesIntoGivenProject() {
        var request = new ScheduleCreateRequest("회의", null, ScheduleCategory.MEETING, DATE, DATE, null, null, "#6366f1");

        service.create(PROJECT_ID, request, memberWithRole(1L, UserRole.MEMBER));

        ArgumentCaptor<Schedule> saved = ArgumentCaptor.forClass(Schedule.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getProjectId()).isEqualTo(PROJECT_ID);
    }

    @Test
    void update_rejectsScheduleFromAnotherProject() {
        // 다른 프로젝트의 일정 id로 접근하면, 관리자여도 "없음"으로 처리한다.
        Schedule other = scheduleOwnedBy(1L);
        ReflectionTestUtils.setField(other, "projectId", OTHER_PROJECT_ID);
        when(repository.findById(1L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.update(PROJECT_ID, 1L, updateRequest(), memberWithRole(2L, UserRole.ADMIN)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("찾을 수 없습니다");
    }

    @Test
    void getMonth_rejectsNonMember() {
        User outsider = new User("x@example.com", "외부인", AuthProvider.GOOGLE, "google-x");
        ReflectionTestUtils.setField(outsider, "id", 9L);
        when(projectAccessService.requireMember(PROJECT_ID, outsider))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN, "이 프로젝트의 멤버가 아닙니다."));

        assertThatThrownBy(() -> service.getMonth(PROJECT_ID, 2026, 9, outsider))
                .isInstanceOf(ApiException.class);
        verify(repository, never()).findOverlapping(any(), any(), any());
    }
}
