package com.kanban.backend.integration.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.integration.Integration;
import com.kanban.backend.integration.IntegrationProvider;
import com.kanban.backend.integration.IntegrationRepository;
import com.kanban.backend.integration.IntegrationService;
import com.kanban.backend.integration.IntegrationStateStore;
import com.kanban.backend.integration.github.dto.GithubIssueResponse;
import com.kanban.backend.user.User;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
public class GithubIntegrationService {

    private static final String AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String API_BASE = "https://api.github.com";

    private final IntegrationRepository integrationRepository;
    private final IntegrationStateStore stateStore;
    private final IntegrationService integrationService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    public GithubIntegrationService(
            IntegrationRepository integrationRepository,
            IntegrationStateStore stateStore,
            IntegrationService integrationService,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            @Value("${integrations.github.client-id}") String clientId,
            @Value("${integrations.github.client-secret}") String clientSecret
    ) {
        this.integrationRepository = integrationRepository;
        this.stateStore = stateStore;
        this.integrationService = integrationService;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String buildAuthorizeUrl(Long userId, String redirectUri) {
        String state = stateStore.issue(userId);
        return AUTHORIZE_URL
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&scope=" + encode("repo")
                + "&state=" + encode(state);
    }

    @Transactional
    public void handleCallback(String code, String state, String redirectUri) {
        Long userId = stateStore.redeem(state);

        String accessToken = exchangeCodeForToken(code, redirectUri);
        String login = fetchLogin(accessToken);

        Integration integration = integrationRepository.findByProvider(IntegrationProvider.GITHUB)
                .orElseGet(() -> new Integration(IntegrationProvider.GITHUB, accessToken, login, userId));
        integration.updateTokens(accessToken, null, null);
        integrationRepository.save(integration);
    }

    @Transactional
    public void linkRepo(String owner, String repo, User currentUser) {
        integrationService.requireAdmin(currentUser);
        Integration integration = requireConnected();
        integration.linkGithubRepo(owner, repo);
    }

    @Transactional(readOnly = true)
    public List<GithubIssueResponse> listIssues() {
        Integration integration = requireConnected();
        if (integration.getGithubOwner() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "먼저 연결할 저장소를 설정해주세요.");
        }

        String body = restClient.get()
                .uri(API_BASE + "/repos/{owner}/{repo}/issues?state=open&per_page=20",
                        integration.getGithubOwner(), integration.getGithubRepo())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + integration.getAccessToken())
                .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .retrieve()
                .body(String.class);

        return parseIssues(body);
    }

    private List<GithubIssueResponse> parseIssues(String body) {
        List<GithubIssueResponse> issues = new ArrayList<>();
        try {
            for (JsonNode node : objectMapper.readTree(body)) {
                if (node.has("pull_request")) {
                    continue; // GitHub의 issues 엔드포인트는 PR도 같이 내려준다 — 제외.
                }
                issues.add(new GithubIssueResponse(
                        node.path("number").asLong(),
                        node.path("title").asText(""),
                        node.path("state").asText(""),
                        node.path("html_url").asText(""),
                        node.path("user").path("login").asText(""),
                        node.path("created_at").asText("")
                ));
            }
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "GitHub 이슈 목록을 불러오지 못했어요.");
        }
        return issues;
    }

    private String exchangeCodeForToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("redirect_uri", redirectUri);

        String body = restClient.post()
                .uri(TOKEN_URL)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);

        try {
            JsonNode json = objectMapper.readTree(body);
            String token = json.path("access_token").asText(null);
            if (token == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "GitHub 인증에 실패했어요: " + json.path("error_description").asText(body));
            }
            return token;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "GitHub 인증 응답을 처리하지 못했어요.");
        }
    }

    private String fetchLogin(String accessToken) {
        String body = restClient.get()
                .uri(API_BASE + "/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .retrieve()
                .body(String.class);
        try {
            return objectMapper.readTree(body).path("login").asText("GitHub");
        } catch (Exception e) {
            return "GitHub";
        }
    }

    private Integration requireConnected() {
        return integrationRepository.findByProvider(IntegrationProvider.GITHUB)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "GitHub이 연결되어 있지 않습니다."));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
