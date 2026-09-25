package com.kanban.backend.realtime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Disabled under the "test" profile: the listener container eagerly opens a
 * subscribe connection on startup, which would make every test require a
 * live Redis instance. The "test" profile is specifically the one that runs
 * without any external services (see KanbanBackendApplicationTests), so it
 * skips this the same way it swaps MySQL for H2.
 */
@Configuration
@Profile("!test")
public class RealtimeRedisConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RealtimeRedisSubscriber subscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(RealtimeChannels.BOARD_EVENTS));
        container.addMessageListener(subscriber, new ChannelTopic(RealtimeChannels.PRESENCE_EVENTS));
        container.addMessageListener(subscriber, new ChannelTopic(RealtimeChannels.TIMELINE_EVENTS));
        container.addMessageListener(subscriber, new ChannelTopic(RealtimeChannels.TIMELINE_DELETED_EVENTS));
        container.addMessageListener(subscriber, new ChannelTopic(RealtimeChannels.LABEL_EVENTS));
        container.addMessageListener(subscriber, new ChannelTopic(RealtimeChannels.COLUMN_EVENTS));
        return container;
    }
}
