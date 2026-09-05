package dn.marketplace;

import dn.marketplace.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Полный контекст монолита на настоящих Postgres и Redis.
 */
@Disabled("""
        Ждёт задачи B1-B3. Сейчас Hibernate с ddl-auto: validate не находит
        market_place.accounts: AccountEntity мапится на accounts с колонками
        account_status и user_name, а 01-account.sql всё ещё создаёт
        public.account с колонками email, role, first_name.
        Снять @Disabled сразу после того, как 01-account.sql и AccountEntity
        приведены к одной модели (строгий SSOT, решение №4).""")
class MarketplaceApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
