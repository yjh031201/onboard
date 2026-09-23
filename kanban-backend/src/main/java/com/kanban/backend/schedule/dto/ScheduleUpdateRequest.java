package com.kanban.backend.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ScheduleUpdateRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String content,
        @NotNull LocalDate scheduleDate
) {
}
