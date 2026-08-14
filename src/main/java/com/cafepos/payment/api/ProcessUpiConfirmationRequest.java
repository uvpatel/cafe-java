package com.cafepos.payment.api;

public record ProcessUpiConfirmationRequest(
        String transactionReference
) {
}
