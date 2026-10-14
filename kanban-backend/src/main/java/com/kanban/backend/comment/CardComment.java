package com.kanban.backend.comment;

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
@Table(name = "card_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardComment {

    /** 댓글 하나의 글자 수 제한 (CommentRequest와 같음). */
    public static final int MAX_CONTENT_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "author_name", nullable = false, length = 100)
    private String authorName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CardComment(Long projectId, Long cardId, Long authorId, String authorName, String content) {
        this.projectId = projectId;
        this.cardId = cardId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.content = content;
    }

    public void edit(String content) {
        this.content = content;
    }

    /** 수정한 적이 있으면 true — 화면에 "(수정됨)"을 붙인다. */
    public boolean isEdited() {
        return updatedAt != null && createdAt != null && updatedAt.isAfter(createdAt);
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
