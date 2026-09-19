package dn.marketplace.account.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "marketplace.account.seller-application")
public record AccountSellerApplicationProperties(Duration hold) {

    public AccountSellerApplicationProperties {
        if (hold == null) {
            hold = Duration.ofMinutes(3);
        }
    }
}
