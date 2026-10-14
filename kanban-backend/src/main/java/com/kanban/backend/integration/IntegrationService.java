package com.kanban.backend.integration;

import com.kanban.backend.common.ApiException;
import com.kanban.backend.integration.dto.IntegrationStatusResponse;
import com.kanban.backend.project.ProjectAccessService;
import com.kanban.backend.user.User;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public List<IntegrationStatusResponse> list() {
        return Arrays.stream(IntegrationProvider.values())
                .map(provider -> integrationRepository.findByProvider(provider)
                        .map(IntegrationStatusResponse::from)
                        .orElseGet(() -> IntegrationStatusResponse.disconnected(provider)))
                .toList();
    }

    @Transactional
    public void disconnect(IntegrationProvider provider, User currentUser) {
        requireAdmin(currentUser);
        integrationRepository.deleteByProvider(provider);
    }

    /**
     * 연동 연결/해제는 users.role(전역, 멀티 프로젝트 전환 후로는 항상 MEMBER로 고정됨)이 아니라
     * 이 사용자가 ACCEPTED 상태로 속한 프로젝트들 중 OWNER/ADMIN이 하나라도 있는지로 판단한다 —
     * 연동 자체는 아직 프로젝트별로 나뉘지 않은 단일 워크스페이스 기능이라 "어느 프로젝트의 관리자"
     * 인지는 구분하지 않는다.
     */
    public void requireAdmin(User user) {
        if (!projectAccessService.isAdminInAnyProject(user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "연동 설정은 관리자만 변경할 수 있습니다.");
        }
    }
}
