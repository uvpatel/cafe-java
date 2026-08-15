package com.cafepos.receipt.api;


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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @DisplayName("POST /api/v1/receipts/orders/{orderId} should generate receipt with 201 CREATED")
    void shouldGenerateReceiptForPaidOrder() throws Exception {
        ReceiptResponse response = new ReceiptResponse(
                UUID.randomUUID(), cafeId, orderId, "RCP-20260815-ORD-1001",
                new BigDecimal("20.0000"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("20.0000"), "USD", "[]", Instant.now()
        );

        when(receiptApplicationService.generateReceiptForOrder(cafeId, orderId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/receipts/orders/{orderId}", orderId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.receiptNumber").value("RCP-20260815-ORD-1001"))
                .andExpect(jsonPath("$.totalAmount").value(20.0000));

        verify(receiptApplicationService).generateReceiptForOrder(cafeId, orderId);
    }

    @Test
    @DisplayName("GET /api/v1/receipts/orders/{orderId} should return receipt with 200 OK")
    void shouldGetReceiptByOrderId() throws Exception {
        ReceiptResponse response = new ReceiptResponse(
                UUID.randomUUID(), cafeId, orderId, "RCP-20260815-ORD-1001",
                new BigDecimal("20.0000"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("20.0000"), "USD", "[]", Instant.now()
        );

        when(receiptApplicationService.getReceiptByOrderId(cafeId, orderId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/receipts/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.receiptNumber").value("RCP-20260815-ORD-1001"));

        verify(receiptApplicationService).getReceiptByOrderId(cafeId, orderId);
    }

    @Test
    @DisplayName("GET /api/v1/receipts/orders/{orderId} when receipt does not exist should return 404 Not Found")
    void shouldReturn404WhenReceiptNotFound() throws Exception {
        when(receiptApplicationService.getReceiptByOrderId(cafeId, orderId))
                .thenThrow(new BusinessException("RECEIPT_NOT_FOUND", "Receipt not found for order id " + orderId, 404));

        mockMvc.perform(get("/api/v1/receipts/orders/{orderId}", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECEIPT_NOT_FOUND"));
    }
}
