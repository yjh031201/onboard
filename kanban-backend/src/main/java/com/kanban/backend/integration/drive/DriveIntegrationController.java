package com.kanban.backend.integration.drive;

import com.kanban.backend.integration.IntegrationCallbackSupport;
import com.kanban.backend.integration.IntegrationService;
import com.kanban.backend.integration.dto.ConnectUrlResponse;
import com.kanban.backend.integration.drive.dto.DriveFileResponse;
import com.kanban.backend.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DriveIntegrationController {

    private static final String SLUG = "drive";

    private final DriveIntegrationService driveIntegrationService;
    private final IntegrationService integrationService;
    private final IntegrationCallbackSupport callbackSupport;

    public DriveIntegrationController(
            DriveIntegrationService driveIntegrationService,
            IntegrationService integrationService,
            IntegrationCallbackSupport callbackSupport
    ) {
        this.driveIntegrationService = driveIntegrationService;
        this.integrationService = integrationService;
        this.callbackSupport = callbackSupport;
    }

    @GetMapping("/api/projects/{projectId}/integrations/drive/connect")
    public ResponseEntity<ConnectUrlResponse> connect(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest request
    ) {
        integrationService.requireAdmin(projectId, currentUser);
        String authorizeUrl = driveIntegrationService.buildAuthorizeUrl(
                currentUser.getId(), projectId, callbackSupport.callbackUrl(request, SLUG));
        return ResponseEntity.ok(new ConnectUrlResponse(authorizeUrl));
    }

    /** Google Cloud Console에 등록된 고정 콜백 주소 — 프로젝트는 state로 되찾는다. */
    @GetMapping("/api/integrations/drive/callback")
    public void callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String redirectUri = callbackSupport.callbackUrl(request, SLUG);
        response.sendRedirect(callbackSupport.handle(SLUG, "Google Drive", code, state, error,
                (authCode, connectState) -> driveIntegrationService.handleCallback(authCode, connectState, redirectUri)));
    }

    @GetMapping("/api/projects/{projectId}/integrations/drive/files")
    public ResponseEntity<List<DriveFileResponse>> files(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(driveIntegrationService.listFiles(projectId, currentUser));
    }
}
