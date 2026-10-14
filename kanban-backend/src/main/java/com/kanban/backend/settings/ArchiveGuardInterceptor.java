package com.kanban.backend.settings;

import com.kanban.backend.common.ApiException;
import com.kanban.backend.project.Project;
import com.kanban.backend.project.ProjectRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 프로젝트가 보관 중이면 그 프로젝트 하위 쓰기 요청(POST/PUT/PATCH/DELETE)을 한곳에서 막는다 — 각 서비스가
 * 따로 검사하지 않아도 새로 추가되는 API까지 자동으로 읽기 전용이 된다. 조회(GET)는 그대로 된다.
 * 멀티 프로젝트 구조라 "보관"은 한 프로젝트에만 적용되고, "/api/projects/{id}/..." 경로가 아닌 요청
 * (로그인, 내 계정, 프로젝트 목록/생성 등)은 애초에 특정 프로젝트와 무관하므로 건드리지 않는다.
 */
@Component
public class ArchiveGuardInterceptor implements HandlerInterceptor {

    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    /** "/api/projects/{id}/..." 또는 "/api/projects/{id}" 형태에서 프로젝트 id를 뽑는다. */
    private static final Pattern PROJECT_SCOPED_PATH = Pattern.compile("^/api/projects/(\\d+)(/.*)?$");

    /** 보관 중에도 되는 하위 경로 — 보관을 풀 수 없게 되면 그 프로젝트는 영원히 못 빠져나오게 된다. */
    private static final List<String> ALLOWED_SUFFIXES = List.of("/archive", "/unarchive");

    private final ProjectRepository projectRepository;

    public ArchiveGuardInterceptor(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {
        if (READ_METHODS.contains(request.getMethod())) {
            return true;
        }

        Matcher matcher = PROJECT_SCOPED_PATH.matcher(request.getServletPath());
        if (!matcher.matches()) {
            // 이 프로젝트 하위 경로가 아님 — 로그인, 내 계정, 프로젝트 생성 등은 보관과 무관하게 그대로 둔다.
            return true;
        }

        String suffix = matcher.group(2);
        boolean isProjectDeleteItself = (suffix == null || suffix.isEmpty()) && "DELETE".equals(request.getMethod());
        if (isProjectDeleteItself || (suffix != null && ALLOWED_SUFFIXES.contains(suffix))) {
            return true;
        }

        Long projectId = Long.valueOf(matcher.group(1));
        boolean archived = projectRepository.findById(projectId).map(Project::isArchived).orElse(false);
        if (archived) {
            throw new ApiException(HttpStatus.LOCKED, "보관된 프로젝트는 읽기 전용입니다. 설정에서 보관을 해제해 주세요.");
        }
        return true;
    }
}
