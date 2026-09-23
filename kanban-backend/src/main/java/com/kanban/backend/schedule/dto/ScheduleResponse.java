package com.kanban.backend.schedule.dto;

import com.kanban.backend.schedule.Schedule;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ScheduleResponse(
        Long id,
        String title,
        String content,
        LocalDate scheduleDate,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getContent(),
                schedule.getScheduleDate(),
                schedule.getCreatedBy(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
