package com.kanban.backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = jwtProperties.expirationMs();
        this.refreshExpirationMs = jwtProperties.refreshExpirationMs();
    }

    public String createToken(Long userId, String email) {
        return build(userId, email, null, expirationMs);
    }

    public String createRefreshToken(Long userId, String email) {
        return build(userId, email, TYPE_REFRESH, refreshExpirationMs);
    }

    private String build(Long userId, String email, String type, long ttlMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMs);

        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiry);

        if (type != null) {
            builder.claim(CLAIM_TYPE, type);
        }

        return builder.signWith(key).compact();
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    /** Returns the user id (subject) if the token is a valid access token. */
    public Optional<Long> parseUserId(String token) {
        return parseClaims(token)
                .filter(claims -> !TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class)))
                .map(claims -> Long.valueOf(claims.getSubject()));
    }

    /** Returns the user id (subject) if the token is a valid refresh token. */
    public Optional<Long> parseRefreshUserId(String token) {
        return parseClaims(token)
                .filter(claims -> TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class)))
                .map(claims -> Long.valueOf(claims.getSubject()));
    }

    private Optional<Claims> parseClaims(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
