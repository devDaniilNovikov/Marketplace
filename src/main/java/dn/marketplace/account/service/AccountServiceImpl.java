package dn.marketplace.account.service;


import dn.marketplace.account.api.dto.AccountListResponse;
import dn.marketplace.account.api.dto.AccountMapResponse;
import dn.marketplace.account.api.dto.AccountResponse;
import dn.marketplace.account.api.enums.AccountStatus;
import dn.marketplace.account.api.exception.AccountNotFoundException;
import dn.marketplace.account.api.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;



    @Override
    public AccountListResponse findAll(int pageNumber, int pageSize) {
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);
        Page<AccountResponse> page = accountRepository.findAll(pageRequest)
                .map(accountMapper::toResponse);
        return AccountListResponse.builder()
                .accounts(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
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
        return accountRepository.findByUsername(username)
                .map(accountMapper::toResponse)
                .orElseThrow(()->new AccountNotFoundException(
                        MessageFormat.format("Account with username: {0} not found",username))
                );
    }
}
