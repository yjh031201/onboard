-- GitHub/Slack/Google Drive 연동 상태 저장. 단일 워크스페이스 앱이라 provider당 한 행만 존재한다.

CREATE TABLE integrations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider VARCHAR(30) NOT NULL,
    access_token VARCHAR(1000) NOT NULL,
    refresh_token VARCHAR(1000) NULL,
    token_expires_at DATETIME(6) NULL,
    account_label VARCHAR(200) NULL,
    github_owner VARCHAR(100) NULL,
    github_repo VARCHAR(100) NULL,
    slack_webhook_url VARCHAR(500) NULL,
    slack_channel VARCHAR(100) NULL,
    connected_by_id BIGINT NOT NULL,
    connected_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_integrations_provider (provider)
) ENGINE=InnoDB;
