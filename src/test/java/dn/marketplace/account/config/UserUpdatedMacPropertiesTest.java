package dn.marketplace.account.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserUpdatedMacPropertiesTest {

    @Test
    void пустой_секрет_запрещён() {
        assertThatThrownBy(() -> new UserUpdatedMacProperties(" ", Duration.ofSeconds(30)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("KEYCLOAK_EVENTS_MAC_SECRET");
    }

    @Test
    void нулевой_skew_становится_30s() {
        assertThat(new UserUpdatedMacProperties("secret", Duration.ZERO).eventsMacSkew())
                .isEqualTo(Duration.ofSeconds(30));
    }
}
