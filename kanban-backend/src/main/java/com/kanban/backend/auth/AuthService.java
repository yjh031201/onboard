package com.kanban.backend.auth;

import com.kanban.backend.auth.dto.AuthResponse;
import com.kanban.backend.auth.dto.FindIdRequest;
import com.kanban.backend.auth.dto.FindIdResponse;
import com.kanban.backend.auth.dto.LoginRequest;
import com.kanban.backend.auth.dto.SignupRequest;
import com.kanban.backend.auth.dto.UserResponse;
import com.kanban.backend.auth.oauth2.OAuthCodeExchangeService;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.config.JwtTokenProvider;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final OAuthCodeExchangeService oAuthCodeExchangeService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenStore refreshTokenStore,
            OAuthCodeExchangeService oAuthCodeExchangeService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenStore = refreshTokenStore;
        this.oAuthCodeExchangeService = oAuthCodeExchangeService;
    }

    @Transactional
    public AuthResult signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 등록된 휴대폰 번호입니다.");
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.phone()
        );
        userRepository.save(user);

        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public AuthResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return issueToken(user);
    }

    /** Access token이 만료된 클라이언트가 refresh token으로 재로그인 없이 새 토큰을 받는다. */
    @Transactional(readOnly = true)
    public AuthResult refresh(String refreshToken) {
        Long userId = jwtTokenProvider.parseRefreshUserId(refreshToken)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."));

        if (!refreshTokenStore.isValid(userId, refreshToken)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "만료되었거나 무효화된 리프레시 토큰입니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "존재하지 않는 사용자입니다."));

        // 쓴 토큰은 버리고 새 토큰으로 바꾼다 — 이 기기의 세션만 교체되고 다른 기기의 로그인은 그대로.
        refreshTokenStore.revoke(userId, refreshToken);
        return issueToken(user);
    }

    /** OAuth2AuthenticationSuccessHandler가 발급한 1회용 코드를 우리 JWT로 교환한다. */
    @Transactional
    public AuthResult exchangeOAuthCode(String code) {
        Long userId = oAuthCodeExchangeService.redeem(code);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "존재하지 않는 사용자입니다."));

        return issueToken(user);
    }

    /** 로그아웃 — 이 기기(쿠키)의 refresh token만 Redis에서 지운다. 다른 기기의 로그인은 그대로. */
    public void logout(String refreshToken) {
        if (refreshToken == null) {
            return;
        }
        jwtTokenProvider.parseRefreshUserId(refreshToken)
                .ifPresent(userId -> refreshTokenStore.revoke(userId, refreshToken));
    }

    private AuthResult issueToken(User user) {
        String accessToken = jwtTokenProvider.createToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());
        refreshTokenStore.save(user.getId(), refreshToken, jwtTokenProvider.getRefreshExpirationMs());

        AuthResponse body = AuthResponse.of(accessToken, jwtTokenProvider.getExpirationMs(), UserResponse.from(user));
        return new AuthResult(body, refreshToken);
    }

    /** 이름+휴대폰번호로 본인 확인 후, 가입된 이메일(아이디)을 마스킹해서 돌려줌. */
    @Transactional(readOnly = true)
    public FindIdResponse findId(FindIdRequest request) {
        User user = userRepository.findByNameAndPhone(request.name(), request.phone())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "일치하는 회원 정보를 찾을 수 없습니다."));

        return new FindIdResponse(maskEmail(user.getEmail()));
    }

    // 비밀번호 재설정은 이메일 인증 코드 방식으로 PasswordResetService가 맡는다 — 예전의 "이름+이메일만 맞으면
    // 바로 변경"은 남의 계정을 가져갈 수 있는 구멍이라 없앴다.

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);

        if (local.length() <= 2) {
            return local.charAt(0) + "*".repeat(Math.max(local.length() - 1, 1)) + domain;
        }
        String visible = local.substring(0, 2);
        String masked = "*".repeat(local.length() - 2);
        return visible + masked + domain;
    }
}
