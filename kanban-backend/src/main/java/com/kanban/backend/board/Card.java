package com.kanban.backend.board;

import com.kanban.backend.label.Label;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRole;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card {

    /** 카드 하나에 붙일 수 있는 라벨 수. */
    public static final int MAX_LABELS = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    /** 카드 설명. 없으면 null. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 카드가 들어 있는 컬럼의 id (board_columns.id). */
    @Column(nullable = false, length = 20)
    private String status;

    /** Order within its status column — 0-based, contiguous, renumbered on every move. */
    @Column(nullable = false)
    private int position;

    /** 설정 페이지 라벨의 id 목록 (예: "bug"), 붙인 순서대로. 최대 MAX_LABELS개. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "card_labels", joinColumns = @JoinColumn(name = "card_id"))
    @OrderColumn(name = "position")
    @Column(name = "label_id", length = 30, nullable = false)
    private List<String> labelIds = new ArrayList<>();

    /** "기타" 라벨(Label.ETC_ID)에 직접 적은 글자. 기타 라벨이 안 붙어 있거나 안 적었으면 null. */
    @Column(name = "custom_label", length = 20)
    private String customLabel;

    /** 마감 일시. 없으면 null. */
    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "created_by_id", nullable = false)
    private Long createdById;

    @Column(name = "created_by_name", nullable = false, length = 100)
    private String createdByName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Card(
            String title,
            String status,
            int position,
            List<String> labelIds,
            String customLabel,
            LocalDateTime dueAt,
            Long createdById,
            String createdByName
    ) {
        this.title = title;
        this.status = status;
        this.position = position;
        this.labelIds = new ArrayList<>(labelIds);
        this.customLabel = customLabel;
        this.dueAt = dueAt;
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

    public void changeLabels(List<String> labelIds, String customLabel) {
        this.labelIds.clear();
        this.labelIds.addAll(labelIds);
        this.customLabel = customLabel;
    }

    /** 라벨이 삭제됐을 때 — 붙어 있었으면 떼고 true. */
    public boolean removeLabel(String labelId) {
        if (Label.ETC_ID.equals(labelId)) {
            this.customLabel = null;
        }
        return this.labelIds.remove(labelId);
    }

    public void update(String title, String description, LocalDateTime dueAt) {
        this.title = title;
        this.description = description;
        this.dueAt = dueAt;
    }

    /** 작성자 본인이거나 관리자(OWNER/ADMIN)면 수정·삭제 가능. */
    public boolean isManageableBy(User user) {
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
