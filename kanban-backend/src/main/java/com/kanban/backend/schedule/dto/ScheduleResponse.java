package com.kanban.backend.schedule.dto;

import com.kanban.backend.schedule.Schedule;
import com.kanban.backend.schedule.ScheduleCategory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ScheduleResponse(
        Long id,
        String title,
        String content,
        ScheduleCategory category,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        String color,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getContent(),
                schedule.getCategory(),
                schedule.getStartDate(),
                schedule.getEndDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getColor(),
                schedule.getCreatedBy(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
