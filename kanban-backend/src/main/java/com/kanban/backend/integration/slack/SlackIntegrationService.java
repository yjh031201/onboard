package com.kanban.backend.integration.slack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.integration.ConnectState;
import com.kanban.backend.integration.Integration;
import com.kanban.backend.integration.IntegrationProvider;
import com.kanban.backend.integration.IntegrationRepository;
import com.kanban.backend.integration.IntegrationService;
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
    private static final String WEBHOOK_URL_PREFIX = "https://hooks.slack.com/services/";
    /** 웹훅 주소로 연결한 경우엔 OAuth 토큰이 없다 — access_token이 NOT NULL이라 넣어 두는 자리 표시 값. */
    private static final String WEBHOOK_ONLY_TOKEN = "webhook";

    private final IntegrationRepository integrationRepository;
    private final IntegrationStateStore stateStore;
    private final IntegrationService integrationService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    public SlackIntegrationService(
            IntegrationRepository integrationRepository,
            IntegrationStateStore stateStore,
            IntegrationService integrationService,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            @Value("${integrations.slack.client-id}") String clientId,
            @Value("${integrations.slack.client-secret}") String clientSecret
    ) {
        this.integrationRepository = integrationRepository;
        this.stateStore = stateStore;
        this.integrationService = integrationService;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String buildAuthorizeUrl(Long userId, Long projectId, String redirectUri) {
        if (clientId.startsWith("dummy-")) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "서버에 Slack 앱 키가 설정되어 있지 않아요. 웹훅 주소로 연결해주세요.");
        }
        String state = stateStore.issue(userId, projectId);
        return AUTHORIZE_URL
                + "?client_id=" + encode(clientId)
                + "&scope=" + encode(SCOPES)
                + "&redirect_uri=" + encode(redirectUri)
                + "&state=" + encode(state);
    }

    @Transactional
    public void handleCallback(String code, ConnectState state, String redirectUri) {
        integrationService.requireAdminForCallback(state);

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

        Integration integration = integrationRepository
                .findByProjectIdAndProvider(state.projectId(), IntegrationProvider.SLACK)
                .orElseGet(() -> new Integration(state.projectId(), IntegrationProvider.SLACK, accessToken, teamName, state.userId()));
        integration.reconnect(accessToken, teamName, state.userId());
        integration.setSlackWebhook(webhookUrl, channel);
        integrationRepository.save(integration);
    }

    /**
     * OAuth 없이 Slack Incoming Webhook 주소를 직접 받아 이 프로젝트에 연결한다 — Slack 앱 키(client id/secret)나
     * HTTPS 콜백 주소가 없는 환경(로컬 개발 등)에서도 쓸 수 있다. 저장 전에 실제로 메시지를 보내 주소를 검증한다.
     * 권한 체크(프로젝트 관리자)는 컨트롤러가 한다.
     */
    @Transactional
    public void connectWithWebhook(Long projectId, Long userId, String rawWebhookUrl) {
        String webhookUrl = rawWebhookUrl.trim();
        // 서버가 임의의 주소로 요청을 보내지 않도록 Slack 웹훅 주소만 받는다.
        if (!webhookUrl.startsWith(WEBHOOK_URL_PREFIX)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Slack 웹훅 주소는 " + WEBHOOK_URL_PREFIX + " 로 시작해야 해요.");
        }

        try {
            postToWebhook(webhookUrl, "✅ 칸반보드와 Slack이 연결됐어요.");
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "이 웹훅 주소로 메시지를 보내지 못했어요. 주소를 다시 확인해주세요.");
        }

        Integration integration = integrationRepository
                .findByProjectIdAndProvider(projectId, IntegrationProvider.SLACK)
                .orElseGet(() -> new Integration(projectId, IntegrationProvider.SLACK, WEBHOOK_ONLY_TOKEN, "Slack 웹훅", userId));
        integration.reconnect(WEBHOOK_ONLY_TOKEN, "Slack 웹훅", userId);
        integration.setSlackWebhook(webhookUrl, null);
        integrationRepository.save(integration);
    }

    public void sendTestMessage(Long projectId) {
        Integration integration = integrationService.requireConnected(
                projectId, IntegrationProvider.SLACK, "이 프로젝트에 Slack이 연결되어 있지 않습니다.");
        postToWebhook(integration.getSlackWebhookUrl(), "🔔 칸반보드 Slack 연동 테스트 메시지입니다.");
    }

    /**
     * 카드 이동 등 알림 이벤트 발생 시 호출 — 그 프로젝트에 연결된 Slack 채널로만 보낸다.
     * Slack이 연결 안 돼 있거나 전송 실패해도 호출자 흐름을 막지 않는다.
     */
    public void notifyIfConnected(Long projectId, String text) {
        integrationRepository.findByProjectIdAndProvider(projectId, IntegrationProvider.SLACK).ifPresent(integration -> {
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
