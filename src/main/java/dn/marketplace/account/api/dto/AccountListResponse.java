package dn.marketplace.account.api.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record AccountListResponse(int pageSize,
                                  int pageNumber,
                                  long totalElements,
                                  int totalPages,
                                  boolean hasNext,
                                  List<AccountResponse> accounts) {
}
