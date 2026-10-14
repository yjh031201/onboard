package com.kanban.backend.integration;

import com.kanban.backend.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * GitHub/Slack/Drive OAuth 콜백 공통 처리.
 * 콜백 주소(/api/integrations/{slug}/callback)는 각 서비스 콘솔에 고정으로 등록돼 있어 프로젝트 id를 담을 수 없다 —
 * state로 프로젝트를 되찾고, 끝나면 그 프로젝트의 멤버 페이지로 돌려보낸다.
 */
@Component
public class IntegrationCallbackSupport {

    private static final Logger log = LoggerFactory.getLogger(IntegrationCallbackSupport.class);

    @FunctionalInterface
    public interface CallbackHandler {
        void handle(String code, ConnectState state);
    }

    private final IntegrationStateStore stateStore;
    private final String frontendBaseUrl;

    public IntegrationCallbackSupport(
            IntegrationStateStore stateStore,
            @Value("${app.frontend-base-url}") String frontendBaseUrl
    ) {
        this.stateStore = stateStore;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    /** 외부 서비스에 등록한 콜백 주소 — /connect 때와 콜백 때 똑같이 만들어야 토큰 교환이 된다. */
    public String callbackUrl(HttpServletRequest request, String slug) {
        return ServletUriComponentsBuilder.fromContextPath(request)
                .path("/api/integrations/" + slug + "/callback")
                .toUriString();
    }

    /** 콜백을 처리하고, 브라우저를 보낼 프론트 주소를 돌려준다. */
    public String handle(String slug, String providerName, String code, String state, String error, CallbackHandler handler) {
        ConnectState connectState;
        try {
            if (state == null) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "state 없음");
            }
            connectState = stateStore.redeem(state);
        } catch (Exception e) {
            return redirectUrl(null, slug, "error", providerName + " 연동 요청이 만료됐어요. 다시 시도해주세요.");
        }

        if (error != null || code == null) {
            return redirectUrl(connectState.projectId(), slug, "error", providerName + " 연동이 취소되었어요.");
        }

        try {
            handler.handle(code, connectState);
            return redirectUrl(connectState.projectId(), slug, "connected", null);
        } catch (ApiException e) {
            log.warn("{} 연동 콜백 처리 실패: {}", providerName, e.getMessage());
            return redirectUrl(connectState.projectId(), slug, "error", e.getMessage());
        } catch (Exception e) {
            log.warn("{} 연동 콜백 처리 실패", providerName, e);
            return redirectUrl(connectState.projectId(), slug, "error", providerName + " 연동에 실패했어요.");
        }
    }

    private String redirectUrl(Long projectId, String slug, String status, String message) {
        String path = projectId != null ? "/projects/" + projectId + "/members" : "/";
        String url = frontendBaseUrl + path + "?integration=" + slug + "&status=" + status;
        if (message != null) {
            url += "&message=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
        }
        return url;
    }
}
