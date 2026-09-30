package com.kanban.backend.label;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 카드에 붙이는 라벨. 카드는 id만 저장하므로(cards.label_id) id는 생성 후 바뀌지 않는다. */
@Entity
@Table(name = "labels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Label {

    @Id
    @Column(length = 30)
    private String id;

    @Column(nullable = false, length = 20)
    private String name;

    /** "#rrggbb" 형식. */
    @Column(nullable = false, length = 7)
    private String color;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Label(String id, String name, String color, int position) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.position = position;
    }

    public void update(String name, String color) {
        this.name = name;
        this.color = color;
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
