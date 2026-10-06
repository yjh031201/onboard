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
 *
 * status가 PENDING인 행은 "초대는 보냈지만 아직 수락 안 한" 상태라 진짜 멤버가 아니다 —
 * ProjectAccessService가 ACCEPTED만 멤버로 인정해서, 수락 전에는 보드/라벨/타임라인 등
 * 어떤 프로젝트 리소스에도 접근할 수 없다.
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InviteStatus status;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    /** 프로젝트를 만든 사람 본인을 바로 OWNER로 넣을 때 쓴다 — 수락 절차 없이 바로 ACCEPTED. */
    public ProjectMember(Long projectId, Long userId, UserRole role) {
        this(projectId, userId, role, InviteStatus.ACCEPTED);
    }

    /** 다른 사람을 초대할 때 쓴다 — 기본 PENDING으로 생기고, 초대받은 사람이 accept()해야 한다. */
    public ProjectMember(Long projectId, Long userId, UserRole role, InviteStatus status) {
        this.projectId = projectId;
        this.userId = userId;
        this.role = role;
        this.status = status;
    }

    public void changeRole(UserRole role) {
        this.role = role;
    }

    /** 초대받은 사람이 수락 — PENDING -> ACCEPTED. */
    public void accept() {
        this.status = InviteStatus.ACCEPTED;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
    }
}
