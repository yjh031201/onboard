package com.kanban.backend.integration.dto;

import com.kanban.backend.integration.Integration;
import com.kanban.backend.integration.IntegrationProvider;
import java.time.LocalDateTime;

public record IntegrationStatusResponse(
        IntegrationProvider provider,
        boolean connected,
        String accountLabel,
        String detail,
        LocalDateTime connectedAt
) {
    public static IntegrationStatusResponse disconnected(IntegrationProvider provider) {
        return new IntegrationStatusResponse(provider, false, null, null, null);
    }

    public static IntegrationStatusResponse from(Integration integration) {
        return new IntegrationStatusResponse(
                integration.getProvider(),
                true,
                integration.getAccountLabel(),
                detailOf(integration),
                integration.getConnectedAt()
        );
    }

    private static String detailOf(Integration integration) {
        return switch (integration.getProvider()) {
            case GITHUB -> integration.getGithubOwner() != null
                    ? integration.getGithubOwner() + "/" + integration.getGithubRepo()
                    : null;
            case SLACK -> integration.getSlackChannel();
            case GOOGLE_DRIVE -> null;
        };
    }
}
