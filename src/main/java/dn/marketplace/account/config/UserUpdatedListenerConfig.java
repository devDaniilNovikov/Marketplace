package dn.marketplace.account.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@EnableConfigurationProperties(UserUpdatedMacProperties.class)
public class UserUpdatedListenerConfig {

    @Bean
    RedisMessageListenerContainer userUpdatedListenerContainer(
            RedisConnectionFactory connectionFactory,
            UserUpdatedMessageListener userUpdatedMessageListener,
            @Value("${marketplace.keycloak.events-channel}") String channel) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(serialExecutor());
        container.addMessageListener(userUpdatedMessageListener, new ChannelTopic(channel));
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
}
