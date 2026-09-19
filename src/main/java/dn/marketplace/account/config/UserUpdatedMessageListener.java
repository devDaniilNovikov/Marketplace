package dn.marketplace.account.config;

import dn.marketplace.account.service.UserUpdatedHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Адаптер Redis Pub/Sub: MAC проверяется здесь, HTTP JIT остаётся на JWT.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class UserUpdatedMessageListener implements MessageListener {

    private final JsonMapper jsonMapper;
    private final UserUpdatedEventAuthenticator authenticator;
    private final UserUpdatedHandler userUpdatedHandler;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            UserUpdatedWireEvent wire = jsonMapper.readValue(message.getBody(), UserUpdatedWireEvent.class);
            if (!authenticator.accept(wire)) {
                log.warn("USER_UPDATED отклонён: нет MAC, чужой MAC или issuedAt вне окна");
                return;
            }
            userUpdatedHandler.handle(wire.toDomain());
        } catch (RuntimeException e) {
            log.error("Не удалось обработать USER_UPDATED", e);
        }
    }
}
