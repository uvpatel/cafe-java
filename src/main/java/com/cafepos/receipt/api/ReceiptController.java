package com.cafepos.receipt.api;

import com.cafepos.receipt.application.ReceiptApplicationService;
import com.cafepos.shared.tenancy.TenantContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/receipts")
public class ReceiptController {

    private final ReceiptApplicationService receiptApplicationService;

    public ReceiptController(ReceiptApplicationService receiptApplicationService) {
        this.receiptApplicationService = Objects.requireNonNull(receiptApplicationService, "receiptApplicationService must not be null");
    }

    @PostMapping("/orders/{orderId}")
    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ReceiptResponse> generateReceipt(@PathVariable UUID orderId) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        ReceiptResponse response = receiptApplicationService.generateReceiptForOrder(cafeId, orderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER', 'OWNER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ReceiptResponse> getReceiptByOrderId(@PathVariable UUID orderId) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        ReceiptResponse response = receiptApplicationService.getReceiptByOrderId(cafeId, orderId);
        return ResponseEntity.ok(response);
    }
}
