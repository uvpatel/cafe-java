package com.cafepos.loyalty.api;

import com.cafepos.loyalty.application.LoyaltyApplicationService;
import com.cafepos.loyalty.domain.LoyaltyAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LoyaltyControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LoyaltyApplicationService loyaltyApplicationService;

    private UUID cafeId;
    private UUID customerId;
    private LoyaltyAccount sampleAccount;

    @BeforeEach
    void setUp() {
        LoyaltyController controller = new LoyaltyController(loyaltyApplicationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.cafepos.shared.error.GlobalExceptionHandler())
                .build();

        cafeId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        sampleAccount = new LoyaltyAccount(UUID.randomUUID(), cafeId, customerId, 150, LoyaltyAccount.TierLevel.BRONZE);
    }

    @Test
    @DisplayName("GET /api/v1/cafes/{cafeId}/customers/{customerId}/loyalty should return account")
    void shouldGetLoyaltyAccount() throws Exception {
        when(loyaltyApplicationService.getOrCreateAccount(cafeId, customerId)).thenReturn(sampleAccount);

        mockMvc.perform(get("/api/v1/cafes/{cafeId}/customers/{customerId}/loyalty", cafeId, customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPoints").value(150))
                .andExpect(jsonPath("$.tierLevel").value("BRONZE"));
    }

    @Test
    @DisplayName("POST /api/v1/cafes/{cafeId}/customers/{customerId}/loyalty/earn should add points")
    void shouldEarnPoints() throws Exception {
        when(loyaltyApplicationService.earnPoints(eq(cafeId), eq(customerId), eq(50), any(), any()))
                .thenReturn(new LoyaltyAccount(sampleAccount.getId(), cafeId, customerId, 200, LoyaltyAccount.TierLevel.BRONZE));

        String requestJson = """
                {
                    "points": 50,
                    "description": "Coffee order bonus"
                }
                """;

        mockMvc.perform(post("/api/v1/cafes/{cafeId}/customers/{customerId}/loyalty/earn", cafeId, customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPoints").value(200));
    }

    @Test
    @DisplayName("POST /api/v1/cafes/{cafeId}/customers/{customerId}/loyalty/redeem should deduct points")
    void shouldRedeemPoints() throws Exception {
        when(loyaltyApplicationService.redeemPoints(eq(cafeId), eq(customerId), eq(100), any(), any()))
                .thenReturn(new LoyaltyAccount(sampleAccount.getId(), cafeId, customerId, 50, LoyaltyAccount.TierLevel.BRONZE));

        String requestJson = """
                {
                    "points": 100,
                    "description": "Free Drink Reward"
                }
                """;

        mockMvc.perform(post("/api/v1/cafes/{cafeId}/customers/{customerId}/loyalty/redeem", cafeId, customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPoints").value(50));
    }
}
