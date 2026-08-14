package com.cafepos.payment.api;

import com.cafepos.payment.application.PaymentApplicationService;
import com.cafepos.shared.error.BusinessException;
import com.cafepos.shared.security.UserPrincipal;
import com.cafepos.shared.tenancy.TenantContextHolder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentApplicationService paymentApplicationService;

    public PaymentController(PaymentApplicationService paymentApplicationService) {
        this.paymentApplicationService = Objects.requireNonNull(paymentApplicationService, "paymentApplicationService must not be null");
    }

    @PostMapping("/orders/{id}/payments/cash")
    @PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<PaymentResponse> processCashPayment(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ProcessCashPaymentRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        PaymentResponse response = paymentApplicationService.processCashPayment(cafeId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/orders/{id}/payments/card")
    @PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<PaymentResponse> processCardPayment(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) ProcessCardPaymentRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        PaymentResponse response = paymentApplicationService.processCardPayment(cafeId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/orders/{id}/payments/upi-intent")
    @PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<UpiIntentResponse> generateUpiIntent(@PathVariable("id") UUID id) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        UpiIntentResponse response = paymentApplicationService.generateUpiIntent(cafeId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/orders/{id}/payments/upi-confirmation")
    @PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<PaymentResponse> processUpiConfirmation(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) ProcessUpiConfirmationRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        PaymentResponse response = paymentApplicationService.processUpiConfirmation(cafeId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/payments/{id}/refunds")
    @PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<RefundResponse> executeRefund(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RefundRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        UUID employeeId = getAuthenticatedEmployeeId();
        RefundResponse response = paymentApplicationService.executeRefund(cafeId, id, employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private UUID getAuthenticatedEmployeeId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getEmployeeId();
        }
        throw new BusinessException("UNAUTHORIZED", "Authenticated employee context is required", 401);
    }
}
