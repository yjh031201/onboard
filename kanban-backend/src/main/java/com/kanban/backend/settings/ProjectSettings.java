package com.kanban.backend.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 팀 전체가 공유하는 단일 워크스페이스 설정이라 로우가 하나만 존재한다고 가정.
 * (여러 팀/워크스페이스를 지원해야 해지면 그때 team_id를 추가할 것.)
 */
@Entity
@Table(name = "project_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_name", nullable = false, length = 100)
    private String projectName;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 팀원 페이지의 팀 이름. 아직 아무도 바꾸지 않았으면 null. */
    @Column(name = "team_name", length = 100)
    private String teamName;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 보관 중이면 프로젝트 전체가 읽기 전용이다 (ArchiveGuardInterceptor가 쓰기 요청을 막는다). */
    @Column(nullable = false)
    private boolean archived;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    public void update(String projectName, String description, Long updatedBy) {
        this.projectName = projectName;
        this.description = description;
        this.updatedBy = updatedBy;
    }

    public void renameTeam(String teamName, Long updatedBy) {
        this.teamName = teamName;
        this.updatedBy = updatedBy;
    }

    public void archive(Long updatedBy) {
        this.archived = true;
        this.archivedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }

    public void unarchive(Long updatedBy) {
        this.archived = false;
        this.archivedAt = null;
        this.updatedBy = updatedBy;
    }

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = LocalDateTime.now();
    }
}
