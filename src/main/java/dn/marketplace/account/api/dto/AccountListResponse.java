package dn.marketplace.account.api.dto;

import dn.marketplace.account.api.AccountEntity;
import lombok.Builder;

import java.util.List;

@Builder
public record AccountListResponse(List<AccountEntity> accounts) {
}
