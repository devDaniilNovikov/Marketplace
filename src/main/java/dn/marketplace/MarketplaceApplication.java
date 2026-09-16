package dn.marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@SpringBootApplication
@EnableRetry
@EnableScheduling
@EnableAsync
// @EnableCaching вернётся вместе с Redis CacheManager (фаза I, кэш каталога):
// без CacheManager контекст не стартует, а @Cacheable пока нигде нет
@EnableWebSocket
public class MarketplaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketplaceApplication.class, args);
    }

}
