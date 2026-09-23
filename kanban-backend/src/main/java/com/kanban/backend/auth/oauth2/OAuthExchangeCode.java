package com.kanban.backend.auth.oauth2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 소셜 로그인 성공 후 실제 JWT를 받아가기 위한 1회용 교환 코드.
 * 원본 코드는 저장하지 않고 SHA-256 해시만 저장 — DB가 털려도 코드 자체는 복원 불가.
 */
@Entity
@Table(name = "oauth_exchange_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuthExchangeCode {

    private static final int TTL_SECONDS = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "code_hash", nullable = false, unique = true, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "redeemed_at")
    private LocalDateTime redeemedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public OAuthExchangeCode(Long userId, String codeHash) {
        this.userId = userId;
        this.codeHash = codeHash;
        this.expiresAt = LocalDateTime.now().plusSeconds(TTL_SECONDS);
    }

    public boolean isRedeemed() {
        return redeemedAt != null;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void redeem() {
        this.redeemedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
