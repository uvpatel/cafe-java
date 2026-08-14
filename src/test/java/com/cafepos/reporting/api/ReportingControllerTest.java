package com.cafepos.reporting.api;

import com.cafepos.reporting.application.ReportingApplicationService;
import com.cafepos.reporting.application.SalesSummaryProjection;
import com.cafepos.reporting.application.TopProductProjection;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReportingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReportingApplicationService reportingApplicationService;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReportingController controller = new ReportingController(reportingApplicationService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        TenantContextHolder.setTenantId(cafeId.toString());
        UserPrincipal principal = new UserPrincipal(cafeId, employeeId, "manager@cafe.com", "MANAGER");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/reports/summary should return 200 OK with sales summary")
    void shouldReturnSalesSummary() throws Exception {
        SalesSummaryProjection proj = new SalesSummaryProjection(
                10,
                new BigDecimal("500.00"),
                new BigDecimal("50.00"),
                new BigDecimal("25.00"),
                new BigDecimal("475.00"),
                Map.of("CASH", new BigDecimal("200.00"), "CARD", new BigDecimal("275.00")),
                new BigDecimal("0.00")
        );

        when(reportingApplicationService.getSalesSummary(eq(cafeId), any(), any())).thenReturn(proj);

        mockMvc.perform(get("/api/v1/reports/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(10))
                .andExpect(jsonPath("$.grossSales").value(500.00))
                .andExpect(jsonPath("$.netRevenue").value(475.00))
                .andExpect(jsonPath("$.paymentMethodBreakdown.CASH").value(200.00));
    }

    @Test
    @DisplayName("GET /api/v1/reports/top-products should return 200 OK with top products list")
    void shouldReturnTopProducts() throws Exception {
        UUID p1 = UUID.randomUUID();
        TopProductProjection proj = new TopProductProjection(p1, "Capuccino", "Beverages", 15, new BigDecimal("60.00"));

        when(reportingApplicationService.getTopProducts(eq(cafeId), any(), any(), eq(10))).thenReturn(List.of(proj));

        mockMvc.perform(get("/api/v1/reports/top-products")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(p1.toString()))
                .andExpect(jsonPath("$[0].productName").value("Capuccino"))
                .andExpect(jsonPath("$[0].totalQuantitySold").value(15));
    }
}
