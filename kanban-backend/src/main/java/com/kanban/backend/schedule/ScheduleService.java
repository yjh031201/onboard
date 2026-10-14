package com.kanban.backend.schedule;

import com.kanban.backend.common.ApiException;
import com.kanban.backend.project.ProjectAccessService;
import com.kanban.backend.project.ProjectMember;
import com.kanban.backend.schedule.dto.ScheduleCreateRequest;
import com.kanban.backend.schedule.dto.ScheduleResponse;
import com.kanban.backend.schedule.dto.ScheduleUpdateRequest;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRole;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 프로젝트별 일정 — 그 프로젝트의 멤버만 보고 추가할 수 있다. */
@Service
public class ScheduleService {

    private final ScheduleRepository repository;
    private final ProjectAccessService projectAccessService;

    public ScheduleService(ScheduleRepository repository, ProjectAccessService projectAccessService) {
        this.repository = repository;
        this.projectAccessService = projectAccessService;
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getMonth(Long projectId, int year, int month, User currentUser) {
        projectAccessService.requireMember(projectId, currentUser);
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        return repository.findOverlapping(projectId, start, end).stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    @Transactional
    public ScheduleResponse create(Long projectId, ScheduleCreateRequest request, User currentUser) {
        projectAccessService.requireMember(projectId, currentUser);
        requireValidPeriod(request.startDate(), request.endDate(), request.startTime(), request.endTime());
        Schedule schedule = new Schedule(projectId, request.title(), request.content(), request.category(),
                request.startDate(), request.endDate(), request.startTime(), request.endTime(), request.color(),
                currentUser.getId());
        return ScheduleResponse.from(repository.save(schedule));
    }

    @Transactional
    public ScheduleResponse update(Long projectId, Long id, ScheduleUpdateRequest request, User currentUser) {
        ProjectMember member = projectAccessService.requireMember(projectId, currentUser);
        Schedule schedule = getOrThrow(projectId, id);
        requireEditable(schedule, currentUser, member);
        requireValidPeriod(request.startDate(), request.endDate(), request.startTime(), request.endTime());
        schedule.update(request.title(), request.content(), request.category(),
                request.startDate(), request.endDate(), request.startTime(), request.endTime(), request.color());
        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public void delete(Long projectId, Long id, User currentUser) {
        ProjectMember member = projectAccessService.requireMember(projectId, currentUser);
        Schedule schedule = getOrThrow(projectId, id);
        requireEditable(schedule, currentUser, member);
        repository.delete(schedule);
    }

    /** 다른 프로젝트의 일정 id를 넣어도 "없음"으로 처리한다 — 존재 여부도 알려주지 않는다. */
    private Schedule getOrThrow(Long projectId, Long id) {
        return repository.findById(id)
                .filter(schedule -> schedule.getProjectId().equals(projectId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."));
    }

    /** 시작 시간은 시작일, 종료 시간은 종료일 기준이라 시간 비교는 하루짜리 일정일 때만 의미가 있다. */
    private void requireValidPeriod(LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime) {
        if (endDate.isBefore(startDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "종료일은 시작일보다 빠를 수 없습니다.");
        }
        if (startDate.equals(endDate) && startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "종료 시간은 시작 시간보다 빠를 수 없습니다.");
        }
    }

    private void requireEditable(Schedule schedule, User user, ProjectMember member) {
        boolean isAdmin = member.getRole() != UserRole.MEMBER;
        if (!schedule.isEditableBy(user, isAdmin)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인이 등록한 일정만 수정/삭제할 수 있습니다.");
        }
    }
}
