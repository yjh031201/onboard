package com.kanban.backend.schedule.dto;

import com.kanban.backend.schedule.ScheduleCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleUpdateRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String content,
        @NotNull ScheduleCategory category,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "색상은 #RRGGBB 형식이어야 합니다.") String color
) {
}
