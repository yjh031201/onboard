package com.kanban.backend.file;

import com.kanban.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_key", nullable = false, length = 500)
    private String fileKey;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ProjectFile(String fileName, String fileKey, long fileSize, String contentType, Long uploadedBy) {
        this.fileName = fileName;
        this.fileKey = fileKey;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.uploadedBy = uploadedBy;
    }

    /**
     * 업로더 본인이거나 관리자(OWNER/ADMIN)면 삭제 가능. isAdmin은 users.role(전역, 항상 MEMBER로
     * 고정됨)이 아니라 ProjectAccessService.isAdminInAnyProject(user) 결과를 서비스 레이어에서
     * 미리 계산해서 넘겨준다 — 엔티티가 리포지토리를 직접 들고 있지 않게 하려고.
     */
    public boolean isDeletableBy(User user, boolean isAdmin) {
        return uploadedBy.equals(user.getId()) || isAdmin;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
