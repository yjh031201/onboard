package com.kanban.backend.schedule;

import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "schedules", indexes = @Index(name = "idx_schedule_date", columnList = "start_date"))
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleCategory category;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** 하루짜리 일정이면 startDate와 같다. */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(length = 7)
    private String color;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Schedule(String title, String content, ScheduleCategory category, LocalDate startDate, LocalDate endDate,
                    LocalTime startTime, LocalTime endTime, String color, Long createdBy) {
        this.createdBy = createdBy;
        update(title, content, category, startDate, endDate, startTime, endTime, color);
    }

    public void update(String title, String content, ScheduleCategory category, LocalDate startDate, LocalDate endDate,
                       LocalTime startTime, LocalTime endTime, String color) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.color = color;
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
