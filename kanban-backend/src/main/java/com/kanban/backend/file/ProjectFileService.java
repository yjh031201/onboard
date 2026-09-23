package com.kanban.backend.file;

import com.kanban.backend.common.ApiException;
import com.kanban.backend.file.dto.ProjectFileResponse;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProjectFileService {

    private final ProjectFileRepository repository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    public ProjectFileService(
            ProjectFileRepository repository,
            FileStorageService fileStorageService,
            UserRepository userRepository
    ) {
        this.repository = repository;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectFileResponse> list() {
        List<ProjectFile> files = repository.findAllByOrderByCreatedAtDesc();

        // 업로더 이름은 한 번에 조회해서 N+1 쿼리를 피한다.
        Set<Long> uploaderIds = files.stream().map(ProjectFile::getUploadedBy).collect(Collectors.toSet());
        Map<Long, String> names = userRepository.findAllById(uploaderIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        return files.stream()
                .map(file -> ProjectFileResponse.from(file, names.get(file.getUploadedBy())))
                .toList();
    }

    @Transactional
    public ProjectFileResponse upload(MultipartFile file, User currentUser) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "빈 파일은 업로드할 수 없습니다.");
        }

        StoredFile stored = fileStorageService.store(file);
        String originalName = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "file"));

        ProjectFile projectFile = new ProjectFile(
                originalName, stored.fileKey(), stored.size(), stored.contentType(), currentUser.getId());

        return ProjectFileResponse.from(repository.save(projectFile), currentUser.getName());
    }

    @Transactional(readOnly = true)
    public ProjectFile getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."));
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        ProjectFile file = getOrThrow(id);
        if (!file.isDeletableBy(currentUser)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인이 올린 파일만 삭제할 수 있습니다.");
        }
        fileStorageService.delete(file.getFileKey());
        repository.delete(file);
    }
}
