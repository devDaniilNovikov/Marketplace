package dn.marketplace.account.repository;

import dn.marketplace.account.api.enums.BusinessStatus;
import dn.marketplace.account.entity.AccountEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {

    Optional<AccountEntity> findByIdAndDeletedAtIsNull(UUID id);

    Optional<AccountEntity> findByUsernameAndDeletedAtIsNull(String username);

    Page<AccountEntity> findAllByBusinessStatusAndDeletedAtIsNull(BusinessStatus status, Pageable pageable);

    @Modifying
    @Query(value = """
            INSERT INTO market_place.accounts (id, user_name)
            VALUES (:id, :username)
            ON CONFLICT (id) DO NOTHING
            """, nativeQuery = true)
    void insertIfAbsent(@Param("id") UUID id, @Param("username") String username);
}
