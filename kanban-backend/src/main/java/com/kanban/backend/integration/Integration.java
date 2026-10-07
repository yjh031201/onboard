package com.kanban.backend.integration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 외부 서비스 하나당 한 행 — 이 프로젝트는 단일 워크스페이스라 팀/보드 단위 구분이 필요 없다.
 * accessToken/refreshToken은 이 앱의 다른 토큰들과 동일한 보안 수준(평문 저장, DB 접근 자체가
 * 신뢰 경계)으로 둔다 — RefreshTokenStore도 같은 방식.
 */
@Entity
@Table(name = "integrations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Integration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private IntegrationProvider provider;

    @Column(name = "access_token", nullable = false, length = 1000)
    private String accessToken;

    @Column(name = "refresh_token", length = 1000)
    private String refreshToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    /** 사람이 보기 좋은 연결 계정 식별자 — GitHub 로그인명, Slack 팀명, Google 이메일 등. */
    @Column(name = "account_label", length = 200)
    private String accountLabel;

    @Column(name = "github_owner", length = 100)
    private String githubOwner;

    @Column(name = "github_repo", length = 100)
    private String githubRepo;

    @Column(name = "slack_webhook_url", length = 500)
    private String slackWebhookUrl;

    @Column(name = "slack_channel", length = 100)
    private String slackChannel;

    @Column(name = "connected_by_id", nullable = false)
    private Long connectedById;

    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;

    public Integration(IntegrationProvider provider, String accessToken, String accountLabel, Long connectedById) {
        this.provider = provider;
        this.accessToken = accessToken;
        this.accountLabel = accountLabel;
        this.connectedById = connectedById;
        this.connectedAt = LocalDateTime.now();
    }

    public void updateTokens(String accessToken, String refreshToken, LocalDateTime tokenExpiresAt) {
        this.accessToken = accessToken;
        if (refreshToken != null) {
            this.refreshToken = refreshToken;
        }
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public void linkGithubRepo(String owner, String repo) {
        this.githubOwner = owner;
        this.githubRepo = repo;
    }

    public void setSlackWebhook(String webhookUrl, String channel) {
        this.slackWebhookUrl = webhookUrl;
        this.slackChannel = channel;
    }

    public boolean isTokenExpired() {
        return tokenExpiresAt != null && tokenExpiresAt.isBefore(LocalDateTime.now());
    }
}
