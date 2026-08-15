package com.cafepos.payment.api;

import com.cafepos.payment.application.SplitBillingApplicationService;
import com.cafepos.payment.domain.SplitPayment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cafes/{cafeId}/orders/{orderId}/splits")
public class SplitPaymentController {

    private final SplitBillingApplicationService splitBillingApplicationService;

    public SplitPaymentController(SplitBillingApplicationService splitBillingApplicationService) {
        this.splitBillingApplicationService = splitBillingApplicationService;
    }

    public record EqualSplitRequest(
            @NotNull @Min(2) Integer totalSplits,
            @NotNull UUID paymentMethodId,
            @NotNull @DecimalMin("0.01") BigDecimal totalOrderAmount
    ) {}

    public record CustomSplitRequest(
            @NotEmpty List<SplitBillingApplicationService.IndividualSplitRequest> splits
    ) {}

    @PostMapping("/equal")
    public ResponseEntity<List<SplitPayment>> processEqualSplit(@PathVariable UUID cafeId, @PathVariable UUID orderId, @Valid @RequestBody EqualSplitRequest request) {
        List<SplitPayment> splits = splitBillingApplicationService.processEqualSplit(
                cafeId, orderId, request.totalSplits(), request.paymentMethodId(), request.totalOrderAmount()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(splits);
    }

    @PostMapping("/custom")
    public ResponseEntity<List<SplitPayment>> processCustomSplits(@PathVariable UUID cafeId, @PathVariable UUID orderId, @Valid @RequestBody CustomSplitRequest request) {
        List<SplitPayment> splits = splitBillingApplicationService.processCustomSplits(cafeId, orderId, request.splits());
        return ResponseEntity.status(HttpStatus.CREATED).body(splits);
    }

    @GetMapping
    public ResponseEntity<List<SplitPayment>> getSplits(@PathVariable UUID cafeId, @PathVariable UUID orderId) {
        return ResponseEntity.ok(splitBillingApplicationService.getSplitsForOrder(cafeId, orderId));
    }
}
