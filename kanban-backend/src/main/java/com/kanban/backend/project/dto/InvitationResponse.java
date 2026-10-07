package com.kanban.backend.project.dto;

import com.kanban.backend.project.Project;
import com.kanban.backend.project.ProjectMember;
import com.kanban.backend.user.UserRole;

/** 나에게 온(아직 수락/거절 안 한) 프로젝트 초대 하나. */
public record InvitationResponse(
        Long projectId,
        String projectName,
        String projectDescription,
        /** 수락하면 내가 이 프로젝트에서 갖게 될 role. */
        UserRole role,
        String invitedAt
) {
    public static InvitationResponse from(ProjectMember member, Project project) {
        return new InvitationResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                member.getRole(),
                member.getJoinedAt().toString()
        );
    }
}
