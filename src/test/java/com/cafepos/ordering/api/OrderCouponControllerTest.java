package com.cafepos.ordering.api;

import com.cafepos.ordering.domain.OrderStatus;
import com.cafepos.ordering.domain.OrderType;
import com.cafepos.promotion.api.ApplyCouponRequest;
import com.cafepos.promotion.api.PricingPreviewRequest;
import com.cafepos.promotion.api.PricingPreviewResponse;
import com.cafepos.promotion.application.ApplyCouponToOrderUseCase;
import com.cafepos.promotion.application.PricingPreviewUseCase;
import com.cafepos.promotion.application.RemoveCouponFromOrderUseCase;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderCouponControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ApplyCouponToOrderUseCase applyCouponToOrderUseCase;

    @Mock
    private RemoveCouponFromOrderUseCase removeCouponFromOrderUseCase;

    @Mock
    private PricingPreviewUseCase pricingPreviewUseCase;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID posSessionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        OrderCouponController controller = new OrderCouponController(
                applyCouponToOrderUseCase,
                removeCouponFromOrderUseCase,
                pricingPreviewUseCase
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        TenantContextHolder.setTenantId(cafeId.toString());
        UserPrincipal principal = new UserPrincipal(cafeId, UUID.randomUUID(), "cashier@cafe.com", "CASHIER");
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
    @DisplayName("POST /api/v1/orders/{orderId}/coupon should apply coupon to order")
    void shouldApplyCouponToOrder() throws Exception {
        ApplyCouponRequest request = new ApplyCouponRequest("SAVE10", null);

        OrderResponse orderResponse = new OrderResponse(
                orderId, cafeId, posSessionId, null, "ORD-001",
                OrderType.TAKEAWAY, OrderStatus.DRAFT,
                new BigDecimal("50.0000"), new BigDecimal("5.0000"),
                new BigDecimal("0.0000"), new BigDecimal("45.0000"),
                "USD", null, Instant.now(), Instant.now(), List.of()
        );

        when(applyCouponToOrderUseCase.applyCouponToOrder(eq(cafeId), eq(orderId), any(ApplyCouponRequest.class)))
                .thenReturn(orderResponse);

        mockMvc.perform(post("/api/v1/orders/{orderId}/coupon", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.discountAmount").value(5.0000))
                .andExpect(jsonPath("$.totalAmount").value(45.0000));

        verify(applyCouponToOrderUseCase).applyCouponToOrder(eq(cafeId), eq(orderId), any(ApplyCouponRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/orders/{orderId}/coupon should remove coupon from order")
    void shouldRemoveCouponFromOrder() throws Exception {
        OrderResponse orderResponse = new OrderResponse(
                orderId, cafeId, posSessionId, null, "ORD-001",
                OrderType.TAKEAWAY, OrderStatus.DRAFT,
                new BigDecimal("50.0000"), new BigDecimal("0.0000"),
                new BigDecimal("0.0000"), new BigDecimal("50.0000"),
                "USD", null, Instant.now(), Instant.now(), List.of()
        );

        when(removeCouponFromOrderUseCase.removeCouponFromOrder(cafeId, orderId)).thenReturn(orderResponse);

        mockMvc.perform(delete("/api/v1/orders/{orderId}/coupon", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.discountAmount").value(0.0000));

        verify(removeCouponFromOrderUseCase).removeCouponFromOrder(cafeId, orderId);
    }

    @Test
    @DisplayName("POST /api/v1/orders/{orderId}/pricing-preview should return pricing preview")
    void shouldPreviewPricing() throws Exception {
        PricingPreviewRequest request = new PricingPreviewRequest("FLAT5", null, null);
        PricingPreviewResponse response = new PricingPreviewResponse(
                new BigDecimal("40.0000"), new BigDecimal("0.0000"),
                new BigDecimal("5.0000"), new BigDecimal("35.0000"),
                "USD", "FLAT5", true
        );

        when(pricingPreviewUseCase.previewPricing(eq(cafeId), eq(orderId), any(PricingPreviewRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/orders/{orderId}/pricing-preview", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(40.0000))
                .andExpect(jsonPath("$.discountAmount").value(5.0000))
                .andExpect(jsonPath("$.totalAmount").value(35.0000))
                .andExpect(jsonPath("$.couponApplied").value(true));

        verify(pricingPreviewUseCase).previewPricing(eq(cafeId), eq(orderId), any(PricingPreviewRequest.class));
    }
}
