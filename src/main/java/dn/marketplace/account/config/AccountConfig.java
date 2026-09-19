package dn.marketplace.account.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AccountSellerApplicationProperties.class)
public class AccountConfig {
}
