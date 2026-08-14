package com.cafepos.promotion.api;

import com.cafepos.promotion.application.ManageCouponUseCase;
import com.cafepos.promotion.domain.DiscountType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CouponControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ManageCouponUseCase manageCouponUseCase;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID couponId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        CouponController controller = new CouponController(manageCouponUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        TenantContextHolder.setTenantId(cafeId.toString());
        UserPrincipal principal = new UserPrincipal(cafeId, UUID.randomUUID(), "admin@cafe.com", "ADMIN");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/coupons should return list of coupons")
    void shouldGetCoupons() throws Exception {
        CouponResponse response = new CouponResponse(
                couponId, cafeId, "SUMMER20", DiscountType.PERCENTAGE,
                new BigDecimal("20.0000"), BigDecimal.ZERO, 100, 5, true,
                null, null, Instant.now(), Instant.now()
        );

        when(manageCouponUseCase.getCouponsForCafe(cafeId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/coupons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(couponId.toString()))
                .andExpect(jsonPath("$[0].code").value("SUMMER20"))
                .andExpect(jsonPath("$[0].discountType").value("PERCENTAGE"))
                .andExpect(jsonPath("$[0].discountValue").value(20.0000));

        verify(manageCouponUseCase).getCouponsForCafe(cafeId);
    }

    @Test
    @DisplayName("GET /api/v1/coupons/{id} should return single coupon")
    void shouldGetCouponById() throws Exception {
        CouponResponse response = new CouponResponse(
                couponId, cafeId, "SUMMER20", DiscountType.PERCENTAGE,
                new BigDecimal("20.0000"), BigDecimal.ZERO, 100, 5, true,
                null, null, Instant.now(), Instant.now()
        );

        when(manageCouponUseCase.getCouponById(cafeId, couponId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/coupons/{id}", couponId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(couponId.toString()))
                .andExpect(jsonPath("$.code").value("SUMMER20"));

        verify(manageCouponUseCase).getCouponById(cafeId, couponId);
    }

    @Test
    @DisplayName("POST /api/v1/coupons should create coupon and return 201 CREATED")
    void shouldCreateCoupon() throws Exception {
        CreateCouponRequest request = new CreateCouponRequest(
                "SUMMER20", DiscountType.PERCENTAGE, BigDecimal.valueOf(20), BigDecimal.ZERO, 100, null, null
        );

        CouponResponse response = new CouponResponse(
                couponId, cafeId, "SUMMER20", DiscountType.PERCENTAGE,
                new BigDecimal("20.0000"), BigDecimal.ZERO, 100, 0, true,
                null, null, Instant.now(), Instant.now()
        );

        when(manageCouponUseCase.createCoupon(eq(cafeId), any(CreateCouponRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(couponId.toString()))
                .andExpect(jsonPath("$.code").value("SUMMER20"));

        verify(manageCouponUseCase).createCoupon(eq(cafeId), any(CreateCouponRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/coupons with blank code should return 400 Bad Request")
    void shouldReturn400ForInvalidCreateCoupon() throws Exception {
        CreateCouponRequest request = new CreateCouponRequest(
                "", DiscountType.PERCENTAGE, BigDecimal.valueOf(20), BigDecimal.ZERO, 100, null, null
        );

        mockMvc.perform(post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/coupons/{id}/status should update coupon active status")
    void shouldToggleCouponStatus() throws Exception {
        CouponResponse response = new CouponResponse(
                couponId, cafeId, "SUMMER20", DiscountType.PERCENTAGE,
                new BigDecimal("20.0000"), BigDecimal.ZERO, 100, 0, false,
                null, null, Instant.now(), Instant.now()
        );

        when(manageCouponUseCase.toggleCouponActiveStatus(cafeId, couponId, false)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/coupons/{id}/status?active=false", couponId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(couponId.toString()))
                .andExpect(jsonPath("$.active").value(false));

        verify(manageCouponUseCase).toggleCouponActiveStatus(cafeId, couponId, false);
    }
}
