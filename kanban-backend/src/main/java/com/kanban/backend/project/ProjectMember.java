package com.kanban.backend.project;

import com.kanban.backend.user.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 한 사용자가 한 프로젝트에서 갖는 권한. (project_id, user_id)는 유일하다.
 * 지금까지 users.role이 전역으로 하던 역할을 프로젝트 범위로 좁힌 것 — 권한 체크는
 * 이제 전부 이 role을 봐야 한다 (user/UserRole.java의 OWNER/ADMIN/MEMBER를 그대로 재사용).
 */
@Entity
@Table(name = "project_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    public ProjectMember(Long projectId, Long userId, UserRole role) {
        this.projectId = projectId;
        this.userId = userId;
        this.role = role;
    }

    public void changeRole(UserRole role) {
        this.role = role;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
    }
}
