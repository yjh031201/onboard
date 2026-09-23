package com.kanban.backend.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kanban.backend.common.ApiException;
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
import org.springframework.test.util.ReflectionTestUtils;

class ScheduleServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 9, 24);

    private ScheduleRepository repository;
    private ScheduleService service;

    @BeforeEach
    void setUp() {
        repository = mock(ScheduleRepository.class);
        service = new ScheduleService(repository);
        when(repository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private User userWithId(long id, UserRole role) {
        // feature/infra와 develop 양쪽에 모두 있는 생성자를 사용 (일반 가입 생성자는 브랜치마다 인자가 다름)
        User user = new User("u" + id + "@example.com", "이름" + id, AuthProvider.GOOGLE, "google-" + id);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "role", role);
        return user;
    }

    private Schedule scheduleOwnedBy(long ownerId) {
        Schedule schedule = new Schedule("회의", null, ScheduleCategory.MEETING, DATE,
                LocalTime.of(10, 0), LocalTime.of(11, 0), "#6366f1", ownerId);
        ReflectionTestUtils.setField(schedule, "id", 1L);
        return schedule;
    }

    private ScheduleUpdateRequest updateRequest() {
        return new ScheduleUpdateRequest("마감", "내용", ScheduleCategory.DEADLINE, DATE, null, null, "#10b981");
    }

    @Test
    void create_savesCategoryTimeAndColor() {
        var request = new ScheduleCreateRequest("회의", null, ScheduleCategory.MEETING, DATE,
                LocalTime.of(10, 0), LocalTime.of(11, 30), "#f59e0b");

        var response = service.create(request, userWithId(1L, UserRole.MEMBER));

        assertThat(response.category()).isEqualTo(ScheduleCategory.MEETING);
        assertThat(response.startTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(response.endTime()).isEqualTo(LocalTime.of(11, 30));
        assertThat(response.color()).isEqualTo("#f59e0b");
        assertThat(response.createdBy()).isEqualTo(1L);
    }

    @Test
    void create_rejectsEndTimeBeforeStartTime() {
        var request = new ScheduleCreateRequest("회의", null, ScheduleCategory.MEETING, DATE,
                LocalTime.of(12, 0), LocalTime.of(9, 0), "#6366f1");

        assertThatThrownBy(() -> service.create(request, userWithId(1L, UserRole.MEMBER)))
                .isInstanceOf(ApiException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void update_allowsAuthor() {
        when(repository.findById(1L)).thenReturn(Optional.of(scheduleOwnedBy(1L)));

        var response = service.update(1L, updateRequest(), userWithId(1L, UserRole.MEMBER));

        assertThat(response.title()).isEqualTo("마감");
        assertThat(response.category()).isEqualTo(ScheduleCategory.DEADLINE);
        assertThat(response.startTime()).isNull();
    }

    @Test
    void update_allowsAdminOnOthersSchedule() {
        when(repository.findById(1L)).thenReturn(Optional.of(scheduleOwnedBy(1L)));

        var response = service.update(1L, updateRequest(), userWithId(2L, UserRole.ADMIN));

        assertThat(response.title()).isEqualTo("마감");
    }

    @Test
    void update_rejectsOtherMember() {
        when(repository.findById(1L)).thenReturn(Optional.of(scheduleOwnedBy(1L)));

        assertThatThrownBy(() -> service.update(1L, updateRequest(), userWithId(2L, UserRole.MEMBER)))
                .isInstanceOf(ApiException.class);
    }
}
