package dn.marketplace.account.api.controller;


import dn.marketplace.account.api.dto.AccountMapResponse;
import dn.marketplace.account.api.dto.AccountRequest;
import dn.marketplace.account.api.dto.AccountResponse;
import dn.marketplace.account.api.enums.AccountStatus;
import dn.marketplace.account.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
public class AccountController {

    private static final String GET_ACCOUNTS_BY_STATUS = "/api/v1/accounts/by-status";
    private static final String GET_ACCOUNT_BY_ID = "/api/v1/accounts/{accountId}";
    private static final String GET_ACCOUNT_BY_USERNAME = "/api/v1/accounts/by-username/{username}";
    private static final String GET_ACCOUNT_BY_EMAIL = "/api/v1/accounts/by-email/{email}";
    private static final String CREATE_ACCOUNT = "/api/v1/accounts";
    private static final String UPDATE_ACCOUNT = "/api/v1/accounts/{accountId}";
    private static final String DELETE_ACCOUNT = "/api/v1/accounts/{accountId}";


    private static final int MAX_PAGE_SIZE = 100;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MIN_PAGE_NUMBER = 1;
    private static final String PAGE_SIZE = "10";
    private static final String PAGE_NUMBER = "1";



    private final AccountService accountService;


    /** Перечисление аккаунтов — служебная операция, доступна только администратору. */
    @GetMapping(GET_ACCOUNTS_BY_STATUS)
    @PreAuthorize("hasRole('ADMIN')")
    public AccountMapResponse findAllByStatus(@RequestParam AccountStatus status,
                                              @RequestParam(defaultValue = PAGE_NUMBER)
                                              @Min(MIN_PAGE_NUMBER) int pageNumber,
                                              @RequestParam(defaultValue = PAGE_SIZE)
                                              @Min(MIN_PAGE_SIZE)
                                              @Max(MAX_PAGE_SIZE) int pageSize){
        return accountService.findAllByStatus(status,pageNumber,pageSize);
    }

    @DeleteMapping(DELETE_ACCOUNT)
    @PreAuthorize("hasRole('ADMIN') or #accountId.toString() == authentication.name")
    public void deleteAccount(@PathVariable UUID accountId) {
        accountService.deleteAccount(accountId);
    }

    @PatchMapping(UPDATE_ACCOUNT)
    @PreAuthorize("hasRole('ADMIN') or #accountId.toString() == authentication.name")
    public void updateAccount(@PathVariable UUID accountId,
                              @Valid @RequestBody AccountRequest accountRequest) {
        accountService.updateAccount(accountId, accountRequest);
    }

    @GetMapping(GET_ACCOUNT_BY_EMAIL)
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse findAccountByEmail(@PathVariable String email) {
        return accountService.findByEmail(email);
    }

    @PostMapping(CREATE_ACCOUNT)
    public void createAccount(@Valid @RequestBody AccountRequest accountRequest){
        accountService.createAccount(accountRequest);
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
