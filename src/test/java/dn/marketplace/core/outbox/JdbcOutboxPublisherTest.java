package dn.marketplace.core.outbox;

import dn.marketplace.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcOutboxPublisherTest extends AbstractIntegrationTest {

    @Autowired
    OutboxPublisher outboxPublisher;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void вне_транзакции_бросает() {
        assertThatThrownBy(() -> outboxPublisher.publish(
                "account", UUID.randomUUID(), "SELLER_ROLE_GRANTED", Map.of("accountId", UUID.randomUUID())))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void в_транзакции_пишет_pending() {
        UUID accountId = UUID.randomUUID();
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                outboxPublisher.publish("account", accountId, "SELLER_ROLE_GRANTED", Map.of("accountId", accountId)));

        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT count(*) FROM market_place.outbox_messages
                        WHERE aggregate_id = ? AND event_type = 'SELLER_ROLE_GRANTED' AND status = 'PENDING'
                        """,
                Integer.class,
                accountId);
        assertThat(count).isOne();
    }
}
