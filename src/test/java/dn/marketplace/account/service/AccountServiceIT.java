package dn.marketplace.account.service;

import dn.marketplace.account.AccountFacade;
import dn.marketplace.account.api.enums.BusinessStatus;
import dn.marketplace.account.entity.AccountEntity;
import dn.marketplace.account.repository.AccountRepository;
import dn.marketplace.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AccountServiceIT extends AbstractIntegrationTest {

    @Autowired
    AccountService accountService;

    @Autowired
    AccountFacade accountFacade;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    TransactionTemplate transactionTemplate;

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

    /**
     * Два параллельных approveSeller на одной версии строки: оба читают version=0, барьер
     * гарантирует, что ни один не закоммитил раньше чтения другого. Победитель обновляет
     * строку, проигравший получает OptimisticLockingFailureException (→ 409 в GlobalExceptionHandler).
     */
    @Test
    void version_гонка_даёт_ровно_один_optimistic_lock() throws Exception {
        UUID id = UUID.randomUUID();
        accountFacade.provision(id, "versioned");
        accountService.applyAsSeller(id);

        CyclicBarrier bothLoaded = new CyclicBarrier(2);
        Callable<Throwable> approve = () -> {
            try {
                transactionTemplate.executeWithoutResult(tx -> {
                    AccountEntity account = accountRepository.findById(id).orElseThrow();
                    await(bothLoaded);
                    account.approveSeller();
                });
                return null;
            } catch (Throwable t) {
                return t;
            }
        };

        List<Throwable> outcomes;
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Throwable>> futures = pool.invokeAll(List.of(approve, approve));
            outcomes = new ArrayList<>();
            for (Future<Throwable> f : futures) {
                outcomes.add(f.get(10, TimeUnit.SECONDS));
            }
        }

        assertThat(outcomes).filteredOn(Objects::isNull).as("ровно один коммит прошёл").hasSize(1);
        assertThat(outcomes).filteredOn(Objects::nonNull)
                .as("второй получил именно OptimisticLockingFailureException")
                .hasSize(1)
                .allSatisfy(t -> assertThat(t).isInstanceOf(OptimisticLockingFailureException.class));
        assertThat(accountService.findById(id).businessStatus()).isEqualTo(BusinessStatus.SELLER);
        Long version = jdbcTemplate.queryForObject(
                "SELECT version FROM market_place.accounts WHERE id = ?", Long.class, id);
        assertThat(version).isEqualTo(2L); // applyAsSeller + один approveSeller
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("барьер не дождался второго потока", e);
        }
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
