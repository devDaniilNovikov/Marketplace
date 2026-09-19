package dn.marketplace.core.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JdbcOutboxPublisher implements OutboxPublisher {

    private final JdbcClient jdbcClient;
    private final JsonMapper jsonMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String aggregateType, UUID aggregateId, String eventType, Object payload) {
        String json = jsonMapper.writeValueAsString(payload);
        jdbcClient.sql("""
                        INSERT INTO market_place.outbox_messages
                            (id, aggregate_type, aggregate_id, event_type, payload)
                        VALUES (:id, :aggregateType, :aggregateId, :eventType, CAST(:payload AS jsonb))
                        """)
                .param("id", UUID.randomUUID())
                .param("aggregateType", aggregateType)
                .param("aggregateId", aggregateId)
                .param("eventType", eventType)
                .param("payload", json)
                .update();
    }
}
