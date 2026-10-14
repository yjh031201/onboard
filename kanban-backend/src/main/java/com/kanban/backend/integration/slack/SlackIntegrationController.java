package com.kanban.backend.integration.slack;

import com.kanban.backend.integration.IntegrationService;
import com.kanban.backend.integration.dto.ConnectUrlResponse;
import com.kanban.backend.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/integrations/slack")
public class SlackIntegrationController {

    private static final Logger log = LoggerFactory.getLogger(SlackIntegrationController.class);

    private final SlackIntegrationService slackIntegrationService;
    private final IntegrationService integrationService;
    private final String frontendBaseUrl;

    public SlackIntegrationController(
            SlackIntegrationService slackIntegrationService,
            IntegrationService integrationService,
            @Value("${app.frontend-base-url}") String frontendBaseUrl
    ) {
        this.slackIntegrationService = slackIntegrationService;
        this.integrationService = integrationService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @GetMapping("/connect")
    public ResponseEntity<ConnectUrlResponse> connect(
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest request
    ) {
        integrationService.requireAdmin(currentUser);
        String authorizeUrl = slackIntegrationService.buildAuthorizeUrl(currentUser.getId(), callbackUrl(request));
        return ResponseEntity.ok(new ConnectUrlResponse(authorizeUrl));
    }

    @GetMapping("/callback")
    public void callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        if (error != null || code == null || state == null) {
            response.sendRedirect(redirectTarget("error", "Slack 연동이 취소되었어요."));
            return;
        }
        try {
            slackIntegrationService.handleCallback(code, state, callbackUrl(request));
            response.sendRedirect(redirectTarget("connected", null));
        } catch (Exception e) {
            log.warn("Slack 연동 콜백 처리 실패", e);
            response.sendRedirect(redirectTarget("error", "Slack 연동에 실패했어요."));
        }
    }

    @PostMapping("/test")
    public ResponseEntity<Void> test(@AuthenticationPrincipal User currentUser) {
        integrationService.requireAdmin(currentUser);
        slackIntegrationService.sendTestMessage();
        return ResponseEntity.noContent().build();
    }

    private String callbackUrl(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromContextPath(request)
                .path("/api/integrations/slack/callback")
                .toUriString();
    }

    private String redirectTarget(String status, String message) {
        String url = frontendBaseUrl + "/members?integration=slack&status=" + status;
        if (message != null) {
            url += "&message=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
        }
        return url;
    }
}
