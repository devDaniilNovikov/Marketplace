package dn.marketplace.account.api.repository;

import dn.marketplace.account.api.AccountEntity;
import dn.marketplace.account.api.enums.AccountStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {

    List<AccountEntity> findAllByStatus(AccountStatus status, Pageable pageable);
}
