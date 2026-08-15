package com.cafepos.customer.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GrantConsentRequest(
        @NotNull(message = "Customer ID is required")
        UUID customerId,

        @NotBlank(message = "Consent type is required")
        String consentType,

        String ipAddress
) {
}
