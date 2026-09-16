package dn.marketplace.account.service;

import dn.marketplace.account.AccountFacade;
import dn.marketplace.account.api.enums.BusinessStatus;
import dn.marketplace.core.outbox.OutboxPublisher;
import dn.marketplace.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

/**
 * Атомарность перехода и outbox (спека §4): если publish бросил — откатывается и статус.
 * Отдельный класс: @MockitoBean OutboxPublisher меняет контекст, остальные IT живут на общем.
 */
class AccountServiceAtomicityIT extends AbstractIntegrationTest {

    @Autowired
    AccountService accountService;

    @Autowired
    AccountFacade accountFacade;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockitoBean
    OutboxPublisher outboxPublisher;

    @Test
    void падение_outbox_откатывает_переход() {
        UUID id = UUID.randomUUID();
        accountFacade.provision(id, "atomic");
        accountService.applyAsSeller(id);
        doThrow(new IllegalStateException("outbox недоступен"))
                .when(outboxPublisher).publish(eq("account"), eq(id), eq("SELLER_ROLE_GRANTED"), any());

        assertThatThrownBy(() -> accountService.approveSeller(id))
                .isInstanceOf(IllegalStateException.class);

        String status = jdbcTemplate.queryForObject(
                "SELECT business_status FROM market_place.accounts WHERE id = ?", String.class, id);
        assertThat(status).isEqualTo(BusinessStatus.SELLER_PENDING.name());
    }
}
