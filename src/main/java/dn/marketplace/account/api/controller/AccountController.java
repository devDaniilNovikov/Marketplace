package dn.marketplace.account.api.controller;

import dn.marketplace.account.api.dto.AccountListResponse;
import dn.marketplace.account.api.dto.AccountProfileResponse;
import dn.marketplace.account.api.dto.AccountResponse;
import dn.marketplace.account.api.dto.RejectSellerRequest;
import dn.marketplace.account.api.enums.BusinessStatus;
import dn.marketplace.account.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Validated
public class AccountController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AccountService accountService;

    @GetMapping("/by-status")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountListResponse findAllByStatus(
            @RequestParam BusinessStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int pageNumber,
            @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int pageSize) {
        return accountService.findAllByStatus(status, pageNumber, pageSize);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public AccountProfileResponse me(Authentication authentication) {
        return accountService.findMe(currentAccountId(authentication));
    }

    @GetMapping("/{accountId}")
    @PreAuthorize("hasRole('ADMIN') or #accountId.toString() == authentication.name")
    public AccountResponse findById(@PathVariable UUID accountId) {
        return accountService.findById(accountId);
    }

    @GetMapping("/by-username/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse findByUsername(@PathVariable String username) {
        return accountService.findByUsername(username);
    }

    @PostMapping("/me/seller-application")
    @PreAuthorize("isAuthenticated()")
    public AccountResponse applyAsSeller(Authentication authentication) {
        return accountService.applyAsSeller(currentAccountId(authentication));
    }

    @PostMapping("/{accountId}/seller-application/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse approveSeller(@PathVariable UUID accountId) {
        return accountService.approveSeller(accountId);
    }

    @PostMapping("/{accountId}/seller-application/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse rejectSeller(@PathVariable UUID accountId,
                                        @Valid @RequestBody RejectSellerRequest request) {
        return accountService.rejectSeller(accountId, request.reason());
    }

    @DeleteMapping("/{accountId}/seller-status")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse revokeSeller(@PathVariable UUID accountId) {
        return accountService.revokeSeller(accountId);
    }

    @PostMapping("/{accountId}/ban")
    @PreAuthorize("hasRole('ADMIN') and #accountId.toString() != authentication.name")
    public AccountResponse ban(@PathVariable UUID accountId) {
        return accountService.ban(accountId);
    }

    @DeleteMapping("/{accountId}/ban")
    @PreAuthorize("hasRole('ADMIN') and #accountId.toString() != authentication.name")
    public AccountResponse unban(@PathVariable UUID accountId) {
        return accountService.unban(accountId);
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe(Authentication authentication) {
        accountService.delete(currentAccountId(authentication));
    }

    @DeleteMapping("/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID accountId) {
        accountService.delete(accountId);
    }

    private static UUID currentAccountId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
