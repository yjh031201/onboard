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
import com.kanban.backend.integration.github.dto.GithubRepoResponse;
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

    /**
     * 저장소 연결 — GitHub에 실제로 있는 저장소인지 먼저 확인하고, GitHub이 돌려준 정확한 이름(대소문자 포함)으로 저장한다.
     * 없는 저장소를 저장해 두고 이슈 목록에서야 실패하는 일이 없게 한다.
     */
    @Transactional
    public void linkRepo(String owner, String repo, User currentUser) {
        integrationService.requireAdmin(currentUser);
        Integration integration = requireConnected();

        JsonNode found;
        try {
            found = readJson(githubGet(integration, "/repos/{owner}/{repo}", owner.trim(), repo.trim()));
        } catch (RestClientResponseException e) {
            throw translate(e, "저장소를 찾을 수 없어요. 소유자/저장소 이름을 확인해주세요.", "저장소를 확인하지 못했어요.");
        }
        integration.linkGithubRepo(
                found.path("owner").path("login").asText(owner.trim()),
                found.path("name").asText(repo.trim())
        );
    }

    /**
     * 소유자의 저장소 목록 (최근에 고친 순, 최대 100개).
     * 연결한 계정 본인이면 비공개 저장소까지, 조직이면 그 조직에서 볼 수 있는 저장소를, 다른 사용자면 공개 저장소만 보인다.
     */
    @Transactional(readOnly = true)
    public List<GithubRepoResponse> listRepos(String owner, User currentUser) {
        integrationService.requireAdmin(currentUser);
        Integration integration = requireConnected();
        String trimmed = owner == null ? "" : owner.trim();
        if (trimmed.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "저장소 소유자를 입력해주세요.");
        }

        String body;
        try {
            if (trimmed.equalsIgnoreCase(integration.getAccountLabel())) {
                body = githubGet(integration, "/user/repos?affiliation=owner&sort=updated&per_page=100");
            } else {
                body = listOrgOrUserRepos(integration, trimmed);
            }
        } catch (RestClientResponseException e) {
            throw translate(e, "'%s' 소유자를 찾을 수 없어요.".formatted(trimmed), "저장소 목록을 불러오지 못했어요.");
        }

        List<GithubRepoResponse> repos = new ArrayList<>();
        for (JsonNode node : readJson(body)) {
            String description = node.path("description").asText("");
            repos.add(new GithubRepoResponse(
                    node.path("name").asText(""),
                    node.path("private").asBoolean(false),
                    description.isBlank() ? null : description
            ));
        }
        return repos;
    }

    /** 조직이면 조직 저장소(멤버라면 비공개 포함), 조직이 아니면(404) 그 사용자의 공개 저장소. */
    private String listOrgOrUserRepos(Integration integration, String owner) {
        try {
            return githubGet(integration, "/orgs/{owner}/repos?sort=updated&per_page=100", owner);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() != 404) {
                throw e;
            }
            return githubGet(integration, "/users/{owner}/repos?sort=updated&per_page=100", owner);
        }
    }

    private String githubGet(Integration integration, String path, Object... uriVariables) {
        return restClient.get()
                .uri(API_BASE + path, uriVariables)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + integration.getAccessToken())
                .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .retrieve()
                .body(String.class);
    }

    private JsonNode readJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "GitHub 응답을 처리하지 못했어요.");
        }
    }

    /** GitHub이 거절한 이유를 그대로 500으로 흘리지 않고, 사용자가 고칠 수 있는 안내로 바꾼다. */
    private ApiException translate(RestClientResponseException e, String notFoundMessage, String fallbackMessage) {
        int status = e.getStatusCode().value();
        if (status == 404) {
            return new ApiException(HttpStatus.BAD_REQUEST, notFoundMessage);
        }
        if (status == 401) {
            return new ApiException(HttpStatus.BAD_REQUEST, "GitHub 연동이 만료됐어요. 연결을 해제하고 다시 연결해주세요.");
        }
        return new ApiException(HttpStatus.BAD_GATEWAY, fallbackMessage);
    }

    @Transactional(readOnly = true)
    public List<GithubIssueResponse> listIssues() {
        Integration integration = requireConnected();
        if (integration.getGithubOwner() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "먼저 연결할 저장소를 설정해주세요.");
        }

        String body;
        try {
            body = restClient.get()
                    .uri(API_BASE + "/repos/{owner}/{repo}/issues?state=open&per_page=20",
                            integration.getGithubOwner(), integration.getGithubRepo())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + integration.getAccessToken())
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            // GitHub이 거절한 이유를 그대로 500으로 흘리지 않고, 사용자가 고칠 수 있는 안내로 바꾼다.
            int status = e.getStatusCode().value();
            if (status == 404) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "저장소를 찾을 수 없어요. 소유자/저장소 이름을 확인해주세요.");
            }
            if (status == 401) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "GitHub 연동이 만료됐어요. 연결을 해제하고 다시 연결해주세요.");
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "GitHub 이슈 목록을 불러오지 못했어요.");
        }

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
