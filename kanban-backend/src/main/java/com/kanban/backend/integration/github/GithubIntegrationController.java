package com.kanban.backend.integration.github;

import com.kanban.backend.integration.IntegrationService;
import com.kanban.backend.integration.dto.ConnectUrlResponse;
import com.kanban.backend.integration.github.dto.GithubIssueResponse;
import com.kanban.backend.integration.github.dto.GithubRepoRequest;
import com.kanban.backend.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/integrations/github")
public class GithubIntegrationController {

    private final GithubIntegrationService githubIntegrationService;
    private final String frontendBaseUrl;

    public GithubIntegrationController(
            GithubIntegrationService githubIntegrationService,
            @Value("${app.frontend-base-url}") String frontendBaseUrl
    ) {
        this.githubIntegrationService = githubIntegrationService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @GetMapping("/connect")
    public ResponseEntity<ConnectUrlResponse> connect(
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest request
    ) {
        IntegrationService.requireAdmin(currentUser);
        String authorizeUrl = githubIntegrationService.buildAuthorizeUrl(currentUser.getId(), callbackUrl(request));
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
            response.sendRedirect(redirectTarget("error", "GitHub 연동이 취소되었어요."));
            return;
        }
        try {
            githubIntegrationService.handleCallback(code, state, callbackUrl(request));
            response.sendRedirect(redirectTarget("connected", null));
        } catch (Exception e) {
            response.sendRedirect(redirectTarget("error", "GitHub 연동에 실패했어요."));
        }
    }

    @PutMapping("/repo")
    public ResponseEntity<Void> linkRepo(
            @Valid @RequestBody GithubRepoRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        githubIntegrationService.linkRepo(request.owner(), request.repo(), currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/issues")
    public ResponseEntity<List<GithubIssueResponse>> issues() {
        return ResponseEntity.ok(githubIntegrationService.listIssues());
    }

    private String callbackUrl(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromContextPath(request)
                .path("/api/integrations/github/callback")
                .toUriString();
    }

    private String redirectTarget(String status, String message) {
        String url = frontendBaseUrl + "/members?integration=github&status=" + status;
        if (message != null) {
            url += "&message=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
        }
        return url;
    }
}
