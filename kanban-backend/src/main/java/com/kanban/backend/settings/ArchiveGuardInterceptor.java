package com.kanban.backend.settings;

import com.kanban.backend.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 프로젝트가 보관 중이면 쓰기 요청(POST/PUT/PATCH/DELETE)을 한곳에서 막는다 — 각 서비스가 따로 검사하지 않아도
 * 새로 추가되는 API까지 자동으로 읽기 전용이 된다. 조회(GET)는 그대로 된다.
 */
@Component
public class ArchiveGuardInterceptor implements HandlerInterceptor {

    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    /**
     * 보관 중에도 되는 쓰기 요청: 로그인/토큰 갱신, 내 계정·팀원 관리, 접속 상태,
     * 그리고 보관을 풀거나 프로젝트를 삭제하는 요청 (이게 막히면 보관에서 빠져나올 수 없다).
     */
    private static final List<String> ALLOWED_PREFIXES = List.of(
            "/api/auth",
            "/api/users",
            "/api/presence",
            "/api/settings/archive",
            "/api/settings/unarchive",
            "/api/settings/project"
    );

    private final ProjectSettingsService settingsService;

    public ArchiveGuardInterceptor(ProjectSettingsService settingsService) {
        this.settingsService = settingsService;
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
        String path = request.getServletPath();
        if (ALLOWED_PREFIXES.stream().anyMatch(path::startsWith)) {
            return true;
        }
        if (settingsService.isArchived()) {
            throw new ApiException(HttpStatus.LOCKED, "보관된 프로젝트는 읽기 전용입니다. 설정에서 보관을 해제해 주세요.");
        }
        return true;
    }
}
