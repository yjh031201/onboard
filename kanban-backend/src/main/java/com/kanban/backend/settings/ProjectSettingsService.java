package com.kanban.backend.settings;

import com.kanban.backend.common.ApiException;
import com.kanban.backend.realtime.RealtimeChannels;
import com.kanban.backend.realtime.RealtimeEventPublisher;
import com.kanban.backend.settings.dto.ProjectSettingsResponse;
import com.kanban.backend.settings.dto.ProjectSettingsUpdateRequest;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectSettingsService {

    /** 설정을 한 번도 저장하지 않은 채 보관하거나 팀 이름을 바꿀 때 쓰는 이름 (project_name이 NOT NULL). */
    private static final String DEFAULT_PROJECT_NAME = "칸반보드 프로젝트";

    private final ProjectSettingsRepository repository;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public ProjectSettingsService(ProjectSettingsRepository repository, RealtimeEventPublisher realtimeEventPublisher) {
        this.repository = repository;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Transactional(readOnly = true)
    public ProjectSettingsResponse get() {
        return repository.findTopByOrderByIdAsc()
                .map(ProjectSettingsResponse::from)
                .orElseGet(ProjectSettingsResponse::empty);
    }

    @Transactional(readOnly = true)
    public boolean isArchived() {
        return repository.findTopByOrderByIdAsc().map(ProjectSettings::isArchived).orElse(false);
    }

    @Transactional
    public ProjectSettingsResponse update(ProjectSettingsUpdateRequest request, User currentUser) {
        requireAdmin(currentUser);

        ProjectSettings settings = repository.findTopByOrderByIdAsc().orElseGet(ProjectSettings::new);
        settings.update(request.projectName(), request.description(), currentUser.getId());

        return ProjectSettingsResponse.from(repository.save(settings));
    }

    /** 팀원 페이지의 팀 이름을 바꾼다 (OWNER/ADMIN). */
    @Transactional
    public ProjectSettingsResponse renameTeam(String teamName, User currentUser) {
        requireAdmin(currentUser);

        ProjectSettings settings = findOrCreateDefault(currentUser);
        settings.renameTeam(teamName.trim(), currentUser.getId());

        return saveAndBroadcast(settings);
    }

    /** 프로젝트를 읽기 전용으로 전환한다. 이미 보관 중이면 그대로 둔다. */
    @Transactional
    public ProjectSettingsResponse archive(User currentUser) {
        requireAdmin(currentUser);

        ProjectSettings settings = findOrCreateDefault(currentUser);
        if (!settings.isArchived()) {
            settings.archive(currentUser.getId());
        }

        return saveAndBroadcast(settings);
    }

    @Transactional
    public ProjectSettingsResponse unarchive(User currentUser) {
        requireAdmin(currentUser);

        ProjectSettings settings = repository.findTopByOrderByIdAsc().orElse(null);
        if (settings == null || !settings.isArchived()) {
            return get();
        }
        settings.unarchive(currentUser.getId());

        return saveAndBroadcast(settings);
    }

    private ProjectSettings findOrCreateDefault(User currentUser) {
        return repository.findTopByOrderByIdAsc().orElseGet(() -> {
            ProjectSettings created = new ProjectSettings();
            created.update(DEFAULT_PROJECT_NAME, "", currentUser.getId());
            return created;
        });
    }

    /** 다른 사람 화면의 "보관됨" 표시도 바로 바뀌도록 /topic/settings로 알린다. */
    private ProjectSettingsResponse saveAndBroadcast(ProjectSettings settings) {
        ProjectSettingsResponse response = ProjectSettingsResponse.from(repository.save(settings));
        realtimeEventPublisher.publish(RealtimeChannels.SETTINGS_EVENTS, response);
        return response;
    }

    private void requireAdmin(User user) {
        if (user.getRole() == UserRole.MEMBER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "프로젝트 설정은 관리자만 변경할 수 있습니다.");
        }
    }
}
