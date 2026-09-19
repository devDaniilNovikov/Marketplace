package dn.marketplace.account;

import dn.marketplace.account.entity.AccountEntity;
import dn.marketplace.account.repository.AccountRepository;
import dn.marketplace.core.security.AccountProvisioner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountFacade implements AccountProvisioner {

    private final AccountRepository accountRepository;

    public Optional<AccountView> find(UUID accountId) {
        return accountRepository.findById(accountId).map(this::toView);
    }

    @Override
    @Transactional
    public void provision(UUID accountId, String username) {
        accountRepository.insertIfAbsent(accountId, username);
    }

    private AccountView toView(AccountEntity entity) {
        return new AccountView(
                entity.getId(),
                entity.getUsername(),
                entity.getBusinessStatus(),
                entity.isBanned(),
                entity.isDeleted());
    }
}
