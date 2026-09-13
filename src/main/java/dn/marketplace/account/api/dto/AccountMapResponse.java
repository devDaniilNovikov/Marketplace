package dn.marketplace.account.api.dto;


import lombok.Builder;

import java.util.List;
import java.util.Map;


@Builder
public record AccountMapResponse(int pageSize,
                                 int pageNumber,
                                 long totalElements,
                                 int totalPages,
                                 boolean hasNext,
                                 Map<String, List<AccountResponse>> accounts) {
}
