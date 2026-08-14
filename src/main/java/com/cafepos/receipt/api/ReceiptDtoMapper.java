package com.cafepos.receipt.api;

import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderLine;
import com.cafepos.ordering.domain.OrderTaxLine;
import com.cafepos.payment.domain.Payment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Component
public class ReceiptDtoMapper {

    public ReceiptResponse toReceiptResponse(Order order, Payment payment) {
        Objects.requireNonNull(order, "order must not be null");
        Objects.requireNonNull(payment, "payment must not be null");

        ReceiptResponse.ReceiptPaymentInfo paymentInfo = new ReceiptResponse.ReceiptPaymentInfo(
                payment.getId(),
                payment.getPaymentMethod(),
                payment.getAmount().amount(),
                payment.getTenderedAmount() != null ? payment.getTenderedAmount().amount() : null,
                payment.getChangeAmount() != null ? payment.getChangeAmount().amount() : null,
                payment.getTransactionReference(),
                payment.getStatus(),
                payment.getCreatedAt()
        );

        List<ReceiptResponse.ReceiptLineResponse> lineResponses = order.getLines().stream()
                .map(this::toReceiptLineResponse)
                .toList();

        List<ReceiptResponse.ReceiptTaxLineResponse> taxLineResponses = order.getTaxLines().stream()
                .map(this::toReceiptTaxLineResponse)
                .toList();

        return new ReceiptResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCafeId(),
                order.getPosSessionId(),
                order.getDiningTableId(),
                order.getOrderType(),
                order.getStatus(),
                order.getSubtotal().amount(),
                order.getDiscount().amount(),
                order.getTax().amount(),
                order.getTotal().amount(),
                order.getCurrency().getCurrencyCode(),
                order.getNotes(),
                order.getCreatedAt(),
                Instant.now(),
                paymentInfo,
                lineResponses,
                taxLineResponses
        );
    }

    private ReceiptResponse.ReceiptLineResponse toReceiptLineResponse(OrderLine line) {
        return new ReceiptResponse.ReceiptLineResponse(
                line.getId(),
                line.getProductId(),
                line.getProductNameSnapshot(),
                line.getUnitPriceSnapshotAmount(),
                line.getQuantity(),
                line.getLineSubtotalAmount(),
                line.getLineTotalAmount()
        );
    }

    private ReceiptResponse.ReceiptTaxLineResponse toReceiptTaxLineResponse(OrderTaxLine taxLine) {
        return new ReceiptResponse.ReceiptTaxLineResponse(
                taxLine.getId(),
                taxLine.getTaxNameSnapshot(),
                taxLine.getRatePercent(),
                taxLine.getTaxAmount()
        );
    }
}
