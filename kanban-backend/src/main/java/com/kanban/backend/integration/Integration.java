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
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트 하나에 외부 서비스 하나당 한 행 — (project_id, provider)가 유니크.
 * 프로젝트마다 자기 GitHub 저장소·Slack 워크스페이스·Google Drive를 따로 연결한다.
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

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
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

    public Integration(Long projectId, IntegrationProvider provider, String accessToken, String accountLabel, Long connectedById) {
        this.projectId = projectId;
        this.provider = provider;
        this.accessToken = accessToken;
        this.accountLabel = accountLabel;
        this.connectedById = connectedById;
        this.connectedAt = LocalDateTime.now();
    }

    /**
     * 다시 연결했을 때 — 토큰만 바꾸면 화면에는 처음 연결한 사람의 계정이 그대로 남는다.
     * 연결한 계정·사람·시각을 새로 기록하고, 다른 계정으로 바뀌었는지를 돌려준다.
     */
    public boolean reconnect(String accessToken, String accountLabel, Long connectedById) {
        boolean accountChanged = !Objects.equals(this.accountLabel, accountLabel);
        this.accessToken = accessToken;
        this.accountLabel = accountLabel;
        this.connectedById = connectedById;
        this.connectedAt = LocalDateTime.now();
        return accountChanged;
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

    public void clearGithubRepo() {
        this.githubOwner = null;
        this.githubRepo = null;
    }

    public void setSlackWebhook(String webhookUrl, String channel) {
        this.slackWebhookUrl = webhookUrl;
        this.slackChannel = channel;
    }

    public boolean isTokenExpired() {
        return tokenExpiresAt != null && tokenExpiresAt.isBefore(LocalDateTime.now());
    }
}
