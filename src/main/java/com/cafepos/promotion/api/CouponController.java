package com.cafepos.promotion.api;

import com.cafepos.promotion.application.ManageCouponUseCase;
import com.cafepos.shared.tenancy.TenantContextHolder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private final ManageCouponUseCase manageCouponUseCase;

    public CouponController(ManageCouponUseCase manageCouponUseCase) {
        this.manageCouponUseCase = Objects.requireNonNull(manageCouponUseCase, "manageCouponUseCase must not be null");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<List<CouponResponse>> getCoupons() {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        List<CouponResponse> coupons = manageCouponUseCase.getCouponsForCafe(cafeId);
        return ResponseEntity.ok(coupons);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<CouponResponse> getCouponById(@PathVariable("id") UUID id) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        CouponResponse coupon = manageCouponUseCase.getCouponById(cafeId, id);
        return ResponseEntity.ok(coupon);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        CouponResponse coupon = manageCouponUseCase.createCoupon(cafeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(coupon);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<CouponResponse> toggleCouponActiveStatus(
            @PathVariable("id") UUID id,
            @RequestParam("active") boolean active) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        CouponResponse coupon = manageCouponUseCase.toggleCouponActiveStatus(cafeId, id, active);
        return ResponseEntity.ok(coupon);
    }
}
