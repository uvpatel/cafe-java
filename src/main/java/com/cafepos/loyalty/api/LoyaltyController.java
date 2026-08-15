package com.cafepos.loyalty.api;

import com.cafepos.loyalty.application.LoyaltyApplicationService;
import com.cafepos.loyalty.domain.LoyaltyAccount;
import com.cafepos.loyalty.domain.LoyaltyTransaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cafes/{cafeId}/customers/{customerId}/loyalty")
public class LoyaltyController {

    private final LoyaltyApplicationService loyaltyApplicationService;

    public LoyaltyController(LoyaltyApplicationService loyaltyApplicationService) {
        this.loyaltyApplicationService = loyaltyApplicationService;
    }

    public record PointsTransactionRequest(
            @NotNull @Min(1) Integer points,
            UUID referenceOrderId,
            String description
    ) {}

    @GetMapping
    public ResponseEntity<LoyaltyAccount> getLoyaltyAccount(@PathVariable UUID cafeId, @PathVariable UUID customerId) {
        return ResponseEntity.ok(loyaltyApplicationService.getOrCreateAccount(cafeId, customerId));
    }

    @PostMapping("/earn")
    public ResponseEntity<LoyaltyAccount> earnPoints(@PathVariable UUID cafeId, @PathVariable UUID customerId, @Valid @RequestBody PointsTransactionRequest request) {
        return ResponseEntity.ok(loyaltyApplicationService.earnPoints(cafeId, customerId, request.points(), request.referenceOrderId(), request.description()));
    }

    @PostMapping("/redeem")
    public ResponseEntity<LoyaltyAccount> redeemPoints(@PathVariable UUID cafeId, @PathVariable UUID customerId, @Valid @RequestBody PointsTransactionRequest request) {
        return ResponseEntity.ok(loyaltyApplicationService.redeemPoints(cafeId, customerId, request.points(), request.referenceOrderId(), request.description()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<LoyaltyTransaction>> getTransactionHistory(@PathVariable UUID cafeId, @PathVariable UUID customerId) {
        return ResponseEntity.ok(loyaltyApplicationService.getTransactionHistory(cafeId, customerId));
    }
}
