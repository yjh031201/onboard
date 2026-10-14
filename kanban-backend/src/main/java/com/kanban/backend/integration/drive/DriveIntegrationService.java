package com.kanban.backend.integration.drive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.integration.Integration;
import com.kanban.backend.integration.IntegrationProvider;
import com.kanban.backend.integration.IntegrationRepository;
import com.kanban.backend.integration.IntegrationStateStore;
import com.kanban.backend.integration.drive.dto.DriveFileResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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
public class DriveIntegrationService {

    private static final String AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    // oauth2/v2/userinfo는 email 스코프가 있어야 해서 drive.readonly 토큰으로는 401이 난다 — Drive API로 계정 이메일을 얻는다.
    private static final String ABOUT_URL = "https://www.googleapis.com/drive/v3/about?fields=user(emailAddress)";
    private static final String FILES_URL = "https://www.googleapis.com/drive/v3/files"
            + "?pageSize=20&fields=files(id,name,webViewLink,mimeType,modifiedTime)&orderBy=modifiedTime desc";
    private static final String SCOPE = "https://www.googleapis.com/auth/drive.readonly";

    private final IntegrationRepository integrationRepository;
    private final IntegrationStateStore stateStore;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    public DriveIntegrationService(
            IntegrationRepository integrationRepository,
            IntegrationStateStore stateStore,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            @Value("${integrations.drive.client-id}") String clientId,
            @Value("${integrations.drive.client-secret}") String clientSecret
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
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&scope=" + encode(SCOPE)
                + "&access_type=offline"
                + "&prompt=consent"
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
        form.add("grant_type", "authorization_code");

        JsonNode token = requestToken(form);
        String accessToken = token.path("access_token").asText(null);
        String refreshToken = token.path("refresh_token").asText(null);
        long expiresIn = token.path("expires_in").asLong(3600);

        if (accessToken == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Google Drive 인증에 실패했어요.");
        }

        String email = fetchEmail(accessToken);

        Integration integration = integrationRepository.findByProvider(IntegrationProvider.GOOGLE_DRIVE)
                .orElseGet(() -> new Integration(IntegrationProvider.GOOGLE_DRIVE, accessToken, email, userId));
        integration.updateTokens(accessToken, refreshToken, LocalDateTime.now().plusSeconds(expiresIn));
        integrationRepository.save(integration);
    }

    @Transactional
    public List<DriveFileResponse> listFiles() {
        Integration integration = requireConnected();
        String accessToken = ensureFreshToken(integration);

        String body = restClient.get()
                .uri(FILES_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(String.class);

        return parseFiles(body);
    }

    private String ensureFreshToken(Integration integration) {
        if (!integration.isTokenExpired()) {
            return integration.getAccessToken();
        }
        if (integration.getRefreshToken() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Google Drive 연동이 만료됐어요. 다시 연결해주세요.");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", integration.getRefreshToken());
        form.add("grant_type", "refresh_token");

        JsonNode token = requestToken(form);
        String accessToken = token.path("access_token").asText(null);
        long expiresIn = token.path("expires_in").asLong(3600);
        if (accessToken == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Google Drive 연동이 만료됐어요. 다시 연결해주세요.");
        }

        integration.updateTokens(accessToken, null, LocalDateTime.now().plusSeconds(expiresIn));
        return accessToken;
    }

    private JsonNode requestToken(MultiValueMap<String, String> form) {
        String body = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Google 인증 응답을 처리하지 못했어요.");
        }
    }

    private String fetchEmail(String accessToken) {
        try {
            String body = restClient.get()
                    .uri(ABOUT_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(body).path("user").path("emailAddress").asText("Google Drive");
        } catch (Exception e) {
            return "Google Drive"; // 표시용 이름이라 못 가져와도 연동 자체는 진행한다.
        }
    }

    private List<DriveFileResponse> parseFiles(String body) {
        List<DriveFileResponse> files = new ArrayList<>();
        try {
            for (JsonNode node : objectMapper.readTree(body).path("files")) {
                files.add(new DriveFileResponse(
                        node.path("id").asText(""),
                        node.path("name").asText(""),
                        node.path("webViewLink").asText(""),
                        node.path("mimeType").asText(""),
                        node.path("modifiedTime").asText("")
                ));
            }
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Google Drive 파일 목록을 불러오지 못했어요.");
        }
        return files;
    }

    private Integration requireConnected() {
        return integrationRepository.findByProvider(IntegrationProvider.GOOGLE_DRIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Google Drive가 연결되어 있지 않습니다."));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
