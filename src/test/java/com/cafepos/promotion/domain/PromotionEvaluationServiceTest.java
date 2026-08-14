package com.cafepos.promotion.domain;

import com.cafepos.shared.error.BusinessException;
import com.cafepos.shared.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromotionEvaluationServiceTest {

    private PromotionEvaluationService evaluationService;

    @BeforeEach
    void setUp() {
        evaluationService = new PromotionEvaluationService();
    }

    @Nested
    @DisplayName("Code Normalization Tests")
    class CodeNormalizationTests {

        @Test
        @DisplayName("Should trim and uppercase valid raw code")
        void shouldNormalizeValidCode() {
            assertThat(evaluationService.normalizeCode("  save20  ")).isEqualTo("SAVE20");
            assertThat(evaluationService.normalizeCode("welcome")).isEqualTo("WELCOME");
        }

        @Test
        @DisplayName("Should throw BusinessException when raw code is empty or null")
        void shouldThrowExceptionForEmptyCode() {
            assertThatThrownBy(() -> evaluationService.normalizeCode("   "))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("INVALID_COUPON_CODE");

            assertThatThrownBy(() -> evaluationService.normalizeCode(null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("INVALID_COUPON_CODE");
        }
    }

    @Nested
    @DisplayName("Coupon Applicability Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation for a fully valid coupon")
        void shouldPassValidationForValidCoupon() {
            UUID cafeId = UUID.randomUUID();
            Instant now = Instant.now();
            Coupon coupon = new Coupon(UUID.randomUUID(), cafeId, "PROMO10", DiscountType.PERCENTAGE,
                    BigDecimal.TEN, BigDecimal.valueOf(20), 100, now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));

            evaluationService.validateCouponApplicable(coupon, Money.of(50.00), now);
            assertThat(evaluationService.isCouponApplicable(coupon, Money.of(50.00), now)).isTrue();
        }

        @Test
        @DisplayName("Should fail validation when coupon is inactive")
        void shouldFailWhenInactive() {
            UUID cafeId = UUID.randomUUID();
            Instant now = Instant.now();
            Coupon coupon = new Coupon(cafeId, "OFF10", DiscountType.FIXED_AMOUNT, BigDecimal.TEN);
            coupon.deactivate();

            assertThatThrownBy(() -> evaluationService.validateCouponApplicable(coupon, Money.of(50.00), now))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("COUPON_INACTIVE");

            assertThat(evaluationService.isCouponApplicable(coupon, Money.of(50.00), now)).isFalse();
        }

        @Test
        @DisplayName("Should fail validation when coupon has not started yet")
        void shouldFailWhenNotStarted() {
            UUID cafeId = UUID.randomUUID();
            Instant now = Instant.now();
            Instant future = now.plus(1, ChronoUnit.DAYS);
            Coupon coupon = new Coupon(UUID.randomUUID(), cafeId, "FUTURE", DiscountType.FIXED_AMOUNT,
                    BigDecimal.TEN, BigDecimal.ZERO, null, future, future.plus(5, ChronoUnit.DAYS));

            assertThatThrownBy(() -> evaluationService.validateCouponApplicable(coupon, Money.of(50.00), now))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("COUPON_NOT_STARTED");
        }

        @Test
        @DisplayName("Should fail validation when coupon is expired")
        void shouldFailWhenExpired() {
            UUID cafeId = UUID.randomUUID();
            Instant now = Instant.now();
            Instant past = now.minus(5, ChronoUnit.DAYS);
            Coupon coupon = new Coupon(UUID.randomUUID(), cafeId, "EXPIRED", DiscountType.FIXED_AMOUNT,
                    BigDecimal.TEN, BigDecimal.ZERO, null, past.minus(1, ChronoUnit.DAYS), past);

            assertThatThrownBy(() -> evaluationService.validateCouponApplicable(coupon, Money.of(50.00), now))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("COUPON_EXPIRED");
        }

        @Test
        @DisplayName("Should fail validation when redemption limit reached")
        void shouldFailWhenMaxRedemptionsReached() {
            UUID cafeId = UUID.randomUUID();
            Instant now = Instant.now();
            Coupon coupon = new Coupon(UUID.randomUUID(), cafeId, "LIMIT1", DiscountType.FIXED_AMOUNT,
                    BigDecimal.TEN, BigDecimal.ZERO, 1, null, null);
            coupon.incrementTimesRedeemed();

            assertThatThrownBy(() -> evaluationService.validateCouponApplicable(coupon, Money.of(50.00), now))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("COUPON_MAX_REDEMPTIONS_REACHED");
        }

        @Test
        @DisplayName("Should fail validation when minimum order amount is not met")
        void shouldFailWhenMinOrderNotMet() {
            UUID cafeId = UUID.randomUUID();
            Instant now = Instant.now();
            Coupon coupon = new Coupon(UUID.randomUUID(), cafeId, "MIN100", DiscountType.FIXED_AMOUNT,
                    BigDecimal.TEN, BigDecimal.valueOf(100), null, null, null);

            assertThatThrownBy(() -> evaluationService.validateCouponApplicable(coupon, Money.of(50.00), now))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("COUPON_MIN_ORDER_NOT_MET");
        }
    }

    @Nested
    @DisplayName("Discount Calculation & Capping Tests")
    class DiscountCalculationTests {

        @Test
        @DisplayName("Should calculate percentage discount correctly")
        void shouldCalculatePercentageDiscount() {
            UUID cafeId = UUID.randomUUID();
            Coupon coupon = new Coupon(cafeId, "PERCENT15", DiscountType.PERCENTAGE, BigDecimal.valueOf(15));
            Money subtotal = Money.of(200.00);

            Money discount = evaluationService.calculateDiscount(coupon, subtotal);

            assertThat(discount.amount()).isEqualByComparingTo(BigDecimal.valueOf(30.0000));
            assertThat(discount.currency()).isEqualTo(subtotal.currency());
        }

        @Test
        @DisplayName("Should calculate fixed amount discount correctly")
        void shouldCalculateFixedAmountDiscount() {
            UUID cafeId = UUID.randomUUID();
            Coupon coupon = new Coupon(cafeId, "FLAT25", DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(25));
            Money subtotal = Money.of(100.00);

            Money discount = evaluationService.calculateDiscount(coupon, subtotal);

            assertThat(discount.amount()).isEqualByComparingTo(BigDecimal.valueOf(25.0000));
            assertThat(discount.currency()).isEqualTo(subtotal.currency());
        }

        @Test
        @DisplayName("Should cap fixed discount amount to subtotal when discount exceeds subtotal")
        void shouldCapFixedDiscountToSubtotal() {
            UUID cafeId = UUID.randomUUID();
            Coupon coupon = new Coupon(cafeId, "BIG50", DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(50));
            Money subtotal = Money.of(30.00);

            Money discount = evaluationService.calculateDiscount(coupon, subtotal);

            assertThat(discount.amount()).isEqualByComparingTo(BigDecimal.valueOf(30.0000));
        }

        @Test
        @DisplayName("Should cap 100% percentage discount to exact subtotal")
        void shouldCapFullPercentageDiscount() {
            UUID cafeId = UUID.randomUUID();
            Coupon coupon = new Coupon(cafeId, "FREE", DiscountType.PERCENTAGE, BigDecimal.valueOf(100));
            Money subtotal = Money.of(45.50);

            Money discount = evaluationService.calculateDiscount(coupon, subtotal);

            assertThat(discount.amount()).isEqualByComparingTo(BigDecimal.valueOf(45.5000));
        }

        @Test
        @DisplayName("Should preserve non-default currency")
        void shouldPreserveCurrency() {
            UUID cafeId = UUID.randomUUID();
            Coupon coupon = new Coupon(cafeId, "EUR10", DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(10));
            Money eurSubtotal = new Money(BigDecimal.valueOf(50), Currency.getInstance("EUR"));

            Money discount = evaluationService.calculateDiscount(coupon, eurSubtotal);

            assertThat(discount.currency()).isEqualTo(Currency.getInstance("EUR"));
            assertThat(discount.amount()).isEqualByComparingTo(BigDecimal.valueOf(10.0000));
        }

        @Test
        @DisplayName("Should validate and evaluate discount in single call")
        void shouldEvaluateDiscountWithValidation() {
            UUID cafeId = UUID.randomUUID();
            Instant now = Instant.now();
            Coupon coupon = new Coupon(UUID.randomUUID(), cafeId, "VALID20", DiscountType.PERCENTAGE,
                    BigDecimal.valueOf(20), BigDecimal.valueOf(10), null, null, null);
            Money subtotal = Money.of(100.00);

            Money discount = evaluationService.evaluateDiscount(coupon, subtotal, now);

            assertThat(discount.amount()).isEqualByComparingTo(BigDecimal.valueOf(20.0000));
        }
    }
}
