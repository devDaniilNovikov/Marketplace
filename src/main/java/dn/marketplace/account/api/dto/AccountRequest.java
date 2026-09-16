package dn.marketplace.account.api.dto;

import lombok.Builder;

@Builder
public record AccountRequest(String username,
                             String email)
                             //TODO: password)
                             {
}
