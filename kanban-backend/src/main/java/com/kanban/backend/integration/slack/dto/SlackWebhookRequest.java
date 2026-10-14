package com.kanban.backend.integration.slack.dto;

import jakarta.validation.constraints.NotBlank;

public record SlackWebhookRequest(
        @NotBlank(message = "웹훅 주소를 입력해주세요.") String webhookUrl
) {
}
