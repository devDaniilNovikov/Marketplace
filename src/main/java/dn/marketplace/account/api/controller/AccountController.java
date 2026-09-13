package dn.marketplace.account.api.controller;


import dn.marketplace.account.api.dto.AccountMapResponse;
import dn.marketplace.account.api.dto.AccountResponse;
import dn.marketplace.account.api.enums.AccountStatus;
import dn.marketplace.account.service.AccountService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
public class AccountController {

    private static final String GET_ACCOUNTS_BY_STATUS = "/api/v1/accounts/by-status";
    private static final String GET_ACCOUNT_BY_ID = "/api/v1/accounts/{accountId}";
    private static final String GET_ACCOUNT_BY_USERNAME = "/api/v1/accounts/by-username/{username}";

    // Верхняя граница страницы: защита от выгрузки всей таблицы одним запросом
    private static final int MAX_PAGE_SIZE = 100;


    private final AccountService accountService;


    /** Перечисление аккаунтов — служебная операция, доступна только администратору. */
    @GetMapping(GET_ACCOUNTS_BY_STATUS)
    @PreAuthorize("hasRole('ADMIN')")
    public AccountMapResponse findAllByStatus(@RequestParam AccountStatus status,
                                              @RequestParam(defaultValue = "0") @Min(0) int pageNumber,
                                              @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int pageSize){
        return accountService.findAllByStatus(status,pageNumber,pageSize);
    }

    /**
     * Читать можно только свой аккаунт: id аккаунта — это claim {@code sub} из JWT,
     * он же {@code authentication.name} (JIT-provisioning, сценарий 1). Администратор — любой.
     */
    @GetMapping(GET_ACCOUNT_BY_ID)
    @PreAuthorize("hasRole('ADMIN') or #accountId.toString() == authentication.name")
    public AccountResponse findById(@PathVariable UUID accountId){
        return accountService.findById(accountId);
    }

    /** Поиск по username — оракул существования чужих аккаунтов, поэтому только администратору. */
    @GetMapping(GET_ACCOUNT_BY_USERNAME)
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse findByUsername(@PathVariable String username){
        return accountService.findByUsername(username);
    }
}
