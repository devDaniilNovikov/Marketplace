package dn.marketplace.account.service;

import dn.marketplace.account.api.dto.AccountListResponse;
import dn.marketplace.account.api.dto.AccountMapResponse;
import dn.marketplace.account.api.dto.AccountResponse;
import dn.marketplace.account.api.enums.AccountStatus;

import java.util.UUID;

public interface AccountService {


    AccountListResponse findAll(int pageNumber,
                                int pageSize);

    AccountMapResponse findAllByStatus(AccountStatus status,
                                       int pageNumber,
                                       int pageSize);

    AccountResponse findById(UUID accountId);

    AccountResponse findByUsername(String username);
}
