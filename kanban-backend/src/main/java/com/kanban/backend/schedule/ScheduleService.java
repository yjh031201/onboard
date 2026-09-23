package com.kanban.backend.schedule;

import com.kanban.backend.common.ApiException;
import com.kanban.backend.schedule.dto.ScheduleCreateRequest;
import com.kanban.backend.schedule.dto.ScheduleResponse;
import com.kanban.backend.schedule.dto.ScheduleUpdateRequest;
import com.kanban.backend.user.User;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleService {

    private final ScheduleRepository repository;

    public ScheduleService(ScheduleRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getMonth(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        return repository.findOverlapping(start, end).stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    @Transactional
    public ScheduleResponse create(ScheduleCreateRequest request, User currentUser) {
        requireValidPeriod(request.startDate(), request.endDate(), request.startTime(), request.endTime());
        Schedule schedule = new Schedule(request.title(), request.content(), request.category(),
                request.startDate(), request.endDate(), request.startTime(), request.endTime(), request.color(),
                currentUser.getId());
        return ScheduleResponse.from(repository.save(schedule));
    }

    @Transactional
    public ScheduleResponse update(Long id, ScheduleUpdateRequest request, User currentUser) {
        Schedule schedule = getOrThrow(id);
        requireEditable(schedule, currentUser);
        requireValidPeriod(request.startDate(), request.endDate(), request.startTime(), request.endTime());
        schedule.update(request.title(), request.content(), request.category(),
                request.startDate(), request.endDate(), request.startTime(), request.endTime(), request.color());
        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        Schedule schedule = getOrThrow(id);
        requireEditable(schedule, currentUser);
        repository.delete(schedule);
    }

    private Schedule getOrThrow(Long id) {
        return repository.findById(id)
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

    private void requireEditable(Schedule schedule, User user) {
        if (!schedule.isEditableBy(user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인이 등록한 일정만 수정/삭제할 수 있습니다.");
        }
    }
}
