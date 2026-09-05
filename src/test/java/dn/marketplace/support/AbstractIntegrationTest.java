package dn.marketplace.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * База для интеграционных тестов: настоящие Postgres и Redis в контейнерах.
 * <p>
 * H2 сознательно не используется: половина того, что мы проверяем, —
 * это поведение самого PostgreSQL (частичные индексы, {@code ON CONFLICT},
 * {@code SKIP LOCKED}, {@code CHECK}, триггеры, {@code JSONB}). На H2 такие
 * тесты были бы зелёными и бессмысленными.
 * <p>
 * Контейнеры поднимаются один раз на всю JVM (статический блок, а не
 * {@code @Container}) и переиспользуются всеми наследниками — иначе каждый
 * тест-класс платил бы за старт Postgres заново.
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    protected static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

        // Keycloak в тестах не поднимаем. issuer-uri заставил бы resource server
        // сходить за метаданными на старте контекста и упасть; jwk-set-uri
        // резолвится лениво, поэтому контекст поднимается без Keycloak.
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://localhost:1/.well-known/jwks.json");
    }
}
