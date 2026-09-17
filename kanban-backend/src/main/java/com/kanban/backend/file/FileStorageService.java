package com.kanban.backend.file;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 로컬 디스크/S3 등 실제 저장소를 감추는 경계.
 * 나중에 S3로 옮길 때는 S3FileStorageService를 추가하고 빈 등록만 바꾸면 됨.
 */
public interface FileStorageService {

    StoredFile store(MultipartFile file);

    Resource load(String fileKey);

    void delete(String fileKey);
}
