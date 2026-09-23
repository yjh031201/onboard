package com.kanban.backend.settings;

import com.kanban.backend.settings.dto.ProjectSettingsResponse;
import com.kanban.backend.settings.dto.ProjectSettingsUpdateRequest;
import com.kanban.backend.user.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class ProjectSettingsController {

    private final ProjectSettingsService service;

    public ProjectSettingsController(ProjectSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ProjectSettingsResponse> get() {
        return ResponseEntity.ok(service.get());
    }

    @PutMapping
    public ResponseEntity<ProjectSettingsResponse> update(
            @Valid @RequestBody ProjectSettingsUpdateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(service.update(request, currentUser));
    }
}
