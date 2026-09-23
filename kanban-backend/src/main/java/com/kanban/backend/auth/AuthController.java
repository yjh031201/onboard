package com.kanban.backend.auth;

import com.kanban.backend.auth.dto.AuthResponse;
import com.kanban.backend.auth.dto.FindIdRequest;
import com.kanban.backend.auth.dto.FindIdResponse;
import com.kanban.backend.auth.dto.LoginRequest;
import com.kanban.backend.auth.dto.ResetPasswordRequest;
import com.kanban.backend.auth.dto.SignupRequest;
import com.kanban.backend.auth.dto.UserResponse;
import com.kanban.backend.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** Returns the currently authenticated user — used by the frontend to verify a stored token. */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PostMapping("/find-id")
    public ResponseEntity<FindIdResponse> findId(@Valid @RequestBody FindIdRequest request) {
        return ResponseEntity.ok(authService.findId(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}
