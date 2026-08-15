package com.cafepos.receipt.api;

import com.cafepos.receipt.domain.Receipt;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ReceiptDtoMapper {

    public ReceiptResponse toResponse(Receipt receipt) {
        Objects.requireNonNull(receipt, "receipt must not be null");
        return new ReceiptResponse(
                receipt.getId(),
                receipt.getCafeId(),
                receipt.getOrderId(),
                receipt.getReceiptNumber(),
                receipt.getSubtotalAmount(),
                receipt.getDiscountAmount(),
                receipt.getTaxAmount(),
                receipt.getServiceChargeAmount(),
                receipt.getTotalAmount(),
                receipt.getCurrency(),
                receipt.getTaxBreakdownJson(),
                receipt.getCreatedAt()
        );
    }
}
