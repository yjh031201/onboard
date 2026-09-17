package com.kanban.backend.auth;

import com.kanban.backend.auth.dto.AuthResponse;
import com.kanban.backend.auth.dto.LoginRequest;
import com.kanban.backend.auth.dto.SignupRequest;
import com.kanban.backend.auth.dto.UserResponse;
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

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenStore refreshTokenStore
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenStore = refreshTokenStore;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
        }

        User user = new User(request.email(), passwordEncoder.encode(request.password()), request.name());
        userRepository.save(user);

        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return issueToken(user);
    }

    /** Access token이 만료된 클라이언트가 refresh token으로 재로그인 없이 새 토큰을 받는다. */
    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        Long userId = jwtTokenProvider.parseRefreshUserId(refreshToken)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."));

        if (!refreshTokenStore.isValid(userId, refreshToken)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "만료되었거나 무효화된 리프레시 토큰입니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "존재하지 않는 사용자입니다."));

        return issueToken(user);
    }

    /** 로그아웃 시 Redis에 저장된 refresh token을 지워서 즉시 무효화한다. */
    public void logout(Long userId) {
        refreshTokenStore.invalidate(userId);
    }

    private AuthResponse issueToken(User user) {
        String accessToken = jwtTokenProvider.createToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());
        refreshTokenStore.save(user.getId(), refreshToken, jwtTokenProvider.getRefreshExpirationMs());
        return AuthResponse.of(accessToken, refreshToken, jwtTokenProvider.getExpirationMs(), UserResponse.from(user));
    }
}
