package com.cafepos.payment.api;

import com.cafepos.payment.domain.Payment;
import com.cafepos.payment.domain.Refund;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class PaymentDtoMapper {

    public PaymentResponse toPaymentResponse(Payment payment) {
        Objects.requireNonNull(payment, "payment must not be null");
        return new PaymentResponse(
                payment.getId(),
                payment.getCafeId(),
                payment.getOrderId(),
                payment.getPaymentMethod(),
                payment.getAmount().amount(),
                payment.getCurrency().getCurrencyCode(),
                payment.getTenderedAmount() != null ? payment.getTenderedAmount().amount() : null,
                payment.getChangeAmount() != null ? payment.getChangeAmount().amount() : null,
                payment.getTransactionReference(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }

    public RefundResponse toRefundResponse(Refund refund, String currency) {
        Objects.requireNonNull(refund, "refund must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        return new RefundResponse(
                refund.getId(),
                refund.getCafeId(),
                refund.getPaymentId(),
                refund.getAmount(),
                currency,
                refund.getReason(),
                refund.getRefundedByEmployeeId(),
                refund.getCreatedAt()
        );
    }
}
