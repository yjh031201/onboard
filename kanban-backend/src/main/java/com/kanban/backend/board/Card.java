package com.kanban.backend.board;

import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    /** 카드가 들어 있는 컬럼의 id (board_columns.id). */
    @Column(nullable = false, length = 20)
    private String status;

    /** Order within its status column — 0-based, contiguous, renumbered on every move. */
    @Column(nullable = false)
    private int position;

    /** 설정 페이지 라벨의 id (예: "bug"). 라벨이 없으면 null. */
    @Column(name = "label_id", length = 30)
    private String labelId;

    @Column(name = "created_by_id", nullable = false)
    private Long createdById;

    @Column(name = "created_by_name", nullable = false, length = 100)
    private String createdByName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Card(String title, String status, int position, String labelId, Long createdById, String createdByName) {
        this.title = title;
        this.status = status;
        this.position = position;
        this.labelId = labelId;
        this.createdById = createdById;
        this.createdByName = createdByName;
    }

    public void moveTo(String status, int position) {
        this.status = status;
        this.position = position;
    }

    public void reposition(int position) {
        this.position = position;
    }

    public void changeLabel(String labelId) {
        this.labelId = labelId;
    }

    /** 작성자 본인이거나 관리자(OWNER/ADMIN)면 삭제 가능. */
    public boolean isDeletableBy(User user) {
        return createdById.equals(user.getId()) || user.getRole() != UserRole.MEMBER;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @jakarta.persistence.PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
