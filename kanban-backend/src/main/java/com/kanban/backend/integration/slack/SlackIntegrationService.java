package com.kanban.backend.integration.slack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.integration.Integration;
import com.kanban.backend.integration.IntegrationProvider;
import com.kanban.backend.integration.IntegrationRepository;
import com.kanban.backend.integration.IntegrationStateStore;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class SlackIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(SlackIntegrationService.class);

    private static final String AUTHORIZE_URL = "https://slack.com/oauth/v2/authorize";
    private static final String TOKEN_URL = "https://slack.com/api/oauth.v2.access";
    private static final String SCOPES = "incoming-webhook,chat:write";

    private final IntegrationRepository integrationRepository;
    private final IntegrationStateStore stateStore;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    public SlackIntegrationService(
            IntegrationRepository integrationRepository,
            IntegrationStateStore stateStore,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            @Value("${integrations.slack.client-id}") String clientId,
            @Value("${integrations.slack.client-secret}") String clientSecret
    ) {
        this.integrationRepository = integrationRepository;
        this.stateStore = stateStore;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String buildAuthorizeUrl(Long userId, String redirectUri) {
        String state = stateStore.issue(userId);
        return AUTHORIZE_URL
                + "?client_id=" + encode(clientId)
                + "&scope=" + encode(SCOPES)
                + "&redirect_uri=" + encode(redirectUri)
                + "&state=" + encode(state);
    }

    @Transactional
    public void handleCallback(String code, String state, String redirectUri) {
        Long userId = stateStore.redeem(state);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("redirect_uri", redirectUri);

        String body = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);

        JsonNode json;
        try {
            json = objectMapper.readTree(body);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Slack 인증 응답을 처리하지 못했어요.");
        }

        if (!json.path("ok").asBoolean(false)) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Slack 인증에 실패했어요: " + json.path("error").asText("unknown"));
        }

        String accessToken = json.path("access_token").asText(null);
        String teamName = json.path("team").path("name").asText("Slack");
        String webhookUrl = json.path("incoming_webhook").path("url").asText(null);
        String channel = json.path("incoming_webhook").path("channel").asText(null);

        if (accessToken == null || webhookUrl == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Slack에서 필요한 권한(incoming-webhook)을 받지 못했어요.");
        }

        Integration integration = integrationRepository.findByProvider(IntegrationProvider.SLACK)
                .orElseGet(() -> new Integration(IntegrationProvider.SLACK, accessToken, teamName, userId));
        integration.updateTokens(accessToken, null, null);
        integration.setSlackWebhook(webhookUrl, channel);
        integrationRepository.save(integration);
    }

    public void sendTestMessage() {
        Integration integration = integrationRepository.findByProvider(IntegrationProvider.SLACK)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Slack이 연결되어 있지 않습니다."));
        postToWebhook(integration.getSlackWebhookUrl(), "🔔 칸반보드 Slack 연동 테스트 메시지입니다.");
    }

    /** 카드 이동 등 알림 이벤트 발생 시 호출 — Slack이 연결 안 돼 있거나 전송 실패해도 호출자 흐름을 막지 않는다. */
    public void notifyIfConnected(String text) {
        integrationRepository.findByProvider(IntegrationProvider.SLACK).ifPresent(integration -> {
            try {
                postToWebhook(integration.getSlackWebhookUrl(), text);
            } catch (Exception e) {
                log.warn("Slack 알림 전송 실패", e);
            }
        });
    }

    private void postToWebhook(String webhookUrl, String text) {
        restClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("text", text))
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .toBodilessEntity();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
