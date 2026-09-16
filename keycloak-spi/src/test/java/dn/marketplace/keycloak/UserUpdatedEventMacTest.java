package dn.marketplace.keycloak;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserUpdatedEventMacTest {

    static final String GOLDEN_CANONICAL =
            "{\"accountId\":\"11111111-1111-1111-1111-111111111111\","
                    + "\"username\":\"alice\","
                    + "\"email\":\"a@b.c\","
                    + "\"firstName\":\"A\","
                    + "\"lastName\":\"B\","
                    + "\"issuedAt\":1690000000000}";

    @Test
    void канон_совпадает_с_монолитом() {
        assertEquals(GOLDEN_CANONICAL, UserUpdatedEventMac.canonicalJson(
                "11111111-1111-1111-1111-111111111111",
                "alice",
                "a@b.c",
                "A",
                "B",
                1690000000000L));
    }

    @Test
    void своя_mac_принимается_чужая_нет() {
        String mac = UserUpdatedEventMac.macHex("shared-secret", GOLDEN_CANONICAL);
        assertTrue(UserUpdatedEventMac.macEqualsHex("shared-secret", GOLDEN_CANONICAL, mac));
        assertFalse(UserUpdatedEventMac.macEqualsHex("foreign-secret", GOLDEN_CANONICAL, mac));
        assertFalse(UserUpdatedEventMac.macEqualsHex("shared-secret", GOLDEN_CANONICAL, null));
    }
}
