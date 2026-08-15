package com.cafepos.customer.api;

import com.cafepos.customer.application.ConsentApplicationService;
import com.cafepos.shared.tenancy.TenantContextHolder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerConsentController {

    private final ConsentApplicationService consentApplicationService;

    public CustomerConsentController(ConsentApplicationService consentApplicationService) {
        this.consentApplicationService = Objects.requireNonNull(consentApplicationService, "consentApplicationService must not be null");
    }

    @PostMapping("/consents")
    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ConsentRecordResponse> grantConsent(@Valid @RequestBody GrantConsentRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        ConsentRecordResponse response = consentApplicationService.grantConsent(cafeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/consents/{consentId}/revoke")
    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ConsentRecordResponse> revokeConsent(@PathVariable UUID consentId) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        ConsentRecordResponse response = consentApplicationService.revokeConsent(cafeId, consentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{customerId}/consents")
    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER', 'OWNER', 'ADMIN')")
    public ResponseEntity<List<ConsentRecordResponse>> getCustomerConsents(@PathVariable UUID customerId) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        List<ConsentRecordResponse> responses = consentApplicationService.getCustomerConsents(cafeId, customerId);
        return ResponseEntity.ok(responses);
    }
}
