package com.kanban.backend.config;

import java.time.Duration;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 외부 API(GitHub·Slack·Google) 호출 대기 시간 제한.
 * 기본값은 제한이 없어서, 상대 서버가 응답하지 않으면 우리 요청도 끝없이 기다렸다.
 * 각 연동 서비스는 스프링이 주는 RestClient.Builder로 클라이언트를 만들기 때문에 여기서 한 번에 적용된다.
 */
@Configuration
public class HttpClientConfig {

    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    @Bean
    public RestClientCustomizer externalApiTimeouts() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(CONNECT_TIMEOUT)
                .withReadTimeout(READ_TIMEOUT);
        return builder -> builder.requestFactory(ClientHttpRequestFactories.get(settings));
    }
}
