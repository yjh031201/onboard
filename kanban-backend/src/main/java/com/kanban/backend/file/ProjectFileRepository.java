package com.kanban.backend.file;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectFileRepository extends JpaRepository<ProjectFile, Long> {

    List<ProjectFile> findAllByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<ProjectFile> findAllByProjectId(Long projectId);

    /** 회원 탈퇴 시 — 본인이 올린 파일(모든 프로젝트). */
    List<ProjectFile> findAllByUploadedBy(Long uploadedBy);
}
