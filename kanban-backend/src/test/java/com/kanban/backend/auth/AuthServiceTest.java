package com.kanban.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kanban.backend.auth.dto.AuthResponse;
import com.kanban.backend.auth.dto.LoginRequest;
import com.kanban.backend.auth.dto.SignupRequest;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.config.JwtTokenProvider;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        jwtTokenProvider = mock(JwtTokenProvider.class);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("fake-jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86_400_000L);

        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider);
    }

    @Test
    void signup_createsUserAndReturnsToken() {
        SignupRequest request = new SignupRequest("양종호", "jdbdjhd8q@gmail.com", "password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        AuthResponse response = authService.signup(request);

        assertThat(response.accessToken()).isEqualTo("fake-jwt-token");
        assertThat(response.user().email()).isEqualTo(request.email());
        assertThat(response.user().name()).isEqualTo(request.name());
    }

    @Test
    void signup_rejectsDuplicateEmail() {
        SignupRequest request = new SignupRequest("양종호", "jdbdjhd8q@gmail.com", "password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("이미 가입된 이메일");
    }

    @Test
    void login_succeedsWithCorrectPassword() {
        User user = new User("jdbdjhd8q@gmail.com", passwordEncoder.encode("password123"), "양종호");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(new LoginRequest(user.getEmail(), "password123"));

        assertThat(response.accessToken()).isEqualTo("fake-jwt-token");
        assertThat(response.user().email()).isEqualTo(user.getEmail());
    }

    @Test
    void login_rejectsWrongPassword() {
        User user = new User("jdbdjhd8q@gmail.com", passwordEncoder.encode("password123"), "양종호");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest(user.getEmail(), "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_rejectsUnknownEmail() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "password123")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
