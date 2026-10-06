package com.kanban.backend.project.dto;

import com.kanban.backend.user.UserRole;
import jakarta.validation.constraints.NotNull;

public record InviteMemberRequest(
        @NotNull(message = "초대할 사용자를 선택해주세요.")
        Long userId,
        @NotNull(message = "권한을 선택해주세요.")
        UserRole role
) {
}
