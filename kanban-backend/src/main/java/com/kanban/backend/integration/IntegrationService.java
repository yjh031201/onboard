package com.kanban.backend.integration;

import com.kanban.backend.common.ApiException;
import com.kanban.backend.integration.dto.IntegrationStatusResponse;
import com.kanban.backend.project.ProjectAccessService;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRole;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 프로젝트별 연동 공통 — 목록/해제와 권한 체크. */
@Service
public class IntegrationService {

    private final IntegrationRepository integrationRepository;
    private final ProjectAccessService projectAccessService;

    public IntegrationService(
            IntegrationRepository integrationRepository,
            ProjectAccessService projectAccessService
    ) {
        this.integrationRepository = integrationRepository;
        this.projectAccessService = projectAccessService;
    }

    /** 이 프로젝트의 연동 상태 — 멤버 누구나 볼 수 있다. */
    @Transactional(readOnly = true)
    public List<IntegrationStatusResponse> list(Long projectId, User currentUser) {
        projectAccessService.requireMember(projectId, currentUser);
        return Arrays.stream(IntegrationProvider.values())
                .map(provider -> integrationRepository.findByProjectIdAndProvider(projectId, provider)
                        .map(IntegrationStatusResponse::from)
                        .orElseGet(() -> IntegrationStatusResponse.disconnected(provider)))
                .toList();
    }

    @Transactional
    public void disconnect(Long projectId, IntegrationProvider provider, User currentUser) {
        requireAdmin(projectId, currentUser);
        integrationRepository.deleteByProjectIdAndProvider(projectId, provider);
    }

    /** 연동 연결/해제/설정 변경은 이 프로젝트의 OWNER/ADMIN만. */
    public void requireAdmin(Long projectId, User user) {
        projectAccessService.requireAdmin(projectId, user);
    }

    /** 연동된 데이터(이슈·파일 등) 조회는 이 프로젝트의 멤버 누구나. */
    public void requireMember(Long projectId, User user) {
        projectAccessService.requireMember(projectId, user);
    }

    /**
     * OAuth 콜백 시점 재확인 — 연결을 시작하고 외부 동의 화면에 가 있는 동안 관리자에서 빠졌을 수 있다.
     * 콜백 요청에는 로그인 정보가 없어서 state에 담아 둔 사용자 id로 확인한다.
     */
    public void requireAdminForCallback(ConnectState state) {
        UserRole role = projectAccessService.myRoleOrNull(state.projectId(), state.userId());
        if (role != UserRole.OWNER && role != UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "이 프로젝트의 연동 설정은 관리자만 변경할 수 있습니다.");
        }
    }

    public Integration requireConnected(Long projectId, IntegrationProvider provider, String notConnectedMessage) {
        return integrationRepository.findByProjectIdAndProvider(projectId, provider)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, notConnectedMessage));
    }
}
