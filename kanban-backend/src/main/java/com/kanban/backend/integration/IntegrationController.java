package com.kanban.backend.integration;

import com.kanban.backend.integration.dto.IntegrationStatusResponse;
import com.kanban.backend.user.User;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integrations")
public class IntegrationController {

    private final IntegrationService integrationService;

    public IntegrationController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @GetMapping
    public ResponseEntity<List<IntegrationStatusResponse>> list() {
        return ResponseEntity.ok(integrationService.list());
    }

    @DeleteMapping("/{provider}")
    public ResponseEntity<Void> disconnect(
            @PathVariable IntegrationProvider provider,
            @AuthenticationPrincipal User currentUser
    ) {
        integrationService.disconnect(provider, currentUser);
        return ResponseEntity.noContent().build();
    }
}
