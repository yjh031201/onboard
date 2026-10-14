package com.kanban.backend.file;

import com.kanban.backend.common.ApiException;
import com.kanban.backend.file.dto.ProjectFileResponse;
import com.kanban.backend.project.ProjectAccessService;
import com.kanban.backend.project.ProjectMember;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRepository;
import com.kanban.backend.user.UserRole;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** 프로젝트별 파일 — 그 프로젝트의 멤버만 보고 올리고 내려받을 수 있다. */
@Service
public class ProjectFileService {

    private static final Logger log = LoggerFactory.getLogger(ProjectFileService.class);

    private final ProjectFileRepository repository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final ProjectAccessService projectAccessService;

    public ProjectFileService(
            ProjectFileRepository repository,
            FileStorageService fileStorageService,
            UserRepository userRepository,
            ProjectAccessService projectAccessService
    ) {
        this.repository = repository;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
        this.projectAccessService = projectAccessService;
    }

    @Transactional(readOnly = true)
    public List<ProjectFileResponse> list(Long projectId, User currentUser) {
        projectAccessService.requireMember(projectId, currentUser);
        List<ProjectFile> files = repository.findAllByProjectIdOrderByCreatedAtDesc(projectId);

        // 업로더 이름은 한 번에 조회해서 N+1 쿼리를 피한다.
        Set<Long> uploaderIds = files.stream().map(ProjectFile::getUploadedBy).collect(Collectors.toSet());
        Map<Long, String> names = userRepository.findAllById(uploaderIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        return files.stream()
                .map(file -> ProjectFileResponse.from(file, names.get(file.getUploadedBy())))
                .toList();
    }

    @Transactional
    public ProjectFileResponse upload(Long projectId, MultipartFile file, User currentUser) {
        projectAccessService.requireMember(projectId, currentUser);
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "빈 파일은 업로드할 수 없습니다.");
        }

        StoredFile stored = fileStorageService.store(file);
        String originalName = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "file"));

        ProjectFile projectFile = new ProjectFile(
                projectId, originalName, stored.fileKey(), stored.size(), stored.contentType(), currentUser.getId());

        return ProjectFileResponse.from(repository.save(projectFile), currentUser.getName());
    }

    /** 다운로드할 파일 — 이 프로젝트의 멤버이고 파일도 이 프로젝트 것이어야 한다. */
    @Transactional(readOnly = true)
    public ProjectFile getForDownload(Long projectId, Long id, User currentUser) {
        projectAccessService.requireMember(projectId, currentUser);
        return getOrThrow(projectId, id);
    }

    @Transactional
    public void delete(Long projectId, Long id, User currentUser) {
        ProjectMember member = projectAccessService.requireMember(projectId, currentUser);
        ProjectFile file = getOrThrow(projectId, id);
        boolean isAdmin = member.getRole() != UserRole.MEMBER;
        if (!file.isDeletableBy(currentUser, isAdmin)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인이 올린 파일만 삭제할 수 있습니다.");
        }
        fileStorageService.delete(file.getFileKey());
        repository.delete(file);
    }

    /**
     * 프로젝트를 삭제할 때 — 그 프로젝트의 파일 기록과 실제 파일을 모두 지운다. 권한 체크는 호출하는 쪽(ProjectService)이 한다.
     * 디스크 파일 삭제가 하나 실패해도 프로젝트 삭제 자체는 막지 않는다 (남는 건 고아 파일뿐이라 DB 정합성에는 문제가 없다).
     */
    @Transactional
    public void deleteAllInProject(Long projectId) {
        List<ProjectFile> files = repository.findAllByProjectId(projectId);
        repository.deleteAll(files);
        for (ProjectFile file : files) {
            try {
                fileStorageService.delete(file.getFileKey());
            } catch (Exception e) {
                log.warn("프로젝트 {} 삭제 중 파일 {} 삭제 실패", projectId, file.getFileKey(), e);
            }
        }
    }

    /** 다른 프로젝트의 파일 id를 넣어도 "없음"으로 처리한다 — 존재 여부도 알려주지 않는다. */
    private ProjectFile getOrThrow(Long projectId, Long id) {
        return repository.findById(id)
                .filter(file -> file.getProjectId().equals(projectId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."));
    }
}
