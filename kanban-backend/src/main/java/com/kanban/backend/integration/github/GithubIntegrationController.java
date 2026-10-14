package com.kanban.backend.integration.github;

import com.kanban.backend.integration.IntegrationCallbackSupport;
import com.kanban.backend.integration.IntegrationService;
import com.kanban.backend.integration.dto.ConnectUrlResponse;
import com.kanban.backend.integration.github.dto.GithubIssueResponse;
import com.kanban.backend.integration.github.dto.GithubRepoRequest;
import com.kanban.backend.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GithubIntegrationController {

    private static final String SLUG = "github";

    private final GithubIntegrationService githubIntegrationService;
    private final IntegrationService integrationService;
    private final IntegrationCallbackSupport callbackSupport;

    public GithubIntegrationController(
            GithubIntegrationService githubIntegrationService,
            IntegrationService integrationService,
            IntegrationCallbackSupport callbackSupport
    ) {
        this.githubIntegrationService = githubIntegrationService;
        this.integrationService = integrationService;
        this.callbackSupport = callbackSupport;
    }

    @GetMapping("/api/projects/{projectId}/integrations/github/connect")
    public ResponseEntity<ConnectUrlResponse> connect(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest request
    ) {
        integrationService.requireAdmin(projectId, currentUser);
        String authorizeUrl = githubIntegrationService.buildAuthorizeUrl(
                currentUser.getId(), projectId, callbackSupport.callbackUrl(request, SLUG));
        return ResponseEntity.ok(new ConnectUrlResponse(authorizeUrl));
    }

    /** GitHub OAuth App에 등록된 고정 콜백 주소 — 프로젝트는 state로 되찾는다. */
    @GetMapping("/api/integrations/github/callback")
    public void callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String redirectUri = callbackSupport.callbackUrl(request, SLUG);
        response.sendRedirect(callbackSupport.handle(SLUG, "GitHub", code, state, error,
                (authCode, connectState) -> githubIntegrationService.handleCallback(authCode, connectState, redirectUri)));
    }

    @PutMapping("/api/projects/{projectId}/integrations/github/repo")
    public ResponseEntity<Void> linkRepo(
            @PathVariable Long projectId,
            @Valid @RequestBody GithubRepoRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        githubIntegrationService.linkRepo(projectId, request.owner().trim(), request.repo().trim(), currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/projects/{projectId}/integrations/github/issues")
    public ResponseEntity<List<GithubIssueResponse>> issues(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(githubIntegrationService.listIssues(projectId, currentUser));
    }
}
