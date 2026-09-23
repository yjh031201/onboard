package com.kanban.backend.auth.oauth2;

import com.kanban.backend.user.AuthProvider;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuthUserInfo info = OAuthUserInfo.of(registrationId, oAuth2User.getAttributes());

        if (!info.emailVerified()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_verified"), "이메일 인증이 확인되지 않아 로그인할 수 없습니다.");
        }

        User user = resolveUser(info);
        return new OAuth2UserPrincipal(user, oAuth2User.getAttributes());
    }

    private User resolveUser(OAuthUserInfo info) {
        return userRepository.findByProviderAndProviderId(info.provider(), info.providerId())
                .orElseGet(() -> userRepository.findByEmail(info.email())
                        .map(existing -> linkOrReject(existing, info))
                        .orElseGet(() -> userRepository.save(
                                new User(info.email(), info.name(), info.provider(), info.providerId()))));
    }

    /** 이미 같은 이메일로 가입된 계정이 있으면 로컬 계정에 한해서만 연결(link)해준다. */
    private User linkOrReject(User existing, OAuthUserInfo info) {
        if (existing.getProvider() != AuthProvider.LOCAL) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("provider_conflict"),
                    "이미 다른 방식으로 가입된 이메일입니다. 기존 로그인 방식을 이용해주세요.");
        }
        existing.linkProvider(info.provider(), info.providerId());
        return existing;
    }
}
