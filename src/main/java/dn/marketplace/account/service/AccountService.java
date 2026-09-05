package dn.marketplace.account.service;

import dn.marketplace.account.api.AccountEntity;
import dn.marketplace.account.api.dto.AccountMapResponse;
import dn.marketplace.account.api.enums.AccountStatus;

import java.util.List;
import java.util.Map;

public interface AccountService {


    List<AccountEntity> findAll(int pageNumber, int pageSize);

    AccountMapResponse findAllByStatus(AccountStatus status,
                                       int pageNumber, int pageSize);
}
