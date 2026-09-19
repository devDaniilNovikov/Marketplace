package dn.marketplace.account.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserUpdatedEventMacTest {

    static final String GOLDEN_CANONICAL =
            "{\"accountId\":\"11111111-1111-1111-1111-111111111111\","
                    + "\"username\":\"alice\","
                    + "\"email\":\"a@b.c\","
                    + "\"firstName\":\"A\","
                    + "\"lastName\":\"B\","
                    + "\"issuedAt\":1690000000000}";

    @Test
    void канон_фиксированный_порядок_полей() {
        assertThat(UserUpdatedEventMac.canonicalJson(
                "11111111-1111-1111-1111-111111111111",
                "alice",
                "a@b.c",
                "A",
                "B",
                1690000000000L)).isEqualTo(GOLDEN_CANONICAL);
    }

    @Test
    void null_строки_в_каноне_пустые() {
        assertThat(UserUpdatedEventMac.canonicalJson("id", null, null, null, null, 1L))
                .isEqualTo("{\"accountId\":\"id\",\"username\":\"\",\"email\":\"\",\"firstName\":\"\",\"lastName\":\"\",\"issuedAt\":1}");
    }

    @Test
    void своя_mac_принимается_чужая_нет() {
        String canonical = GOLDEN_CANONICAL;
        String mac = UserUpdatedEventMac.macHex("shared-secret", canonical);
        assertThat(UserUpdatedEventMac.macEqualsHex("shared-secret", canonical, mac)).isTrue();
        assertThat(UserUpdatedEventMac.macEqualsHex("foreign-secret", canonical, mac)).isFalse();
        assertThat(UserUpdatedEventMac.macEqualsHex("shared-secret", canonical, null)).isFalse();
        assertThat(UserUpdatedEventMac.macEqualsHex("shared-secret", canonical, "")).isFalse();
        assertThat(UserUpdatedEventMac.macEqualsHex("shared-secret", canonical, "zz")).isFalse();
    }
}
