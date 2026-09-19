package dn.marketplace.core.outbox;

import java.util.UUID;

/**
 * Запись события в {@code market_place.outbox_messages} в текущей транзакции.
 * Вызов вне транзакции запрещён: иначе событие потеряется при откате бизнес-изменения.
 */
public interface OutboxPublisher {

    void publish(String aggregateType, UUID aggregateId, String eventType, Object payload);
}
