package com.kanban.backend.integration.slack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SlackWebhookRequest(
        @NotBlank(message = "웹훅 주소를 입력해주세요.") @Size(max = 500, message = "웹훅 주소가 너무 길어요.") String webhookUrl
) {
}
