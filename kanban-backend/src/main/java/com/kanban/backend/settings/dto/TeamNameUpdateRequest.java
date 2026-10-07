package com.kanban.backend.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamNameUpdateRequest(
        @NotBlank @Size(max = 100) String teamName
) {
}
