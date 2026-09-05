package dn.marketplace.account.api.controller;


import dn.marketplace.account.api.AccountEntity;
import dn.marketplace.account.api.dto.AccountMapResponse;
import dn.marketplace.account.api.enums.AccountStatus;
import dn.marketplace.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AccountController {

    private static final String GET_ACCOUNTS_BY_STATUS = "/api/accounts/by-status";


    private final AccountService accountService;


    @GetMapping(GET_ACCOUNTS_BY_STATUS)
    public AccountMapResponse findAllByStatus(AccountStatus status,
                                              int pageNumber, int pageSize){
        return accountService.findAllByStatus(status,pageNumber,pageSize);
    }
}
