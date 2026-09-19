package dn.marketplace.account.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectSellerRequest(
        @NotBlank @Size(max = 1000) String reason) {
}
