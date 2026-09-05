package dn.marketplace.account.service;


import dn.marketplace.account.api.AccountEntity;
import dn.marketplace.account.api.dto.AccountMapResponse;
import dn.marketplace.account.api.enums.AccountStatus;
import dn.marketplace.account.api.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    private final Map<UUID, AccountEntity> accountMap = new ConcurrentHashMap<>();



    @Override
    public List<AccountEntity> findAll(int pageNumber, int pageSize) {
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);
        return accountRepository.findAll(pageRequest).getContent();
    }

    @Override
    public AccountMapResponse findAllByStatus(AccountStatus status,
                                              int pageNumber,
                                              int pageSize) {
        Map<String,List<AccountEntity>> result = new ConcurrentHashMap<>();
        var pageable = PageRequest.of(pageNumber, pageSize);
        var accounts =  accountRepository.findAllByStatus(status,pageable);
        result.put(status.name(), accounts);
        return AccountMapResponse.builder()
                .accounts(result)
                .build();
    }
}
