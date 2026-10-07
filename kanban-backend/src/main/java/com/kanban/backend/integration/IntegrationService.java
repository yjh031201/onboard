package com.kanban.backend.integration;

import com.kanban.backend.common.ApiException;
import com.kanban.backend.integration.dto.IntegrationStatusResponse;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRole;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntegrationService {

    private final IntegrationRepository integrationRepository;

    public IntegrationService(IntegrationRepository integrationRepository) {
        this.integrationRepository = integrationRepository;
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

    public static void requireAdmin(User user) {
        if (user.getRole() == UserRole.MEMBER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "연동 설정은 관리자만 변경할 수 있습니다.");
        }
    }
}
