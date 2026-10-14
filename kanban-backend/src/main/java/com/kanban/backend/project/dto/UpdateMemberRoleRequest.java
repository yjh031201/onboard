package com.kanban.backend.project.dto;

import com.kanban.backend.user.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull(message = "권한을 선택해주세요.")
        UserRole role
) {
}
