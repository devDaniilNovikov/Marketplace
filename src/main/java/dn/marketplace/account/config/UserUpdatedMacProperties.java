package dn.marketplace.account.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "marketplace.keycloak")
public record UserUpdatedMacProperties(String eventsMacSecret, Duration eventsMacSkew) {

    public UserUpdatedMacProperties {
        if (eventsMacSecret == null || eventsMacSecret.isBlank()) {
            throw new IllegalStateException(
                    "marketplace.keycloak.events-mac-secret (KEYCLOAK_EVENTS_MAC_SECRET) не задан");
        }
        if (eventsMacSkew == null || eventsMacSkew.isNegative() || eventsMacSkew.isZero()) {
            eventsMacSkew = Duration.ofSeconds(30);
        }
    }
}
