package com.kanban.backend.board;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 칸반 보드의 컬럼. 카드는 컬럼 id를 cards.status에 저장하므로 id는 생성 후 바뀌지 않는다. */
@Entity
@Table(name = "board_columns")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardColumn {

    @Id
    @Column(length = 20)
    private String id;

    @Column(nullable = false, length = 30)
    private String name;

    /** 컬럼 표시 색 — "#rrggbb" 형식. */
    @Column(nullable = false, length = 7)
    private String color;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public BoardColumn(String id, String name, String color, int position) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.position = position;
    }

    public void update(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public void reposition(int position) {
        this.position = position;
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
