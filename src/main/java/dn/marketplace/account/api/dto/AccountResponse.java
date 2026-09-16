package dn.marketplace.account.api.dto;

import dn.marketplace.account.api.enums.AccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record AccountResponse(UUID id,
                              String username,
                              AccountStatus status) {
}
