package com.kanban.backend.realtime;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-WebSocket setup. The broker here only ever pushes server -> client
 * (board/presence/timeline updates); all mutations still go through the
 * existing REST API, which keeps auth/validation in one place. See
 * {@link RealtimeChannels} for how a publish reaches every connected client
 * through the Redis backplane.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final RealtimeProperties realtimeProperties;
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    public WebSocketConfig(
            RealtimeProperties realtimeProperties,
            StompAuthChannelInterceptor stompAuthChannelInterceptor
    ) {
        this.realtimeProperties = realtimeProperties;
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(realtimeProperties.allowedOrigin())
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
