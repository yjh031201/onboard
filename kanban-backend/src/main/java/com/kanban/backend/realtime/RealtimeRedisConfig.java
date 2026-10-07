package com.kanban.backend.realtime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Disabled under the "test" profile: the listener container eagerly opens a
 * subscribe connection on startup, which would make every test require a
 * live Redis instance. The "test" profile is specifically the one that runs
 * without any external services (see KanbanBackendApplicationTests), so it
 * skips this the same way it swaps MySQL for H2.
 *
 * Board/label/column/timeline/presence/settings channels are all one-per-project
 * ("realtime:projects:{id}:board", "realtime:projects:{id}:presence" 등), so a
 * single pattern subscription covers every project and every channel kind
 * instead of registering a fixed channel list — see {@link RealtimeRedisSubscriber}
 * for how the matching STOMP topic is derived.
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
        container.addMessageListener(subscriber, new PatternTopic("realtime:projects:*"));
        return container;
    }
}
