package com.cafepos.ordering.api;

import com.cafepos.promotion.api.ApplyCouponRequest;
import com.cafepos.promotion.api.PricingPreviewRequest;
import com.cafepos.promotion.api.PricingPreviewResponse;
import com.cafepos.promotion.application.ApplyCouponToOrderUseCase;
import com.cafepos.promotion.application.PricingPreviewUseCase;
import com.cafepos.promotion.application.RemoveCouponFromOrderUseCase;
import com.cafepos.shared.tenancy.TenantContextHolder;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderCouponController {

    private final ApplyCouponToOrderUseCase applyCouponToOrderUseCase;
    private final RemoveCouponFromOrderUseCase removeCouponFromOrderUseCase;
    private final PricingPreviewUseCase pricingPreviewUseCase;

    public OrderCouponController(ApplyCouponToOrderUseCase applyCouponToOrderUseCase,
                                 RemoveCouponFromOrderUseCase removeCouponFromOrderUseCase,
                                 PricingPreviewUseCase pricingPreviewUseCase) {
        this.applyCouponToOrderUseCase = Objects.requireNonNull(applyCouponToOrderUseCase, "applyCouponToOrderUseCase must not be null");
        this.removeCouponFromOrderUseCase = Objects.requireNonNull(removeCouponFromOrderUseCase, "removeCouponFromOrderUseCase must not be null");
        this.pricingPreviewUseCase = Objects.requireNonNull(pricingPreviewUseCase, "pricingPreviewUseCase must not be null");
    }

    @PostMapping("/{orderId}/coupon")
    @PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> applyCoupon(
            @PathVariable("orderId") UUID orderId,
            @Valid @RequestBody ApplyCouponRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        OrderResponse response = applyCouponToOrderUseCase.applyCouponToOrder(cafeId, orderId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{orderId}/coupon")
    @PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> removeCoupon(@PathVariable("orderId") UUID orderId) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        OrderResponse response = removeCouponFromOrderUseCase.removeCouponFromOrder(cafeId, orderId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orderId}/pricing-preview")
    @PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<PricingPreviewResponse> previewPricing(
            @PathVariable("orderId") UUID orderId,
            @RequestBody(required = false) PricingPreviewRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        PricingPreviewResponse response = pricingPreviewUseCase.previewPricing(cafeId, orderId, request);
        return ResponseEntity.ok(response);
    }
}
