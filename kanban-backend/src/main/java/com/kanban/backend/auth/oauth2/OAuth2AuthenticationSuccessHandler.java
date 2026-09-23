package com.kanban.backend.auth.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthCodeExchangeService oAuthCodeExchangeService;
    private final String frontendBaseUrl;

    public OAuth2AuthenticationSuccessHandler(
            OAuthCodeExchangeService oAuthCodeExchangeService,
            @Value("${app.frontend-base-url}") String frontendBaseUrl
    ) {
        this.oAuthCodeExchangeService = oAuthCodeExchangeService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication
    ) throws IOException, ServletException {
        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        String code = oAuthCodeExchangeService.issue(principal.getUser().getId());
        response.sendRedirect(frontendBaseUrl + "/oauth/callback?code=" + code);
    }
}
