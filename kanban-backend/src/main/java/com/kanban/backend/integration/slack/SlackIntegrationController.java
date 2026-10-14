package com.kanban.backend.integration.slack;

import com.kanban.backend.integration.IntegrationCallbackSupport;
import com.kanban.backend.integration.IntegrationService;
import com.kanban.backend.integration.dto.ConnectUrlResponse;
import com.kanban.backend.integration.slack.dto.SlackWebhookRequest;
import com.kanban.backend.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SlackIntegrationController {

    private static final String SLUG = "slack";

    private final SlackIntegrationService slackIntegrationService;
    private final IntegrationService integrationService;
    private final IntegrationCallbackSupport callbackSupport;

    public SlackIntegrationController(
            SlackIntegrationService slackIntegrationService,
            IntegrationService integrationService,
            IntegrationCallbackSupport callbackSupport
    ) {
        this.slackIntegrationService = slackIntegrationService;
        this.integrationService = integrationService;
        this.callbackSupport = callbackSupport;
    }

    @GetMapping("/api/projects/{projectId}/integrations/slack/connect")
    public ResponseEntity<ConnectUrlResponse> connect(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest request
    ) {
        integrationService.requireAdmin(projectId, currentUser);
        String authorizeUrl = slackIntegrationService.buildAuthorizeUrl(
                currentUser.getId(), projectId, callbackSupport.callbackUrl(request, SLUG));
        return ResponseEntity.ok(new ConnectUrlResponse(authorizeUrl));
    }

    /** Slack 앱에 등록된 고정 콜백 주소 — 프로젝트는 state로 되찾는다. */
    @GetMapping("/api/integrations/slack/callback")
    public void callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String redirectUri = callbackSupport.callbackUrl(request, SLUG);
        response.sendRedirect(callbackSupport.handle(SLUG, "Slack", code, state, error,
                (authCode, connectState) -> slackIntegrationService.handleCallback(authCode, connectState, redirectUri)));
    }

    /** OAuth 없이 Slack Incoming Webhook 주소를 붙여 넣어 이 프로젝트에 연결한다. */
    @PostMapping("/api/projects/{projectId}/integrations/slack/webhook")
    public ResponseEntity<Void> connectWithWebhook(
            @PathVariable Long projectId,
            @Valid @RequestBody SlackWebhookRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        integrationService.requireAdmin(projectId, currentUser);
        slackIntegrationService.connectWithWebhook(projectId, currentUser.getId(), request.webhookUrl());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/projects/{projectId}/integrations/slack/test")
    public ResponseEntity<Void> test(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User currentUser
    ) {
        integrationService.requireAdmin(projectId, currentUser);
        slackIntegrationService.sendTestMessage(projectId);
        return ResponseEntity.noContent().build();
    }
}
