package com.cafepos.payment.api;

import java.math.BigDecimal;

public record UpiIntentResponse(
        String upiUri,
        String qrCodeBase64,
        BigDecimal amount,
        String payeeVpa,
        String payeeName,
        String transactionNote
) {
}
