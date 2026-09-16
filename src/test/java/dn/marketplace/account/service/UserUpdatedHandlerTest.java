package dn.marketplace.account.service;

import dn.marketplace.account.api.event.UserUpdatedEvent;
import dn.marketplace.account.entity.AccountEntity;
import dn.marketplace.account.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserUpdatedHandlerTest {

    @Mock
    AccountRepository accountRepository;

    @InjectMocks
    UserUpdatedHandler handler;

    @Test
    void обновляет_снапшоты() {
        UUID id = UUID.randomUUID();
        AccountEntity entity = AccountEntity.create(id, "old");
        when(accountRepository.findById(id)).thenReturn(Optional.of(entity));

        handler.handle(new UserUpdatedEvent(id, "new", "n@e.e", "N", "E"));

        assertThat(entity.getUsername()).isEqualTo("new");
        assertThat(entity.getEmailSnapshot()).isEqualTo("n@e.e");
        assertThat(entity.getFirstNameSnapshot()).isEqualTo("N");
        assertThat(entity.getLastNameSnapshot()).isEqualTo("E");
    }

    @Test
    void удалённый_не_трогает() {
        UUID id = UUID.randomUUID();
        AccountEntity entity = AccountEntity.create(id, "old");
        entity.delete(Instant.parse("2026-09-16T00:00:00Z"));
        when(accountRepository.findById(id)).thenReturn(Optional.of(entity));

        handler.handle(new UserUpdatedEvent(id, "new", "n@e.e", "N", "E"));

        assertThat(entity.getUsername()).isEqualTo("old");
        assertThat(entity.getEmailSnapshot()).isNull();
    }
}
