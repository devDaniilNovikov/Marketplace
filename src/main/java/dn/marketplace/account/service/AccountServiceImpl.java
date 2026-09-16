package dn.marketplace.account.service;


import dn.marketplace.account.api.AccountEntity;
import dn.marketplace.account.api.dto.AccountListResponse;
import dn.marketplace.account.api.dto.AccountMapResponse;
import dn.marketplace.account.api.dto.AccountRequest;
import dn.marketplace.account.api.dto.AccountResponse;
import dn.marketplace.account.api.enums.AccountStatus;
import dn.marketplace.account.api.exception.AccountNotFoundException;
import dn.marketplace.account.api.repository.AccountRepository;
import dn.marketplace.account.api.mapper.AccountMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;


import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {


    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final JsonMapper jsonMapper;



    @Override
    public AccountListResponse findAll(int pageNumber, int pageSize) {
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);
        Page<AccountResponse> page = accountRepository.findAll(pageRequest)
                .map(accountMapper::toResponse);
        return AccountListResponse.builder()
                .pageSize(pageSize)
                .pageNumber(pageNumber)
                .accounts(page.getContent())
                .build();
    }

    @Override
    public AccountMapResponse findAllByStatus(AccountStatus status,
                                              int pageNumber,
                                              int pageSize) {
        Map<String,List<AccountResponse>> result = new TreeMap<>();
        var pageable = PageRequest.of(pageNumber, pageSize);
        Page<AccountResponse> page = accountRepository.findAllByStatus(status,pageable)
                .map(accountMapper::toResponse);
        result.put(status.name(), page.getContent());
        return AccountMapResponse.builder()
                .accounts(result)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    @Override
    public AccountResponse findById(UUID accountId) {
        return accountRepository.findById(accountId)
                .map(accountMapper::toResponse)
                .orElseThrow(()->new AccountNotFoundException(
                        MessageFormat.format("Account with id: {0} not found",accountId))
                );
    }

    @Override
    public AccountResponse findByUsername(String username) {
        var account =  accountRepository.findByUsername(username)
                .map(accountMapper::toResponse)
                .orElseThrow(()->new AccountNotFoundException(
                        MessageFormat.format("Account with username: {0} not found",username))
                );
        String redisStringValue = jsonMapper.writeValueAsString(account);
        redisTemplate.opsForValue().set(account.id().toString(),redisStringValue);
        return account;
    }

    @Override
    public AccountResponse findByEmail(String email) {
        var accountByEmail = accountRepository.findByEmail(email)
                .map(accountMapper::toResponse)
                .orElseThrow(()->new AccountNotFoundException(
                        MessageFormat.format("Account with email: {0} not found",email)
                ));
        log.info("Account by email is find: {}",accountByEmail);
        return accountByEmail;


    }

    @Transactional
    public void createAccount(AccountRequest accountRequest) {
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setUsername(accountRequest.username());
        accountEntity.setEmail(accountRequest.email());
        accountEntity.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(accountEntity);
        log.info("Created account with id: {}", accountEntity.getId());
    }

    @Override
    public AccountMapResponse findAccountByStatus(AccountStatus accountStatus,
                                                 List<AccountResponse> accountResponses) {
        List<AccountResponse> filteredByStatusAccount = accountResponses.stream()
                .filter(account->account.status().equals(accountStatus))
                .toList();
        Map<String,List<AccountResponse>> result = filteredByStatusAccount.stream()
                .collect(Collectors.groupingBy(a->a.status().name()));
        result.forEach(log::info);
        return AccountMapResponse.builder()
                .accounts(result)
                .build();
    }

    @Override
    @Transactional
    public void deleteAccount(UUID accountId) {
        accountRepository.findById(accountId).ifPresent(accountRepository::delete);
        log.info("Deleted account with id: {}", accountId);
    }


    @Transactional
    @Override
    public void updateAccount(UUID accountId, AccountRequest accountRequest) {
        accountRepository.findById(accountId)
                .ifPresentOrElse(account->{
                    account.setUsername(accountRequest.username());
                    account.setEmail(accountRequest.email());
                    account.setStatus(AccountStatus.ACTIVE);
                    accountRepository.save(account);
                },()->{
                   throw  new AccountNotFoundException(MessageFormat.format("Account with id: {0} not found",accountId));
                });
    }
}
