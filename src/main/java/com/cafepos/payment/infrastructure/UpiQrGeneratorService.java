package com.cafepos.payment.infrastructure;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

@Service
public class UpiQrGeneratorService {

    private static final int DEFAULT_QR_SIZE = 250;

    public String generateUpiUri(String payeeVpa, String payeeName, BigDecimal amount, String transactionNote) {
        if (payeeVpa == null || payeeVpa.isBlank()) {
            throw new IllegalArgumentException("Payee VPA must not be null or empty");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        String encodedPa = URLEncoder.encode(payeeVpa.trim(), StandardCharsets.UTF_8);
        String encodedPn = (payeeName != null && !payeeName.isBlank())
                ? URLEncoder.encode(payeeName.trim(), StandardCharsets.UTF_8)
                : "";
        String formattedAm = String.format(Locale.US, "%.2f", amount);
        String encodedTn = (transactionNote != null && !transactionNote.isBlank())
                ? URLEncoder.encode(transactionNote.trim(), StandardCharsets.UTF_8)
                : "";

        StringBuilder uriBuilder = new StringBuilder("upi://pay?");
        uriBuilder.append("pa=").append(encodedPa);
        if (!encodedPn.isEmpty()) {
            uriBuilder.append("&pn=").append(encodedPn);
        }
        uriBuilder.append("&am=").append(formattedAm);
        if (!encodedTn.isEmpty()) {
            uriBuilder.append("&tn=").append(encodedTn);
        }

        return uriBuilder.toString();
    }

    public byte[] generateQrCodeImage(String text, int width, int height) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("QR text must not be null or empty");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive integers");
        }

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Failed to generate QR code image", e);
        }
    }

    public String generateQrCodeBase64(String text, int width, int height) {
        byte[] imageBytes = generateQrCodeImage(text, width, height);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
    }

    public String generateQrCodeBase64(String text) {
        return generateQrCodeBase64(text, DEFAULT_QR_SIZE, DEFAULT_QR_SIZE);
    }
}
