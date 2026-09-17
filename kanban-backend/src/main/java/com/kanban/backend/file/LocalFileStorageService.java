package com.kanban.backend.file;

import com.kanban.backend.common.ApiException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LocalFileStorageService implements FileStorageService {

    private final Path rootDir;

    public LocalFileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.rootDir = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            throw new IllegalStateException("업로드 디렉터리를 생성할 수 없습니다: " + rootDir, e);
        }
    }

    @Override
    public StoredFile store(MultipartFile file) {
        String originalName = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "file"));
        String fileKey = UUID.randomUUID() + "_" + originalName;
        Path target = resolveWithinRoot(fileKey);

        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다.");
        }

        return new StoredFile(fileKey, file.getSize(), file.getContentType());
    }

    @Override
    public Resource load(String fileKey) {
        Path path = resolveWithinRoot(fileKey);
        Resource resource = new FileSystemResource(path);
        if (!resource.exists()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다.");
        }
        return resource;
    }

    @Override
    public void delete(String fileKey) {
        try {
            Files.deleteIfExists(resolveWithinRoot(fileKey));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다.");
        }
    }

    /** fileKey에 "../" 같은 경로 조작이 섞여도 rootDir 밖으로 못 나가게 막는다. */
    private Path resolveWithinRoot(String fileKey) {
        Path resolved = rootDir.resolve(fileKey).normalize();
        if (!resolved.startsWith(rootDir)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "잘못된 파일 경로입니다.");
        }
        return resolved;
    }
}
