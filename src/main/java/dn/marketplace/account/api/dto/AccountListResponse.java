package dn.marketplace.account.api.dto;

import dn.marketplace.account.api.mapper.AccountMapper;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;

@Builder

public record AccountListResponse(int pageSize,
                                  int pageNumber,
                                  int totalElements,
                                  int totalPages,
                                  boolean hasNextList,
                                  List<AccountResponse> accounts) {
}
