package com.cafepos.payment.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpiQrGeneratorServiceTest {

    private UpiQrGeneratorService upiQrGeneratorService;

    @BeforeEach
    void setUp() {
        upiQrGeneratorService = new UpiQrGeneratorService();
    }

    @Test
    @DisplayName("Should generate sanitized UPI URI payload with 2 decimal places amount")
    void shouldGenerateSanitizedUpiUri() {
        String payeeVpa = "merchant@okaxis";
        String payeeName = "Odoo Cafe";
        BigDecimal amount = new BigDecimal("150.5");
        String transactionNote = "Order #ORD-1001";

        String upiUri = upiQrGeneratorService.generateUpiUri(payeeVpa, payeeName, amount, transactionNote);

        assertThat(upiUri)
                .startsWith("upi://pay?")
                .contains("pa=merchant%40okaxis")
                .contains("pn=Odoo+Cafe")
                .contains("am=150.50")
                .contains("tn=Order+%23ORD-1001");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when mandatory UPI URI parameters are invalid")
    void shouldThrowWhenGeneratingUpiUriWithInvalidInputs() {
        assertThatThrownBy(() -> upiQrGeneratorService.generateUpiUri(null, "Cafe", new BigDecimal("10.00"), "Note"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payee VPA must not be null or empty");

        assertThatThrownBy(() -> upiQrGeneratorService.generateUpiUri("", "Cafe", new BigDecimal("10.00"), "Note"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payee VPA must not be null or empty");

        assertThatThrownBy(() -> upiQrGeneratorService.generateUpiUri("payee@upi", "Cafe", null, "Note"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be greater than zero");

        assertThatThrownBy(() -> upiQrGeneratorService.generateUpiUri("payee@upi", "Cafe", BigDecimal.ZERO, "Note"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be greater than zero");
    }

    @Test
    @DisplayName("Should generate PNG byte array for valid QR text")
    void shouldGenerateQrCodePngByteArray() {
        String qrText = "upi://pay?pa=cafe@upi&am=100.00";

        byte[] qrBytes = upiQrGeneratorService.generateQrCodeImage(qrText, 200, 200);

        assertThat(qrBytes).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when QR code parameters are invalid")
    void shouldThrowWhenGeneratingQrCodeImageWithInvalidInputs() {
        assertThatThrownBy(() -> upiQrGeneratorService.generateQrCodeImage(null, 200, 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("QR text must not be null or empty");

        assertThatThrownBy(() -> upiQrGeneratorService.generateQrCodeImage("content", 0, 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Width and height must be positive integers");
    }

    @Test
    @DisplayName("Should generate Base64 data URI string for QR code")
    void shouldGenerateQrCodeBase64() {
        String qrText = "upi://pay?pa=cafe@upi&am=100.00";

        String base64DataUri = upiQrGeneratorService.generateQrCodeBase64(qrText);

        assertThat(base64DataUri)
                .startsWith("data:image/png;base64,")
                .isNotBlank();
    }
}
