package com.kanban.backend.auth;

import com.kanban.backend.auth.dto.AuthResponse;
import com.kanban.backend.auth.dto.FindIdRequest;
import com.kanban.backend.auth.dto.FindIdResponse;
import com.kanban.backend.auth.dto.LoginRequest;
import com.kanban.backend.auth.dto.OAuthExchangeRequest;
import com.kanban.backend.auth.dto.PasswordResetCodeRequest;
import com.kanban.backend.auth.dto.PasswordResetConfirmRequest;
import com.kanban.backend.auth.dto.SignupRequest;
import com.kanban.backend.auth.dto.UserResponse;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.user.User;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    // 리프레시가 필요한 엔드포인트에만 쿠키가 실리게 범위를 좁혀둠.
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final long refreshExpirationMs;
    private final boolean cookieSecure;

    public AuthController(
            AuthService authService,
            PasswordResetService passwordResetService,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs,
            @Value("${app.cookie-secure}") boolean cookieSecure
    ) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.refreshExpirationMs = refreshExpirationMs;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request, HttpServletResponse response) {
        AuthResult result = authService.signup(request);
        setRefreshCookie(response, result.refreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(result.body());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResult result = authService.login(request);
        setRefreshCookie(response, result.refreshToken());
        return ResponseEntity.ok(result.body());
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

    /** 비밀번호 재설정 1단계 — 가입 여부와 상관없이 같은 응답(204)을 준다. */
    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetCodeRequest request) {
        passwordResetService.requestCode(request.email());
        return ResponseEntity.noContent().build();
    }

    /** 비밀번호 재설정 2단계 — 메일로 받은 코드가 맞으면 새 비밀번호로 바꾸고 다른 기기의 로그인을 끊는다. */
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirm(request.email(), request.code(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 없습니다.");
        }

        AuthResult result = authService.refresh(refreshToken);
        setRefreshCookie(response, result.refreshToken());
        return ResponseEntity.ok(result.body());
    }

    /** 구글/네이버 로그인 성공 후 발급된 1회용 코드를 실제 JWT로 교환한다. */
    @PostMapping("/oauth/exchange")
    public ResponseEntity<AuthResponse> exchangeOAuthCode(
            @Valid @RequestBody OAuthExchangeRequest request,
            HttpServletResponse response
    ) {
        AuthResult result = authService.exchangeOAuthCode(request.code());
        setRefreshCookie(response, result.refreshToken());
        return ResponseEntity.ok(result.body());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        // access token이 이미 만료된 상태에서도 로그아웃은 돼야 하므로, 인증 없이 refresh 쿠키로 처리한다.
        authService.logout(refreshToken);
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(refreshToken, refreshExpirationMs / 1000).toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", 0).toString());
    }

    private ResponseCookie buildCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAgeSeconds)
                .build();
    }
}
