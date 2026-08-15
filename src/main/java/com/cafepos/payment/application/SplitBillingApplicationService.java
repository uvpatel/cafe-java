package com.cafepos.payment.application;

import com.cafepos.payment.domain.SplitPayment;
import com.cafepos.payment.domain.SplitPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SplitBillingApplicationService {

    private final SplitPaymentRepository splitPaymentRepository;

    public SplitBillingApplicationService(SplitPaymentRepository splitPaymentRepository) {
        this.splitPaymentRepository = splitPaymentRepository;
    }

    public record IndividualSplitRequest(
            UUID paymentMethodId,
            BigDecimal amount,
            BigDecimal tipAmount,
            String payerName
    ) {}

    public List<SplitPayment> processEqualSplit(UUID cafeId, UUID orderId, int totalSplits, UUID paymentMethodId, BigDecimal totalOrderAmount) {
        if (totalSplits <= 1) throw new IllegalArgumentException("Total splits must be greater than 1");
        if (totalOrderAmount == null || totalOrderAmount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Total order amount must be positive");

        BigDecimal splitAmount = totalOrderAmount.divide(new BigDecimal(totalSplits), 4, BigDecimal.ROUND_HALF_UP);
        List<SplitPayment> splits = new ArrayList<>();

        for (int i = 1; i <= totalSplits; i++) {
            SplitPayment split = new SplitPayment(cafeId, orderId, i, paymentMethodId, splitAmount, BigDecimal.ZERO, "Guest " + i);
            splits.add(splitPaymentRepository.save(split));
        }

        return splits;
    }

    public List<SplitPayment> processCustomSplits(UUID cafeId, UUID orderId, List<IndividualSplitRequest> splitRequests) {
        if (splitRequests == null || splitRequests.isEmpty()) throw new IllegalArgumentException("Split requests cannot be empty");

        List<SplitPayment> splits = new ArrayList<>();
        int index = 1;

        for (IndividualSplitRequest req : splitRequests) {
            BigDecimal tip = req.tipAmount() != null ? req.tipAmount() : BigDecimal.ZERO;
            SplitPayment split = new SplitPayment(cafeId, orderId, index++, req.paymentMethodId(), req.amount(), tip, req.payerName());
            splits.add(splitPaymentRepository.save(split));
        }

        return splits;
    }

    @Transactional(readOnly = true)
    public List<SplitPayment> getSplitsForOrder(UUID cafeId, UUID orderId) {
        return splitPaymentRepository.findByCafeIdAndOrderIdOrderBySplitIndexAsc(cafeId, orderId);
    }
}
