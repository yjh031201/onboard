package com.kanban.backend.schedule;

import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "schedules", indexes = @Index(name = "idx_schedule_date", columnList = "schedule_date"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Schedule(String title, String content, LocalDate scheduleDate, Long createdBy) {
        this.title = title;
        this.content = content;
        this.scheduleDate = scheduleDate;
        this.createdBy = createdBy;
    }

    public void update(String title, String content, LocalDate scheduleDate) {
        this.title = title;
        this.content = content;
        this.scheduleDate = scheduleDate;
    }

    /** 작성자 본인이거나 관리자(OWNER/ADMIN)면 수정/삭제 가능. */
    public boolean isEditableBy(User user) {
        return createdBy.equals(user.getId()) || user.getRole() != UserRole.MEMBER;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
