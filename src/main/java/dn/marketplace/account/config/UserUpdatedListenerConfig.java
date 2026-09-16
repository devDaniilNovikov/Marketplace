package dn.marketplace.account.config;

import dn.marketplace.account.api.event.UserUpdatedEvent;
import dn.marketplace.account.service.UserUpdatedHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class UserUpdatedListenerConfig {

    private final UserUpdatedHandler userUpdatedHandler;
    private final JsonMapper jsonMapper;

    @Bean
    RedisMessageListenerContainer userUpdatedListenerContainer(
            RedisConnectionFactory connectionFactory,
            @Value("${marketplace.keycloak.events-channel}") String channel) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(userUpdatedMessageListener(), new ChannelTopic(channel));
        return container;
    }

    @Bean
    MessageListener userUpdatedMessageListener() {
        return (Message message, byte[] pattern) -> {
            try {
                UserUpdatedEvent event = jsonMapper.readValue(message.getBody(), UserUpdatedEvent.class);
                userUpdatedHandler.handle(event);
            } catch (RuntimeException e) {
                log.error("Не удалось обработать USER_UPDATED", e);
            }
        };
    }
}
