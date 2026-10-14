package com.kanban.backend.integration.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.integration.ConnectState;
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
import org.springframework.web.client.RestClientResponseException;

@Service
public class GithubIntegrationService {

    private static final String AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String API_BASE = "https://api.github.com";
    private static final String NOT_CONNECTED = "이 프로젝트에 GitHub이 연결되어 있지 않습니다.";

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

    public String buildAuthorizeUrl(Long userId, Long projectId, String redirectUri) {
        String state = stateStore.issue(userId, projectId);
        return AUTHORIZE_URL
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&scope=" + encode("repo")
                + "&state=" + encode(state);
    }

    @Transactional
    public void handleCallback(String code, ConnectState state, String redirectUri) {
        integrationService.requireAdminForCallback(state);

        String accessToken = exchangeCodeForToken(code, redirectUri);
        String login = fetchLogin(accessToken);

        Integration integration = integrationRepository
                .findByProjectIdAndProvider(state.projectId(), IntegrationProvider.GITHUB)
                .orElseGet(() -> new Integration(state.projectId(), IntegrationProvider.GITHUB, accessToken, login, state.userId()));
        boolean accountChanged = integration.reconnect(accessToken, login, state.userId());
        if (accountChanged) {
            // 다른 GitHub 계정으로 바뀌면 이전 계정이 고른 저장소는 새 토큰으로 못 볼 수 있어서 비운다.
            integration.clearGithubRepo();
        }
        integrationRepository.save(integration);
    }

    @Transactional
    public void linkRepo(Long projectId, String owner, String repo, User currentUser) {
        integrationService.requireAdmin(projectId, currentUser);
        Integration integration = integrationService.requireConnected(projectId, IntegrationProvider.GITHUB, NOT_CONNECTED);
        // 오타로 없는 저장소를 저장해 두면 이슈 조회 때마다 실패하므로, 저장 전에 접근 가능한지 확인한다.
        githubGet("/repos/{owner}/{repo}", integration.getAccessToken(), owner, repo);
        integration.linkGithubRepo(owner, repo);
    }

    @Transactional(readOnly = true)
    public List<GithubIssueResponse> listIssues(Long projectId, User currentUser) {
        integrationService.requireMember(projectId, currentUser);
        Integration integration = integrationService.requireConnected(projectId, IntegrationProvider.GITHUB, NOT_CONNECTED);
        if (integration.getGithubOwner() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "먼저 연결할 저장소를 설정해주세요.");
        }

        String body = githubGet("/repos/{owner}/{repo}/issues?state=open&per_page=20",
                integration.getAccessToken(), integration.getGithubOwner(), integration.getGithubRepo());
        return parseIssues(body);
    }

    /** GitHub API GET — 실패 원인을 사용자에게 보여줄 수 있는 메시지로 바꾼다 (그냥 두면 500으로 나간다). */
    private String githubGet(String path, String accessToken, Object... uriVariables) {
        try {
            return restClient.get()
                    .uri(API_BASE + path, uriVariables)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 404) {
                // 비공개 저장소에 권한이 없을 때도 GitHub은 404를 준다.
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "저장소를 찾을 수 없어요. 소유자·저장소 이름과, 연결한 GitHub 계정이 접근할 수 있는 저장소인지 확인해주세요.");
            }
            if (status == 401) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "GitHub 연동이 만료됐어요. 연동을 해제하고 다시 연결해주세요.");
            }
            if (status == 403) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "GitHub이 접근을 거부했어요. 조직 저장소라면 조직 설정에서 이 앱의 접근을 허용해야 해요.");
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "GitHub에서 정보를 불러오지 못했어요.");
        }
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
        try {
            String body = githubGet("/user", accessToken);
            return objectMapper.readTree(body).path("login").asText("GitHub");
        } catch (Exception e) {
            return "GitHub";
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
