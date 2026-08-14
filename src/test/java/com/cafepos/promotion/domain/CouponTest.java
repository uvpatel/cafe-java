package com.cafepos.promotion.domain;

import com.cafepos.shared.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponTest {

    @Nested
    @DisplayName("Creation & Validation Tests")
    class CreationTests {

        @Test
        @DisplayName("Should create valid percentage coupon with normalized code")
        void shouldCreateValidPercentageCoupon() {
            UUID cafeId = UUID.randomUUID();
            Coupon coupon = new Coupon(cafeId, "  summer20  ", DiscountType.PERCENTAGE, BigDecimal.valueOf(20));

            assertThat(coupon.getId()).isNotNull();
            assertThat(coupon.getCafeId()).isEqualTo(cafeId);
            assertThat(coupon.getCode()).isEqualTo("SUMMER20");
            assertThat(coupon.getDiscountType()).isEqualTo(DiscountType.PERCENTAGE);
            assertThat(coupon.getDiscountValue()).isEqualByComparingTo(BigDecimal.valueOf(20));
            assertThat(coupon.getMinOrderAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(coupon.getMaxRedemptions()).isNull();
            assertThat(coupon.getTimesRedeemed()).isEqualTo(0);
            assertThat(coupon.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should create valid fixed amount coupon with all parameters")
        void shouldCreateValidFixedAmountCoupon() {
            UUID id = UUID.randomUUID();
            UUID cafeId = UUID.randomUUID();
            Instant startsAt = Instant.now().minus(1, ChronoUnit.DAYS);
            Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

            Coupon coupon = new Coupon(id, cafeId, "FLAT5", DiscountType.FIXED_AMOUNT,
                    BigDecimal.valueOf(5), BigDecimal.valueOf(15), 100, startsAt, expiresAt);

            assertThat(coupon.getId()).isEqualTo(id);
            assertThat(coupon.getCafeId()).isEqualTo(cafeId);
            assertThat(coupon.getCode()).isEqualTo("FLAT5");
            assertThat(coupon.getDiscountType()).isEqualTo(DiscountType.FIXED_AMOUNT);
            assertThat(coupon.getDiscountValue()).isEqualByComparingTo(BigDecimal.valueOf(5));
            assertThat(coupon.getMinOrderAmount()).isEqualByComparingTo(BigDecimal.valueOf(15));
            assertThat(coupon.getMaxRedemptions()).isEqualTo(100);
            assertThat(coupon.getStartsAt()).isEqualTo(startsAt);
            assertThat(coupon.getExpiresAt()).isEqualTo(expiresAt);
        }

        @Test
        @DisplayName("Should fail creation when cafeId is null")
        void shouldFailWhenCafeIdIsNull() {
            assertThatThrownBy(() -> new Coupon(null, "SAVE10", DiscountType.PERCENTAGE, BigDecimal.TEN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cafe ID cannot be null");
        }

        @Test
        @DisplayName("Should fail creation when code is null or empty")
        void shouldFailWhenCodeIsEmpty() {
            UUID cafeId = UUID.randomUUID();
            assertThatThrownBy(() -> new Coupon(cafeId, "  ", DiscountType.PERCENTAGE, BigDecimal.TEN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Coupon code cannot be null or empty");
        }

        @Test
        @DisplayName("Should fail creation when discountType is null")
        void shouldFailWhenDiscountTypeIsNull() {
            UUID cafeId = UUID.randomUUID();
            assertThatThrownBy(() -> new Coupon(UUID.randomUUID(), cafeId, "SAVE10", null, BigDecimal.TEN, BigDecimal.ZERO, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Discount type cannot be null");
        }

        @Test
        @DisplayName("Should fail creation when discountValue is zero or negative")
        void shouldFailWhenDiscountValueIsZeroOrNegative() {
            UUID cafeId = UUID.randomUUID();
            assertThatThrownBy(() -> new Coupon(cafeId, "SAVE10", DiscountType.PERCENTAGE, BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Discount value must be positive");

            assertThatThrownBy(() -> new Coupon(cafeId, "SAVE10", DiscountType.PERCENTAGE, BigDecimal.valueOf(-5)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Discount value must be positive");
        }

        @Test
        @DisplayName("Should fail creation when percentage discount exceeds 100%")
        void shouldFailWhenPercentageExceeds100() {
            UUID cafeId = UUID.randomUUID();
            assertThatThrownBy(() -> new Coupon(cafeId, "SAVE150", DiscountType.PERCENTAGE, BigDecimal.valueOf(101)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Percentage discount cannot exceed 100%");
        }

        @Test
        @DisplayName("Should fail creation when minOrderAmount is negative")
        void shouldFailWhenMinOrderAmountIsNegative() {
            UUID cafeId = UUID.randomUUID();
            assertThatThrownBy(() -> new Coupon(UUID.randomUUID(), cafeId, "SAVE10", DiscountType.FIXED_AMOUNT, BigDecimal.TEN, BigDecimal.valueOf(-1), null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Minimum order amount cannot be negative");
        }

        @Test
        @DisplayName("Should fail creation when maxRedemptions is non-positive")
        void shouldFailWhenMaxRedemptionsIsNonPositive() {
            UUID cafeId = UUID.randomUUID();
            assertThatThrownBy(() -> new Coupon(UUID.randomUUID(), cafeId, "SAVE10", DiscountType.FIXED_AMOUNT, BigDecimal.TEN, BigDecimal.ZERO, 0, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Max redemptions must be greater than zero");
        }

        @Test
        @DisplayName("Should fail creation when startsAt is after expiresAt")
        void shouldFailWhenStartsAtIsAfterExpiresAt() {
            UUID cafeId = UUID.randomUUID();
            Instant now = Instant.now();
            assertThatThrownBy(() -> new Coupon(UUID.randomUUID(), cafeId, "SAVE10", DiscountType.FIXED_AMOUNT, BigDecimal.TEN, BigDecimal.ZERO, 10, now.plus(2, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Starts at cannot be after expires at");
        }
    }

    @Nested
    @DisplayName("Redemption & State Tests")
    class RedemptionTests {

        @Test
        @DisplayName("Should track times redeemed and enforce max redemptions limit")
        void shouldTrackTimesRedeemed() {
            UUID cafeId = UUID.randomUUID();
            Coupon coupon = new Coupon(UUID.randomUUID(), cafeId, "LIMITED", DiscountType.FIXED_AMOUNT, BigDecimal.TEN, BigDecimal.ZERO, 2, null, null);

            assertThat(coupon.hasRemainingRedemptions()).isTrue();
            coupon.incrementTimesRedeemed();
            assertThat(coupon.getTimesRedeemed()).isEqualTo(1);
            assertThat(coupon.hasRemainingRedemptions()).isTrue();

            coupon.incrementTimesRedeemed();
            assertThat(coupon.getTimesRedeemed()).isEqualTo(2);
            assertThat(coupon.hasRemainingRedemptions()).isFalse();

            assertThatThrownBy(coupon::incrementTimesRedeemed)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("maximum redemption limit reached");
        }

        @Test
        @DisplayName("Should toggle active status with deactivate and activate")
        void shouldToggleActiveStatus() {
            UUID cafeId = UUID.randomUUID();
            Coupon coupon = new Coupon(cafeId, "ACTIVE", DiscountType.PERCENTAGE, BigDecimal.TEN);

            assertThat(coupon.isActive()).isTrue();
            coupon.deactivate();
            assertThat(coupon.isActive()).isFalse();
            coupon.activate();
            assertThat(coupon.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should validate time windows correctly")
        void shouldValidateTimeWindows() {
            UUID cafeId = UUID.randomUUID();
            Instant now = Instant.now();
            Instant past = now.minus(2, ChronoUnit.DAYS);
            Instant future = now.plus(2, ChronoUnit.DAYS);

            Coupon notStarted = new Coupon(UUID.randomUUID(), cafeId, "FUTURE", DiscountType.FIXED_AMOUNT, BigDecimal.TEN, BigDecimal.ZERO, null, future, future.plus(1, ChronoUnit.DAYS));
            Coupon expired = new Coupon(UUID.randomUUID(), cafeId, "EXPIRED", DiscountType.FIXED_AMOUNT, BigDecimal.TEN, BigDecimal.ZERO, null, past, past.plus(1, ChronoUnit.DAYS));
            Coupon activeWindow = new Coupon(UUID.randomUUID(), cafeId, "CURRENT", DiscountType.FIXED_AMOUNT, BigDecimal.TEN, BigDecimal.ZERO, null, past, future);

            assertThat(notStarted.isActiveAt(now)).isFalse();
            assertThat(expired.isActiveAt(now)).isFalse();
            assertThat(activeWindow.isActiveAt(now)).isTrue();
        }

        @Test
        @DisplayName("Should validate minimum order amount requirement")
        void shouldValidateMinimumOrder() {
            UUID cafeId = UUID.randomUUID();
            Coupon coupon = new Coupon(UUID.randomUUID(), cafeId, "MIN50", DiscountType.FIXED_AMOUNT, BigDecimal.TEN, BigDecimal.valueOf(50), null, null, null);

            assertThat(coupon.meetsMinimumOrder(Money.of(49.99))).isFalse();
            assertThat(coupon.meetsMinimumOrder(Money.of(50.00))).isTrue();
            assertThat(coupon.meetsMinimumOrder(Money.of(100.00))).isTrue();
            assertThat(coupon.meetsMinimumOrder(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Equals & HashCode")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should compare equality based on ID")
        void shouldCompareEqualityById() {
            UUID id = UUID.randomUUID();
            UUID cafeId = UUID.randomUUID();
            Coupon coupon1 = new Coupon(id, cafeId, "CODE1", DiscountType.PERCENTAGE, BigDecimal.TEN, BigDecimal.ZERO, null, null, null);
            Coupon coupon2 = new Coupon(id, cafeId, "CODE2", DiscountType.FIXED_AMOUNT, BigDecimal.ONE, BigDecimal.ZERO, null, null, null);
            Coupon coupon3 = new Coupon(UUID.randomUUID(), cafeId, "CODE1", DiscountType.PERCENTAGE, BigDecimal.TEN, BigDecimal.ZERO, null, null, null);

            assertThat(coupon1).isEqualTo(coupon2);
            assertThat(coupon1).isNotEqualTo(coupon3);
            assertThat(coupon1.hashCode()).isEqualTo(coupon2.hashCode());
        }
    }
}
