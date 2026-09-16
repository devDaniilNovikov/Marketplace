package dn.marketplace.account.service;

import dn.marketplace.account.api.event.UserUpdatedEvent;
import dn.marketplace.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserUpdatedHandler {

    private final AccountRepository accountRepository;

    @Transactional
    @Retryable(retryFor = OptimisticLockingFailureException.class, backoff = @Backoff(delay = 50))
    public void handle(UserUpdatedEvent event) {
        accountRepository.findById(event.accountId()).ifPresent(account -> {
            if (account.isDeleted()) {
                log.debug("USER_UPDATED для удалённого аккаунта {} проигнорирован", event.accountId());
                return;
            }
            account.applyProfile(
                    event.username(),
                    event.email(),
                    event.firstName(),
                    event.lastName());
        });
    }
}
