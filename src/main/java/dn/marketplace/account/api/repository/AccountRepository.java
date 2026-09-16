package dn.marketplace.account.api.repository;

import dn.marketplace.account.api.AccountEntity;
import dn.marketplace.account.api.dto.AccountResponse;
import dn.marketplace.account.api.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {

    Page<AccountEntity> findAllByStatus(AccountStatus status, Pageable pageable);

    Optional<AccountEntity> findByUsername(String username);

    Optional<AccountEntity> findByEmail(String email);
}
