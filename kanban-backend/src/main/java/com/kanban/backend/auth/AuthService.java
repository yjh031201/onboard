package com.kanban.backend.auth;

import com.kanban.backend.auth.dto.AuthResponse;
import com.kanban.backend.auth.dto.FindIdRequest;
import com.kanban.backend.auth.dto.FindIdResponse;
import com.kanban.backend.auth.dto.LoginRequest;
import com.kanban.backend.auth.dto.ResetPasswordRequest;
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

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
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
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return issueToken(user);
    }

    private AuthResponse issueToken(User user) {
        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail());
        return AuthResponse.of(token, jwtTokenProvider.getExpirationMs(), UserResponse.from(user));
    }

    /** 이름+휴대폰번호로 본인 확인 후, 가입된 이메일(아이디)을 마스킹해서 돌려줌. */
    @Transactional(readOnly = true)
    public FindIdResponse findId(FindIdRequest request) {
        User user = userRepository.findByNameAndPhone(request.name(), request.phone())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "일치하는 회원 정보를 찾을 수 없습니다."));

        return new FindIdResponse(maskEmail(user.getEmail()));
    }

    /**
     * 이름+이메일 일치 여부로 본인 확인 후 새 비밀번호로 즉시 교체.
     * (이메일 발송 없는 간소화된 플로우 — AuthService.signup과 동일하게 CONFLICT/NOT_FOUND는 ApiException으로 처리)
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
                .filter(found -> found.getName().equals(request.name()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "일치하는 회원 정보를 찾을 수 없습니다."));

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        // user는 영속 상태(managed)이므로 트랜잭션 커밋 시 변경분이 자동 반영됨(dirty checking).
    }

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
