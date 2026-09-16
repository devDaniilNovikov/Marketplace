package dn.marketplace.account.service;

import dn.marketplace.account.api.dto.AccountListResponse;
import dn.marketplace.account.api.dto.AccountMapResponse;
import dn.marketplace.account.api.dto.AccountRequest;
import dn.marketplace.account.api.dto.AccountResponse;
import dn.marketplace.account.api.enums.AccountStatus;

import java.util.List;
import java.util.UUID;

public interface AccountService {


    AccountListResponse findAll(int pageNumber,
                                int pageSize);

    AccountMapResponse findAllByStatus(AccountStatus status,
                                       int pageNumber,
                                       int pageSize);

    AccountResponse findById(UUID accountId);

    AccountResponse findByUsername(String username);

    AccountResponse findByEmail(String email);

    void createAccount(AccountRequest accountRequest);

    AccountMapResponse findAccountByStatus(AccountStatus accountStatus,
                                           List<AccountResponse> accountResponses);

    void deleteAccount(UUID accountId);

    void updateAccount(UUID accountId, AccountRequest accountRequest);
}
