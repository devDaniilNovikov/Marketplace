package dn.marketplace.account;

import dn.marketplace.account.api.enums.BusinessStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountViewTest {

    @Test
    void canSell_только_живой_продавец() {
        UUID id = UUID.randomUUID();
        assertThat(new AccountView(id, "a", BusinessStatus.SELLER, false, false).canSell()).isTrue();
        assertThat(new AccountView(id, "a", BusinessStatus.SELLER, true, false).canSell()).isFalse();
        assertThat(new AccountView(id, "a", BusinessStatus.BUYER, false, false).canSell()).isFalse();
        assertThat(new AccountView(id, "a", BusinessStatus.SELLER, false, true).canTrade()).isFalse();
    }
}
