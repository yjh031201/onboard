package com.kanban.backend.auth.oauth2;

import com.kanban.backend.common.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소셜 로그인 성공 후 프론트가 access/refresh token을 받아가기 위한 1회용 교환 코드 발급/검증.
 *
 * access token을 리다이렉트 URL에 직접 실으면 브라우저 히스토리·서버 접근 로그·리퍼러 헤더에
 * 남을 수 있어서, 그 대신 60초짜리 1회용 코드만 노출시키고 실제 토큰 발급은
 * POST /api/auth/oauth/exchange 안에서 처리한다(코드가 새더라도 곧 만료+1회용이라 위험이 작음).
 */
@Service
public class OAuthCodeExchangeService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OAuthExchangeCodeRepository repository;

    public OAuthCodeExchangeService(OAuthExchangeCodeRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public String issue(Long userId) {
        String rawCode = generateRandomCode();
        repository.save(new OAuthExchangeCode(userId, sha256Hex(rawCode)));
        return rawCode;
    }

    @Transactional
    public Long redeem(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "교환 코드가 없습니다.");
        }

        OAuthExchangeCode stored = repository.findByCodeHash(sha256Hex(rawCode))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "유효하지 않은 교환 코드입니다."));

        if (stored.isRedeemed()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "이미 사용된 교환 코드입니다.");
        }
        if (stored.isExpired()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "만료된 교환 코드입니다.");
        }

        stored.redeem();
        return stored.getUserId();
    }

    private static String generateRandomCode() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return toHex(bytes);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
