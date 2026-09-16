package dn.marketplace.account.service;

import dn.marketplace.account.AccountFacade;
import dn.marketplace.account.api.enums.BusinessStatus;
import dn.marketplace.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AccountServiceIT extends AbstractIntegrationTest {

    @Autowired
    AccountService accountService;

    @Autowired
    AccountFacade accountFacade;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void approveSeller_пишет_outbox() {
        UUID id = UUID.randomUUID();
        accountFacade.provision(id, "seller-candidate");
        accountService.applyAsSeller(id);
        accountService.approveSeller(id);

        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT count(*) FROM market_place.outbox_messages
                        WHERE aggregate_id = ? AND event_type = 'SELLER_ROLE_GRANTED' AND status = 'PENDING'
                        """,
                Integer.class,
                id);
        assertThat(count).isOne();
        assertThat(accountService.findById(id).businessStatus()).isEqualTo(BusinessStatus.SELLER);
    }

    @Test
    void rejectSeller_не_пишет_outbox() {
        UUID id = UUID.randomUUID();
        accountFacade.provision(id, "rejected-one");
        accountService.applyAsSeller(id);
        accountService.rejectSeller(id, "мало документов");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM market_place.outbox_messages WHERE aggregate_id = ?",
                Integer.class,
                id);
        assertThat(count).isZero();
    }

    @Test
    void jit_идемпотентен_при_гонке() throws Exception {
        UUID id = UUID.randomUUID();
        var pool = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        Runnable insert = () -> {
            try {
                start.await();
                accountFacade.provision(id, "racer");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        };
        pool.submit(insert);
        pool.submit(insert);
        start.countDown();
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        pool.close();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM market_place.accounts WHERE id = ?", Integer.class, id);
        assertThat(count).isOne();
    }

    @Test
    void удалённый_username_можно_переиспользовать() {
        UUID first = UUID.randomUUID();
        accountFacade.provision(first, "reusable");
        accountService.delete(first);

        UUID second = UUID.randomUUID();
        accountFacade.provision(second, "reusable");
        assertThat(accountService.findById(second).username()).isEqualTo("reusable");
    }
}
