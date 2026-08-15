package com.cafepos.loyalty.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoyaltyAccountTest {

    @Test
    @DisplayName("Should update points and recalculate tiers automatically")
    void earnPointsAndPromoteTier() {
        UUID cafeId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        LoyaltyAccount account = new LoyaltyAccount(cafeId, customerId);
        assertThat(account.getTierLevel()).isEqualTo(LoyaltyAccount.TierLevel.BRONZE);

        account.addPoints(600);
        assertThat(account.getCurrentPoints()).isEqualTo(600);
        assertThat(account.getTierLevel()).isEqualTo(LoyaltyAccount.TierLevel.SILVER);

        account.addPoints(1500);
        assertThat(account.getCurrentPoints()).isEqualTo(2100);
        assertThat(account.getTierLevel()).isEqualTo(LoyaltyAccount.TierLevel.GOLD);
    }

    @Test
    @DisplayName("Should prevent redeeming more points than available")
    void redeemPointsValidation() {
        UUID cafeId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        LoyaltyAccount account = new LoyaltyAccount(cafeId, customerId);
        account.addPoints(100);

        assertThatThrownBy(() -> account.redeemPoints(200))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient loyalty points");
    }
}
