package dn.marketplace.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет, что миграции ядра реально применяются к настоящему PostgreSQL
 * и создают ту схему, на которую рассчитан код.
 * <p>
 * Контекст поднимается минимальный: только DataSource, Liquibase и JdbcTemplate.
 * JPA сюда намеренно не подключён — этот тест про миграции, и он должен
 * оставаться зелёным независимо от состояния сущностей.
 */
@SpringBootTest(
        classes = LiquibaseMigrationTest.MigrationOnlyApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class LiquibaseMigrationTest {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.liquibase.change-log", () -> "classpath:db/changelog/db.changelog-master.yaml");
        registry.add("spring.liquibase.liquibase-schema", () -> "public");
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class,
            LiquibaseAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class})
    static class MigrationOnlyApplication {
    }

    @Test
    @DisplayName("схема market_place создана")
    void schema_is_created(JdbcTemplate jdbc) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.schemata WHERE schema_name = 'market_place'",
                Integer.class);

        assertThat(count).isOne();
    }

    @Test
    @DisplayName("общая триггерная функция set_updated_at доступна")
    void trigger_function_exists(JdbcTemplate jdbc) {
        Integer count = jdbc.queryForObject("""
                SELECT count(*)
                FROM pg_proc p
                JOIN pg_namespace n ON n.oid = p.pronamespace
                WHERE n.nspname = 'market_place' AND p.proname = 'set_updated_at'
                """, Integer.class);

        assertThat(count).isOne();
    }

    @Test
    @DisplayName("outbox_messages имеет колонки, нужные воркеру с retry")
    void outbox_has_worker_columns(JdbcTemplate jdbc) {
        List<String> columns = jdbc.queryForList("""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'market_place' AND table_name = 'outbox_messages'
                """, String.class);

        assertThat(columns).contains(
                "id", "aggregate_type", "aggregate_id", "event_type", "payload",
                "status", "attempts", "error_message", "created_at", "next_retry_at", "processed_at");
    }

    @Test
    @DisplayName("aggregate_id хранится как UUID, а не как строка")
    void aggregate_id_is_uuid(JdbcTemplate jdbc) {
        String type = jdbc.queryForObject("""
                SELECT data_type
                FROM information_schema.columns
                WHERE table_schema = 'market_place'
                  AND table_name = 'outbox_messages'
                  AND column_name = 'aggregate_id'
                """, String.class);

        assertThat(type).isEqualTo("uuid");
    }

    @Test
    @DisplayName("accounts в схеме market_place, без колонки role")
    void accounts_table_matches_ssot(JdbcTemplate jdbc) {
        List<String> columns = jdbc.queryForList("""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'market_place' AND table_name = 'accounts'
                """, String.class);

        assertThat(columns)
                .contains("id", "user_name", "business_status", "banned", "email_snapshot",
                        "version", "deleted_at", "seller_applications", "rejection_reason")
                .doesNotContain("role");
    }

    @Test
    @DisplayName("индекс поллера частичный: SENT и DEAD в него не попадают")
    void poll_index_is_partial(JdbcTemplate jdbc) {
        String definition = jdbc.queryForObject("""
                SELECT indexdef FROM pg_indexes
                WHERE schemaname = 'market_place' AND indexname = 'idx_outbox_poll'
                """, String.class);

        assertThat(definition)
                .as("без WHERE индекс рос бы вместе с таблицей и поллер деградировал бы")
                .contains("WHERE")
                .contains("'PENDING'")
                .contains("'FAILED'");
    }

    @Test
    @DisplayName("CHECK на status не пропускает произвольные значения")
    void status_check_constraint_is_enforced(JdbcTemplate jdbc) {
        assertThat(insertOutboxWithStatus(jdbc, "PENDING"))
                .as("валидный статус должен проходить")
                .isTrue();

        assertThat(insertOutboxWithStatus(jdbc, "WHATEVER"))
                .as("невалидный статус должен отклоняться базой, а не только Java-кодом")
                .isFalse();
    }

    private boolean insertOutboxWithStatus(JdbcTemplate jdbc, String status) {
        try {
            jdbc.update("""
                    INSERT INTO market_place.outbox_messages
                        (id, aggregate_type, aggregate_id, event_type, payload, status)
                    VALUES (gen_random_uuid(), 'order', gen_random_uuid(), 'ORDER_CREATED', '{}'::jsonb, ?)
                    """, status);
            return true;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return false;
        }
    }
}
