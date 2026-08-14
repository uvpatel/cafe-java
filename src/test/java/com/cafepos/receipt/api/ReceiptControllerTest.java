package com.cafepos.receipt.api;

import com.cafepos.ordering.domain.OrderStatus;
import com.cafepos.ordering.domain.OrderType;
import com.cafepos.payment.domain.PaymentMethod;
import com.cafepos.payment.domain.PaymentStatus;
import com.cafepos.receipt.application.ReceiptApplicationService;
import com.cafepos.shared.error.BusinessException;
import com.cafepos.shared.error.GlobalExceptionHandler;
import com.cafepos.shared.security.UserPrincipal;
import com.cafepos.shared.tenancy.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReceiptControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReceiptApplicationService receiptApplicationService;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID posSessionId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();
    private final UUID lineId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReceiptController controller = new ReceiptController(receiptApplicationService);

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
    @DisplayName("GET /api/v1/orders/{id}/receipt should return receipt snapshot with 200 OK")
    void shouldGetReceiptForPaidOrder() throws Exception {
        ReceiptResponse.ReceiptPaymentInfo paymentInfo = new ReceiptResponse.ReceiptPaymentInfo(
                paymentId, PaymentMethod.CASH, new BigDecimal("20.00"),
                new BigDecimal("25.00"), new BigDecimal("5.00"), null,
                PaymentStatus.COMPLETED, Instant.now()
        );

        ReceiptResponse.ReceiptLineResponse lineResponse = new ReceiptResponse.ReceiptLineResponse(
                lineId, productId, "Espresso", new BigDecimal("10.00"), 2,
                new BigDecimal("20.00"), new BigDecimal("20.00")
        );

        ReceiptResponse response = new ReceiptResponse(
                orderId, "ORD-1001", cafeId, posSessionId, null,
                OrderType.TAKEAWAY, OrderStatus.PAID, new BigDecimal("20.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("20.00"),
                "USD", "Extra hot", Instant.now(), Instant.now(),
                paymentInfo, List.of(lineResponse), List.of()
        );

        when(receiptApplicationService.getReceiptForOrder(cafeId, orderId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/{id}/receipt", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.orderNumber").value("ORD-1001"))
                .andExpect(jsonPath("$.orderStatus").value("PAID"))
                .andExpect(jsonPath("$.payment.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.payment.paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.payment.tenderedAmount").value(25.00))
                .andExpect(jsonPath("$.lines[0].productName").value("Espresso"));

        verify(receiptApplicationService).getReceiptForOrder(cafeId, orderId);
    }

    @Test
    @DisplayName("GET /api/v1/orders/{id}/receipt when order is not paid should return 400 Bad Request")
    void shouldReturn400WhenOrderNotPaid() throws Exception {
        when(receiptApplicationService.getReceiptForOrder(cafeId, orderId))
                .thenThrow(new BusinessException("ORDER_NOT_PAID", "Receipt can only be generated for paid orders", 400));

        mockMvc.perform(get("/api/v1/orders/{id}/receipt", orderId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_PAID"));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{id}/receipt when order does not exist should return 404 Not Found")
    void shouldReturn404WhenOrderNotFound() throws Exception {
        when(receiptApplicationService.getReceiptForOrder(cafeId, orderId))
                .thenThrow(new BusinessException("ORDER_NOT_FOUND", "Order not found with id " + orderId, 404));

        mockMvc.perform(get("/api/v1/orders/{id}/receipt", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }
}
