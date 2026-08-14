package com.cafepos.receipt.api;

import com.cafepos.receipt.application.ReceiptApplicationService;
import com.cafepos.shared.tenancy.TenantContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class ReceiptController {

    private final ReceiptApplicationService receiptApplicationService;

    public ReceiptController(ReceiptApplicationService receiptApplicationService) {
        this.receiptApplicationService = Objects.requireNonNull(receiptApplicationService, "receiptApplicationService must not be null");
    }

    @GetMapping("/{id}/receipt")
    @PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN', 'MANAGER', 'KITCHEN_STAFF')")
    public ResponseEntity<ReceiptResponse> getReceipt(@PathVariable("id") UUID id) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        ReceiptResponse response = receiptApplicationService.getReceiptForOrder(cafeId, id);
        return ResponseEntity.ok(response);
    }
}
