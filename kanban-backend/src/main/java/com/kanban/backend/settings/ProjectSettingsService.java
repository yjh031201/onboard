package com.kanban.backend.settings;

import com.kanban.backend.common.ApiException;
import com.kanban.backend.settings.dto.ProjectSettingsResponse;
import com.kanban.backend.settings.dto.ProjectSettingsUpdateRequest;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectSettingsService {

    private final ProjectSettingsRepository repository;

    public ProjectSettingsService(ProjectSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ProjectSettingsResponse get() {
        return repository.findTopByOrderByIdAsc()
                .map(ProjectSettingsResponse::from)
                .orElseGet(ProjectSettingsResponse::empty);
    }

    @Transactional
    public ProjectSettingsResponse update(ProjectSettingsUpdateRequest request, User currentUser) {
        requireAdmin(currentUser);

        ProjectSettings settings = repository.findTopByOrderByIdAsc().orElseGet(ProjectSettings::new);
        settings.update(request.projectName(), request.description(), currentUser.getId());

        return ProjectSettingsResponse.from(repository.save(settings));
    }

    private void requireAdmin(User user) {
        if (user.getRole() == UserRole.MEMBER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "프로젝트 설정은 관리자만 변경할 수 있습니다.");
        }
    }
}
