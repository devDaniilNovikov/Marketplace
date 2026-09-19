package dn.marketplace.account.service;

import dn.marketplace.account.api.dto.AccountListResponse;
import dn.marketplace.account.api.dto.AccountProfileResponse;
import dn.marketplace.account.api.dto.AccountResponse;
import dn.marketplace.account.api.enums.BusinessStatus;

import java.util.UUID;

public interface AccountService {

    AccountListResponse findAllByStatus(BusinessStatus status, int pageNumber, int pageSize);

    AccountResponse findById(UUID accountId);

    AccountProfileResponse findMe(UUID accountId);

    AccountResponse findByUsername(String username);

    AccountResponse applyAsSeller(UUID accountId);

    AccountResponse approveSeller(UUID accountId);

    AccountResponse rejectSeller(UUID accountId, String reason);

    AccountResponse revokeSeller(UUID accountId);

    AccountResponse ban(UUID accountId);

    AccountResponse unban(UUID accountId);

    void delete(UUID accountId);
}
