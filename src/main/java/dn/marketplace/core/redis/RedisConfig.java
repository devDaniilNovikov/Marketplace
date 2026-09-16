package dn.marketplace.core.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * Redis: строковые ключи + JSON-значения.
 * ConnectionFactory, StringRedisTemplate и общий JsonMapper приложения даёт Spring Boot
 * из {@code spring.data.redis.*} / {@code spring.jackson.*} — здесь их не переопределяем.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // Отдельный маппер только для Redis: пишет @class в значение, чтобы Object
        // десериализовался в исходный тип, а не в LinkedHashMap. Белый список типов —
        // защита от подмены класса в payload (в канал пишет внешний Keycloak SPI).
        var typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("dn.marketplace")
                .allowIfSubType("java.util.")
                .build();

        var json = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .customize(builder -> builder.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))
                .build();

        var template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setValueSerializer(json);
        template.setHashValueSerializer(json);
        template.afterPropertiesSet();;
        return template;
    }
}
