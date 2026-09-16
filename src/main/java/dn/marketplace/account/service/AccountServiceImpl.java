package dn.marketplace.account.service;

import dn.marketplace.account.AccountOutboxEvents;
import dn.marketplace.account.api.dto.AccountListResponse;
import dn.marketplace.account.api.dto.AccountProfileResponse;
import dn.marketplace.account.api.dto.AccountResponse;
import dn.marketplace.account.api.enums.BusinessStatus;
import dn.marketplace.account.api.mapper.AccountMapper;
import dn.marketplace.account.config.AccountSellerApplicationProperties;
import dn.marketplace.account.entity.AccountEntity;
import dn.marketplace.account.repository.AccountRepository;
import dn.marketplace.core.exception.ResourceNotFoundException;
import dn.marketplace.core.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final OutboxPublisher outboxPublisher;
    private final Clock clock;
    private final AccountSellerApplicationProperties sellerApplicationProperties;

    @Override
    public AccountListResponse findAll(int pageNumber, int pageSize) {
        return toList(accountRepository.findAll(PageRequest.of(pageNumber, pageSize)));
    }

    @Override
    public AccountListResponse findAllByStatus(BusinessStatus status, int pageNumber, int pageSize) {
        return toList(accountRepository.findAllByBusinessStatusAndDeletedAtIsNull(
                status, PageRequest.of(pageNumber, pageSize)));
    }

    @Override
    public AccountResponse findById(UUID accountId) {
        return accountMapper.toResponse(get(accountId));
    }

    @Override
    public AccountProfileResponse findMe(UUID accountId) {
        return accountMapper.toProfile(get(accountId));
    }

    @Override
    public AccountResponse findByUsername(String username) {
        return accountRepository.findByUsernameAndDeletedAtIsNull(username)
                .map(accountMapper::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("Аккаунт", username));
    }

    @Override
    @Transactional
    public AccountResponse applyAsSeller(UUID accountId) {
        AccountEntity account = getActive(accountId);
        account.applyAsSeller(Instant.now(clock), sellerApplicationProperties.hold());
        return accountMapper.toResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse approveSeller(UUID accountId) {
        AccountEntity account = getActive(accountId);
        account.approveSeller();
        publish(accountId, AccountOutboxEvents.SELLER_ROLE_GRANTED);
        return accountMapper.toResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse rejectSeller(UUID accountId, String reason) {
        AccountEntity account = getActive(accountId);
        account.rejectSeller(reason);
        return accountMapper.toResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse revokeSeller(UUID accountId) {
        AccountEntity account = getActive(accountId);
        account.revokeSeller();
        publish(accountId, AccountOutboxEvents.SELLER_ROLE_REVOKED);
        return accountMapper.toResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse ban(UUID accountId) {
        AccountEntity account = getActive(accountId);
        account.ban();
        publish(accountId, AccountOutboxEvents.ACCOUNT_DISABLED);
        return accountMapper.toResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse unban(UUID accountId) {
        AccountEntity account = getActive(accountId);
        account.unban();
        publish(accountId, AccountOutboxEvents.ACCOUNT_ENABLED);
        return accountMapper.toResponse(account);
    }

    @Override
    @Transactional
    public void delete(UUID accountId) {
        AccountEntity account = getActive(accountId);
        account.delete(Instant.now(clock));
        publish(accountId, AccountOutboxEvents.ACCOUNT_DELETED);
    }

    private AccountListResponse toList(Page<AccountEntity> page) {
        return AccountListResponse.builder()
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .accounts(page.getContent().stream().map(accountMapper::toResponse).toList())
                .build();
    }

    private AccountEntity get(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> ResourceNotFoundException.of("Аккаунт", accountId));
    }

    private AccountEntity getActive(UUID accountId) {
        return accountRepository.findByIdAndDeletedAtIsNull(accountId)
                .orElseThrow(() -> ResourceNotFoundException.of("Аккаунт", accountId));
    }

    private void publish(UUID accountId, String eventType) {
        outboxPublisher.publish(
                AccountOutboxEvents.AGGREGATE,
                accountId,
                eventType,
                AccountOutboxEvents.payload(accountId));
    }
}
