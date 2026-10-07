package com.kanban.backend.project.dto;

import com.kanban.backend.project.ProjectMember;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRole;

public record ProjectMemberResponse(
        Long userId,
        String name,
        String email,
        String phone,
        UserRole role,
        String joinedAt
) {
    public static ProjectMemberResponse from(ProjectMember member, User user) {
        return new ProjectMemberResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                member.getRole(),
                member.getJoinedAt().toString()
        );
    }
}
