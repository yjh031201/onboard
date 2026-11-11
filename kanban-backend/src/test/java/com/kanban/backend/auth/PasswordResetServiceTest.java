package com.kanban.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kanban.backend.common.ApiException;
import com.kanban.backend.common.redis.AttemptLimiter;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRepository;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class PasswordResetServiceTest {

    private final Map<String, String> redis = new HashMap<>();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserRepository userRepository;
    private RefreshTokenStore refreshTokenStore;
    private AttemptLimiter attemptLimiter;
    private JavaMailSender mailSender;
    private StringRedisTemplate redisTemplate;
    private User user;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenStore = mock(RefreshTokenStore.class);
        attemptLimiter = mock(AttemptLimiter.class);
        mailSender = mock(JavaMailSender.class);

        redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenAnswer(inv -> redis.get(inv.<String>getArgument(0)));
        doAnswer(inv -> redis.put(inv.getArgument(0), inv.getArgument(1)))
                .when(ops).set(anyString(), anyString(), any(Duration.class));
        when(redisTemplate.delete(anyString())).thenAnswer(inv -> redis.remove(inv.<String>getArgument(0)) != null);

        user = new User("me@example.com", passwordEncoder.encode("oldPassword1"), "나", "010-0000-0000");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findByEmail("me@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());
    }

    private PasswordResetService service(String mailHost, boolean logCode) {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(mailSender);
        return new PasswordResetService(userRepository, passwordEncoder, refreshTokenStore, redisTemplate,
                attemptLimiter, provider, mailHost, "noreply@example.com", logCode);
    }

    private String sentCode() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        Matcher matcher = Pattern.compile("(\\d{6})").matcher(captor.getValue().getText());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    @Test
    void 메일로_받은_코드로_비밀번호를_바꾸고_다른_기기_로그인을_끊는다() {
        PasswordResetService service = service("smtp.example.com", false);

        service.requestCode("Me@Example.com ");
        service.confirm("me@example.com", sentCode(), "newPassword1");

        assertThat(passwordEncoder.matches("newPassword1", user.getPassword())).isTrue();
        verify(refreshTokenStore).invalidate(1L);
        assertThat(redis).noneSatisfy((key, value) -> assertThat(key).startsWith("password-reset:code:"));
    }

    @Test
    void 코드는_해시로만_저장된다() {
        PasswordResetService service = service("smtp.example.com", false);

        service.requestCode("me@example.com");

        assertThat(redis.get("password-reset:code:me@example.com")).isNotEqualTo(sentCode()).hasSize(64);
    }

    @Test
    void 틀린_코드는_거절하고_실패로_센다() {
        PasswordResetService service = service("smtp.example.com", false);
        service.requestCode("me@example.com");
        String wrong = sentCode().equals("000000") ? "111111" : "000000";

        assertThatThrownBy(() -> service.confirm("me@example.com", wrong, "newPassword1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("인증 코드");

        assertThat(passwordEncoder.matches("oldPassword1", user.getPassword())).isTrue();
        verify(attemptLimiter).recordFailure(eq("password-reset"), eq("me@example.com"), any());
    }

    @Test
    void 가입하지_않은_이메일도_같은_응답이고_메일은_보내지_않는다() {
        PasswordResetService service = service("smtp.example.com", false);

        service.requestCode("nobody@example.com");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        assertThat(redis).isEmpty();
    }

    @Test
    void 메일_설정이_없으면_재설정을_막는다() {
        PasswordResetService service = service("", false);

        assertThatThrownBy(() -> service.requestCode("me@example.com"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
