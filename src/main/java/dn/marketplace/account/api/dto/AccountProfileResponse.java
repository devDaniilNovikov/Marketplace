package dn.marketplace.account.api.dto;

import dn.marketplace.account.api.enums.BusinessStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record AccountProfileResponse(
        UUID id,
        String username,
        BusinessStatus businessStatus,
        boolean banned,
        String rejectionReason,
        Instant sellerApplicationHoldUntil,
        boolean deleted,
        String emailSnapshot,
        String firstNameSnapshot,
        String lastNameSnapshot) {
}
