package com.cafepos.payment.api;

import com.cafepos.payment.application.PaymentApplicationService;
import com.cafepos.payment.domain.PaymentMethod;
import com.cafepos.payment.domain.PaymentStatus;
import com.cafepos.shared.error.BusinessException;
import com.cafepos.shared.error.GlobalExceptionHandler;
import com.cafepos.shared.security.UserPrincipal;
import com.cafepos.shared.tenancy.TenantContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PaymentApplicationService paymentApplicationService;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();
    private final UUID refundId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        PaymentController controller = new PaymentController(paymentApplicationService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        TenantContextHolder.setTenantId(cafeId.toString());
        UserPrincipal principal = new UserPrincipal(cafeId, employeeId, "cashier@cafe.com", "CASHIER");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_CASHIER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/v1/orders/{id}/payments/cash should process cash payment with 201 CREATED")
    void shouldProcessCashPayment() throws Exception {
        ProcessCashPaymentRequest request = new ProcessCashPaymentRequest(new BigDecimal("25.00"));
        PaymentResponse response = new PaymentResponse(
                paymentId, cafeId, orderId, PaymentMethod.CASH,
                new BigDecimal("20.00"), "USD", new BigDecimal("25.00"),
                new BigDecimal("5.00"), null, PaymentStatus.COMPLETED, Instant.now()
        );

        when(paymentApplicationService.processCashPayment(eq(cafeId), eq(orderId), any(ProcessCashPaymentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/orders/{id}/payments/cash", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.amount").value(20.00))
                .andExpect(jsonPath("$.tenderedAmount").value(25.00))
                .andExpect(jsonPath("$.changeAmount").value(5.00));

        verify(paymentApplicationService).processCashPayment(eq(cafeId), eq(orderId), any(ProcessCashPaymentRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{id}/payments/card should process card payment with 201 CREATED")
    void shouldProcessCardPayment() throws Exception {
        ProcessCardPaymentRequest request = new ProcessCardPaymentRequest("TXN-CARD-123");
        PaymentResponse response = new PaymentResponse(
                paymentId, cafeId, orderId, PaymentMethod.CARD,
                new BigDecimal("20.00"), "USD", null, null,
                "TXN-CARD-123", PaymentStatus.COMPLETED, Instant.now()
        );

        when(paymentApplicationService.processCardPayment(eq(cafeId), eq(orderId), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/orders/{id}/payments/card", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.transactionReference").value("TXN-CARD-123"));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{id}/payments/upi-intent should generate UPI intent with 200 OK")
    void shouldGenerateUpiIntent() throws Exception {
        UpiIntentResponse response = new UpiIntentResponse(
                "upi://pay?pa=cafe@upi&am=20.00",
                "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA",
                new BigDecimal("20.00"),
                "cafe@upi",
                "Odoo Cafe",
                "Order #ORD-1001"
        );

        when(paymentApplicationService.generateUpiIntent(cafeId, orderId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders/{id}/payments/upi-intent", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upiUri").value("upi://pay?pa=cafe@upi&am=20.00"))
                .andExpect(jsonPath("$.qrCodeBase64", org.hamcrest.Matchers.startsWith("data:image/png;base64,")))
                .andExpect(jsonPath("$.amount").value(20.00));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{id}/payments/upi-confirmation should confirm UPI payment with 201 CREATED")
    void shouldProcessUpiConfirmation() throws Exception {
        ProcessUpiConfirmationRequest request = new ProcessUpiConfirmationRequest("UTR-987654321");
        PaymentResponse response = new PaymentResponse(
                paymentId, cafeId, orderId, PaymentMethod.UPI_MANUAL,
                new BigDecimal("20.00"), "USD", null, null,
                "UTR-987654321", PaymentStatus.COMPLETED, Instant.now()
        );

        when(paymentApplicationService.processUpiConfirmation(eq(cafeId), eq(orderId), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/orders/{id}/payments/upi-confirmation", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.paymentMethod").value("UPI_MANUAL"))
                .andExpect(jsonPath("$.transactionReference").value("UTR-987654321"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/{id}/refunds should execute refund with 201 CREATED")
    void shouldExecuteRefund() throws Exception {
        RefundRequest request = new RefundRequest(new BigDecimal("20.00"), "Damaged item");
        RefundResponse response = new RefundResponse(
                refundId, cafeId, paymentId, new BigDecimal("20.00"),
                "USD", "Damaged item", employeeId, Instant.now()
        );

        when(paymentApplicationService.executeRefund(eq(cafeId), eq(paymentId), eq(employeeId), any(RefundRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/{id}/refunds", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(refundId.toString()))
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.amount").value(20.00))
                .andExpect(jsonPath("$.reason").value("Damaged item"))
                .andExpect(jsonPath("$.refundedByEmployeeId").value(employeeId.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{id}/payments/cash when insufficient tender should return 400 Bad Request")
    void shouldReturn400WhenTenderInsufficient() throws Exception {
        ProcessCashPaymentRequest request = new ProcessCashPaymentRequest(new BigDecimal("10.00"));

        when(paymentApplicationService.processCashPayment(eq(cafeId), eq(orderId), any(ProcessCashPaymentRequest.class)))
                .thenThrow(new BusinessException("INSUFFICIENT_TENDER", "Tendered amount is less than total amount", 400));

        mockMvc.perform(post("/api/v1/orders/{id}/payments/cash", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_TENDER"));
    }
}
