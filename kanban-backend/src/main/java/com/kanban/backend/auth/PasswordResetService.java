package com.kanban.backend.auth;

import com.kanban.backend.common.ApiException;
import com.kanban.backend.common.redis.AttemptLimiter;
import com.kanban.backend.common.redis.RedisKeys;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이메일 인증 코드로 비밀번호를 재설정한다.
 *
 * 예전에는 이름+이메일만 맞으면 바로 새 비밀번호로 바꿔줘서, 팀원 페이지에 보이는 이름·이메일만으로 남의 계정
 * (구글 로그인 계정 포함)을 가져갈 수 있었다. 지금은 그 메일함을 가진 사람만 받을 수 있는 6자리 코드를 확인한다.
 *
 * - 코드는 10분 동안만 유효하고 Redis에는 해시로만 저장한다.
 * - 코드 확인은 10분에 5번까지 틀릴 수 있다 (6자리라 무차별 대입 확률 5/1,000,000).
 * - 요청은 가입 여부와 상관없이 같은 응답을 준다 — 이 API로 가입된 이메일인지 알아낼 수 없게.
 * - 비밀번호가 바뀌면 refresh token을 지워서 다른 기기의 로그인을 끊는다.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final String CONFIRM_SCOPE = "password-reset";
    private static final int CONFIRM_MAX_FAILURES = 5;
    private static final String REQUEST_SCOPE = "password-reset-request";
    private static final int REQUEST_MAX = 5;
    private static final Duration REQUEST_WINDOW = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenStore refreshTokenStore;
    private final StringRedisTemplate redisTemplate;
    private final AttemptLimiter attemptLimiter;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String mailHost;
    private final String mailFrom;
    private final boolean logCode;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenStore refreshTokenStore,
            StringRedisTemplate redisTemplate,
            AttemptLimiter attemptLimiter,
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${app.mail-from:}") String mailFrom,
            @Value("${app.password-reset-log-code:false}") boolean logCode
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenStore = refreshTokenStore;
        this.redisTemplate = redisTemplate;
        this.attemptLimiter = attemptLimiter;
        this.mailSender = mailSender;
        this.mailHost = mailHost;
        this.mailFrom = mailFrom;
        this.logCode = logCode;
    }

    /** 인증 코드 발송. 가입 안 된 이메일이어도 조용히 끝난다 (같은 응답). */
    public void requestCode(String rawEmail) {
        String email = normalize(rawEmail);
        if (mailHost.isBlank() && !logCode) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "지금은 비밀번호 재설정 메일을 보낼 수 없어요. 관리자에게 문의해 주세요.");
        }
        // 메일 폭탄 방지 — 같은 이메일로 1시간에 5번까지.
        attemptLimiter.checkAllowed(REQUEST_SCOPE, email, REQUEST_MAX);
        attemptLimiter.recordFailure(REQUEST_SCOPE, email, REQUEST_WINDOW);

        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            return;
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        redisTemplate.opsForValue().set(RedisKeys.passwordResetCode(email), hash(code), CODE_TTL);
        attemptLimiter.reset(CONFIRM_SCOPE, email); // 새 코드에는 새 시도 횟수
        send(email, code);
    }

    @Transactional
    public void confirm(String rawEmail, String code, String newPassword) {
        String email = normalize(rawEmail);
        attemptLimiter.checkAllowed(CONFIRM_SCOPE, email, CONFIRM_MAX_FAILURES);

        String stored = redisTemplate.opsForValue().get(RedisKeys.passwordResetCode(email));
        boolean matches = stored != null && MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8), hash(code.trim()).getBytes(StandardCharsets.UTF_8));
        User user = matches ? userRepository.findByEmail(email).orElse(null) : null;
        if (user == null) {
            attemptLimiter.recordFailure(CONFIRM_SCOPE, email, CODE_TTL);
            throw new ApiException(HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않거나 만료됐어요.");
        }

        user.changePassword(passwordEncoder.encode(newPassword));
        redisTemplate.delete(RedisKeys.passwordResetCode(email));
        attemptLimiter.reset(CONFIRM_SCOPE, email);
        refreshTokenStore.invalidate(user.getId());
    }

    private void send(String email, String code) {
        if (mailHost.isBlank()) {
            // app.password-reset-log-code=true인 로컬 개발 환경에서만 여기로 온다.
            log.warn("[로컬 개발용] {} 비밀번호 재설정 인증 코드: {}", email, code);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (!mailFrom.isBlank()) {
            message.setFrom(mailFrom);
        }
        message.setTo(email);
        message.setSubject("[Onboard 칸반보드] 비밀번호 재설정 인증 코드");
        message.setText("""
                비밀번호 재설정 인증 코드: %s

                10분 안에 입력해 주세요.
                본인이 요청하지 않았다면 이 메일은 무시하셔도 됩니다. 비밀번호는 바뀌지 않아요.
                """.formatted(code));
        try {
            mailSender.getObject().send(message);
        } catch (MailException e) {
            log.warn("비밀번호 재설정 메일 전송 실패", e);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "메일을 보내지 못했어요. 잠시 후 다시 시도해 주세요.");
        }
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String hash(String code) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
