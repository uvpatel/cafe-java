package com.cafepos.payment.api;

public record ProcessCardPaymentRequest(
        String transactionReference
) {
}
