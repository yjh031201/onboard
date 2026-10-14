package com.kanban.backend.integration.slack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.integration.ConnectState;
import com.kanban.backend.integration.Integration;
import com.kanban.backend.integration.IntegrationProvider;
import com.kanban.backend.integration.IntegrationRepository;
import com.kanban.backend.integration.IntegrationService;
import com.kanban.backend.integration.IntegrationStateStore;
import com.kanban.backend.project.ProjectAccessService;
import com.kanban.backend.user.UserRole;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Slack 서버 자리에 가짜 서버(MockRestServiceServer)를 세워 놓고 연결 → 저장 → 알림 전송 흐름을 실제로 돌려 본다. */
class SlackIntegrationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PROJECT_ID = 7L;
    /** 콜백 컨트롤러가 OAuth state에서 되찾아 넘겨주는 값. */
    private static final ConnectState STATE = new ConnectState(USER_ID, PROJECT_ID);
    private static final String REDIRECT_URI = "https://onboard-kanban.duckdns.org/api/integrations/slack/callback";
    private static final String TOKEN_URL = "https://slack.com/api/oauth.v2.access";
    private static final String WEBHOOK_URL = "https://hooks.slack.com/services/T000/B000/XXXX";

    private IntegrationRepository integrationRepository;
    private IntegrationStateStore stateStore;
    private IntegrationService integrationService;
    private MockRestServiceServer slack;
    private SlackIntegrationService slackIntegrationService;

    @BeforeEach
    void setUp() {
        integrationRepository = mock(IntegrationRepository.class);
        stateStore = mock(IntegrationStateStore.class);
        ProjectAccessService projectAccessService = mock(ProjectAccessService.class);
        when(projectAccessService.myRoleOrNull(PROJECT_ID, USER_ID)).thenReturn(UserRole.OWNER);
        integrationService = new IntegrationService(integrationRepository, projectAccessService);

        RestClient.Builder restClientBuilder = RestClient.builder();
        slack = MockRestServiceServer.bindTo(restClientBuilder).build();
        slackIntegrationService = new SlackIntegrationService(
                integrationRepository,
                stateStore,
                integrationService,
                new ObjectMapper(),
                restClientBuilder,
                "test-client-id",
                "test-client-secret"
        );
    }

    private Integration connectedSlack() {
        Integration integration = new Integration(PROJECT_ID, IntegrationProvider.SLACK, "xoxb-token", "우리 팀", USER_ID);
        integration.setSlackWebhook(WEBHOOK_URL, "#kanban");
        when(integrationRepository.findByProjectIdAndProvider(PROJECT_ID, IntegrationProvider.SLACK)).thenReturn(Optional.of(integration));
        return integration;
    }

    @Test
    void buildAuthorizeUrl_sendsUserToSlackConsentScreenWithStateAndScopes() {
        when(stateStore.issue(USER_ID, PROJECT_ID)).thenReturn("state-123");

        String url = slackIntegrationService.buildAuthorizeUrl(USER_ID, PROJECT_ID, REDIRECT_URI);

        assertThat(url)
                .startsWith("https://slack.com/oauth/v2/authorize?")
                .contains("client_id=test-client-id")
                .contains("scope=incoming-webhook%2Cchat%3Awrite")
                .contains("redirect_uri=https%3A%2F%2Fonboard-kanban.duckdns.org%2Fapi%2Fintegrations%2Fslack%2Fcallback")
                .contains("state=state-123");
    }

    @Test
    void handleCallback_exchangesCodeAndStoresWebhook() {
        when(integrationRepository.findByProjectIdAndProvider(PROJECT_ID, IntegrationProvider.SLACK)).thenReturn(Optional.empty());
        slack.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formDataContains(Map.of(
                        "client_id", "test-client-id",
                        "client_secret", "test-client-secret",
                        "code", "code-abc",
                        "redirect_uri", REDIRECT_URI)))
                .andRespond(withSuccess("""
                        {
                          "ok": true,
                          "access_token": "xoxb-token",
                          "team": {"id": "T000", "name": "우리 팀"},
                          "incoming_webhook": {"channel": "#kanban", "url": "%s"}
                        }
                        """.formatted(WEBHOOK_URL), MediaType.APPLICATION_JSON));

        slackIntegrationService.handleCallback("code-abc", STATE, REDIRECT_URI);

        slack.verify();
        ArgumentCaptor<Integration> saved = ArgumentCaptor.forClass(Integration.class);
        verify(integrationRepository).save(saved.capture());
        assertThat(saved.getValue().getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(saved.getValue().getProvider()).isEqualTo(IntegrationProvider.SLACK);
        assertThat(saved.getValue().getAccessToken()).isEqualTo("xoxb-token");
        assertThat(saved.getValue().getAccountLabel()).isEqualTo("우리 팀");
        assertThat(saved.getValue().getSlackWebhookUrl()).isEqualTo(WEBHOOK_URL);
        assertThat(saved.getValue().getSlackChannel()).isEqualTo("#kanban");
        assertThat(saved.getValue().getConnectedById()).isEqualTo(USER_ID);
    }

    @Test
    void handleCallback_slackRejects_failsWithoutSaving() {
        slack.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("{\"ok\": false, \"error\": \"invalid_code\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> slackIntegrationService.handleCallback("bad-code", STATE, REDIRECT_URI))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("invalid_code");

        verify(integrationRepository, never()).save(any());
    }

    @Test
    void handleCallback_withoutIncomingWebhookScope_failsWithoutSaving() {
        slack.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess(
                        "{\"ok\": true, \"access_token\": \"xoxb-token\", \"team\": {\"name\": \"우리 팀\"}}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> slackIntegrationService.handleCallback("code-abc", STATE, REDIRECT_URI))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("incoming-webhook");

        verify(integrationRepository, never()).save(any());
    }

    @Test
    void connectWithWebhook_verifiesUrlBySendingMessageThenStores() {
        when(integrationRepository.findByProjectIdAndProvider(PROJECT_ID, IntegrationProvider.SLACK)).thenReturn(Optional.empty());
        slack.expect(requestTo(WEBHOOK_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        slackIntegrationService.connectWithWebhook(PROJECT_ID, USER_ID, "  " + WEBHOOK_URL + " ");

        slack.verify();
        ArgumentCaptor<Integration> saved = ArgumentCaptor.forClass(Integration.class);
        verify(integrationRepository).save(saved.capture());
        assertThat(saved.getValue().getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(saved.getValue().getProvider()).isEqualTo(IntegrationProvider.SLACK);
        assertThat(saved.getValue().getSlackWebhookUrl()).isEqualTo(WEBHOOK_URL);
        assertThat(saved.getValue().getConnectedById()).isEqualTo(USER_ID);
    }

    @Test
    void connectWithWebhook_nonSlackUrl_isRejectedWithoutCallingIt() {
        assertThatThrownBy(() -> slackIntegrationService.connectWithWebhook(PROJECT_ID, USER_ID, "https://example.com/hook"))
                .isInstanceOf(ApiException.class);

        slack.verify();
        verify(integrationRepository, never()).save(any());
    }

    @Test
    void connectWithWebhook_slackRejectsUrl_failsWithoutSaving() {
        slack.expect(requestTo(WEBHOOK_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> slackIntegrationService.connectWithWebhook(PROJECT_ID, USER_ID, WEBHOOK_URL))
                .isInstanceOf(ApiException.class);

        verify(integrationRepository, never()).save(any());
    }

    @Test
    void buildAuthorizeUrl_withoutSlackAppKeys_isRejected() {
        SlackIntegrationService unconfigured = new SlackIntegrationService(
                integrationRepository,
                stateStore,
                integrationService,
                new ObjectMapper(),
                RestClient.builder(),
                "dummy-slack-client-id",
                "dummy-slack-client-secret"
        );

        assertThatThrownBy(() -> unconfigured.buildAuthorizeUrl(USER_ID, PROJECT_ID, REDIRECT_URI))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void notifyIfConnected_postsMessageToStoredWebhook() {
        connectedSlack();
        slack.expect(requestTo(WEBHOOK_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"text\": \"홍길동님이 '로그인' 카드를 할 일에 추가했습니다\"}"))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        slackIntegrationService.notifyIfConnected(PROJECT_ID, "홍길동님이 '로그인' 카드를 할 일에 추가했습니다");

        slack.verify();
    }

    @Test
    void notifyIfConnected_slackDown_doesNotBreakCaller() {
        connectedSlack();
        slack.expect(requestTo(WEBHOOK_URL)).andRespond(withServerError());

        assertThatCode(() -> slackIntegrationService.notifyIfConnected(PROJECT_ID, "알림"))
                .doesNotThrowAnyException();
    }

    @Test
    void notifyIfConnected_notConnected_sendsNothing() {
        when(integrationRepository.findByProjectIdAndProvider(PROJECT_ID, IntegrationProvider.SLACK)).thenReturn(Optional.empty());

        slackIntegrationService.notifyIfConnected(PROJECT_ID, "알림");

        slack.verify(); // 기대한 요청이 없으므로, 한 건이라도 나갔다면 위 호출에서 이미 실패한다.
    }

    @Test
    void sendTestMessage_notConnected_isRejected() {
        when(integrationRepository.findByProjectIdAndProvider(PROJECT_ID, IntegrationProvider.SLACK)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> slackIntegrationService.sendTestMessage(PROJECT_ID))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void sendTestMessage_connected_postsToWebhook() {
        connectedSlack();
        slack.expect(requestTo(WEBHOOK_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        slackIntegrationService.sendTestMessage(PROJECT_ID);

        slack.verify();
    }
}
