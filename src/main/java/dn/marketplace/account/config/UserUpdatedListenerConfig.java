package dn.marketplace.account.config;

import dn.marketplace.account.api.event.UserUpdatedEvent;
import dn.marketplace.account.service.UserUpdatedHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
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
        container.setTaskExecutor(serialExecutor());
        container.addMessageListener(userUpdatedMessageListener(), new ChannelTopic(channel));
        return container;
    }

    /**
     * По умолчанию контейнер отдаёт каждое сообщение в новый поток, и два события одного
     * пользователя могут примениться в обратном порядке: старый payload перезапишет новый
     * (а {@code @Retryable} после {@code OptimisticLockingFailureException} это только закрепит).
     * Лимит 1 заставляет поток подписки ждать окончания предыдущей обработки — канал
     * потребляется строго по порядку. Не бин: иначе он вытеснит applicationTaskExecutor Boot'а.
     */
    private static SimpleAsyncTaskExecutor serialExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("user-updated-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(1);
        return executor;
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
