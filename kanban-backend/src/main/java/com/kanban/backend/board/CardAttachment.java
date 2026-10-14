package com.kanban.backend.board;

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
 * 카드에 붙인 GitHub 이슈 / Google Drive 파일 링크. 파일 자체를 복사하지 않고 제목·주소만 저장한다.
 * 카드 삭제 시에는 DB FK(ON DELETE CASCADE)로 같이 지워진다 (V20).
 */
@Entity
@Table(name = "card_attachments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardAttachment {

    public static final int MAX_TITLE_LENGTH = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttachmentKind kind;

    /** GitHub 이슈는 "owner/repo#번호", Drive 파일은 파일 id. */
    @Column(name = "external_id", nullable = false, length = 200)
    private String externalId;

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @Column(nullable = false, length = 1000)
    private String url;

    /** Drive 파일의 MIME 타입 (아이콘 표시용). GitHub 이슈는 null. */
    @Column(name = "mime_type", length = 150)
    private String mimeType;

    @Column(name = "attached_by_id", nullable = false)
    private Long attachedById;

    @Column(name = "attached_by_name", nullable = false, length = 100)
    private String attachedByName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CardAttachment(
            Long cardId,
            Long projectId,
            AttachmentKind kind,
            String externalId,
            String title,
            String url,
            String mimeType,
            Long attachedById,
            String attachedByName
    ) {
        this.cardId = cardId;
        this.projectId = projectId;
        this.kind = kind;
        this.externalId = externalId;
        this.title = title.length() > MAX_TITLE_LENGTH ? title.substring(0, MAX_TITLE_LENGTH) : title;
        this.url = url;
        this.mimeType = mimeType;
        this.attachedById = attachedById;
        this.attachedByName = attachedByName;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
