package dn.marketplace.account.api.dto;

import dn.marketplace.account.api.enums.AccountStatus;
import lombok.Builder;

import java.util.UUID;

@Builder
public record AccountResponse(UUID id, String username, AccountStatus status) {
}
