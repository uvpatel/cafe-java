package com.cafepos.promotion.application;

import com.cafepos.ordering.api.OrderDtoMapper;
import com.cafepos.ordering.api.OrderResponse;
import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderRepository;
import com.cafepos.ordering.domain.OrderType;
import com.cafepos.promotion.api.ApplyCouponRequest;
import com.cafepos.promotion.api.CouponResponse;
import com.cafepos.promotion.api.CreateCouponRequest;
import com.cafepos.promotion.api.PricingPreviewRequest;
import com.cafepos.promotion.api.PricingPreviewResponse;
import com.cafepos.promotion.api.PromotionDtoMapper;
import com.cafepos.promotion.domain.Coupon;
import com.cafepos.promotion.domain.CouponRedemption;
import com.cafepos.promotion.domain.CouponRedemptionRepository;
import com.cafepos.promotion.domain.CouponRepository;
import com.cafepos.promotion.domain.DiscountType;
import com.cafepos.promotion.domain.PromotionEvaluationService;
import com.cafepos.shared.error.BusinessException;
import com.cafepos.shared.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionApplicationServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponRedemptionRepository couponRedemptionRepository;

    @Mock
    private OrderRepository orderRepository;

    private final PromotionEvaluationService promotionEvaluationService = new PromotionEvaluationService();
    private final PromotionDtoMapper promotionDtoMapper = new PromotionDtoMapper();
    private final OrderDtoMapper orderDtoMapper = new OrderDtoMapper();

    private PromotionApplicationService service;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID posSessionId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID couponId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PromotionApplicationService(
                couponRepository,
                couponRedemptionRepository,
                orderRepository,
                promotionEvaluationService,
                promotionDtoMapper,
                orderDtoMapper
        );
    }

    @Nested
    @DisplayName("Manage Coupon Use Case Tests")
    class ManageCouponTests {

        @Test
        @DisplayName("Should create coupon with normalized code")
        void shouldCreateCouponWithNormalizedCode() {
            CreateCouponRequest request = new CreateCouponRequest(
                    "  summer20  ", DiscountType.PERCENTAGE, BigDecimal.valueOf(20),
                    BigDecimal.valueOf(10), 100, null, null
            );

            when(couponRepository.existsByCafeIdAndCodeIgnoreCase(cafeId, "SUMMER20")).thenReturn(false);
            when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CouponResponse response = service.createCoupon(cafeId, request);

            assertThat(response).isNotNull();
            assertThat(response.code()).isEqualTo("SUMMER20");
            assertThat(response.discountType()).isEqualTo(DiscountType.PERCENTAGE);
            assertThat(response.discountValue()).isEqualByComparingTo("20.0000");

            verify(couponRepository).save(any(Coupon.class));
        }

        @Test
        @DisplayName("Should throw BusinessException when creating coupon with existing code")
        void shouldThrowExceptionWhenCodeExists() {
            CreateCouponRequest request = new CreateCouponRequest(
                    "SAVE10", DiscountType.FIXED_AMOUNT, BigDecimal.TEN, null, null, null, null
            );

            when(couponRepository.existsByCafeIdAndCodeIgnoreCase(cafeId, "SAVE10")).thenReturn(true);

            assertThatThrownBy(() -> service.createCoupon(cafeId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Should toggle coupon active status")
        void shouldToggleActiveStatus() {
            Coupon coupon = new Coupon(couponId, cafeId, "PROMO10", DiscountType.PERCENTAGE, BigDecimal.TEN, BigDecimal.ZERO, null, null, null);
            when(couponRepository.findByIdAndCafeId(couponId, cafeId)).thenReturn(Optional.of(coupon));
            when(couponRepository.save(coupon)).thenReturn(coupon);

            CouponResponse deactivated = service.toggleCouponActiveStatus(cafeId, couponId, false);
            assertThat(deactivated.active()).isFalse();

            CouponResponse activated = service.toggleCouponActiveStatus(cafeId, couponId, true);
            assertThat(activated.active()).isTrue();
        }

        @Test
        @DisplayName("Should fetch coupons for cafe")
        void shouldGetCouponsForCafe() {
            Coupon coupon = new Coupon(couponId, cafeId, "PROMO10", DiscountType.PERCENTAGE, BigDecimal.TEN, BigDecimal.ZERO, null, null, null);
            when(couponRepository.findByCafeId(cafeId)).thenReturn(List.of(coupon));

            List<CouponResponse> responses = service.getCouponsForCafe(cafeId);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).code()).isEqualTo("PROMO10");
        }
    }

    @Nested
    @DisplayName("Apply & Remove Coupon Use Case Tests")
    class ApplyAndRemoveCouponTests {

        @Test
        @DisplayName("Should apply coupon to order successfully")
        void shouldApplyCouponToOrder() {
            Order order = new Order(orderId, cafeId, posSessionId, null, "ORD-001", OrderType.TAKEAWAY, "USD");
            order.addOrUpdateLine(UUID.randomUUID(), "Coffee", Money.of(50.00), 1, false);

            Coupon coupon = new Coupon(couponId, cafeId, "SAVE10", DiscountType.PERCENTAGE, BigDecimal.valueOf(10), BigDecimal.ZERO, 100, null, null);

            ApplyCouponRequest request = new ApplyCouponRequest("  save10  ", null);

            when(orderRepository.findByIdAndCafeId(orderId, cafeId)).thenReturn(Optional.of(order));
            when(couponRedemptionRepository.existsByOrderId(orderId)).thenReturn(false);
            when(couponRepository.findByCafeIdAndCodeIgnoreCase(cafeId, "SAVE10")).thenReturn(Optional.of(coupon));
            when(orderRepository.save(order)).thenReturn(order);
            when(couponRepository.save(coupon)).thenReturn(coupon);

            OrderResponse response = service.applyCouponToOrder(cafeId, orderId, request);

            assertThat(response).isNotNull();
            assertThat(response.discountAmount()).isEqualByComparingTo("5.0000");
            assertThat(response.totalAmount()).isEqualByComparingTo("45.0000");
            assertThat(coupon.getTimesRedeemed()).isEqualTo(1);

            verify(couponRedemptionRepository).save(any(CouponRedemption.class));
            verify(couponRepository).save(coupon);
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("Should throw BusinessException when applying to non-existent order")
        void shouldThrowExceptionWhenOrderNotFound() {
            ApplyCouponRequest request = new ApplyCouponRequest("SAVE10", null);
            when(orderRepository.findByIdAndCafeId(orderId, cafeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.applyCouponToOrder(cafeId, orderId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Order not found");
        }

        @Test
        @DisplayName("Should throw BusinessException when coupon already applied")
        void shouldThrowExceptionWhenCouponAlreadyApplied() {
            Order order = new Order(orderId, cafeId, posSessionId, null, "ORD-001", OrderType.TAKEAWAY, "USD");
            ApplyCouponRequest request = new ApplyCouponRequest("SAVE10", null);

            when(orderRepository.findByIdAndCafeId(orderId, cafeId)).thenReturn(Optional.of(order));
            when(couponRedemptionRepository.existsByOrderId(orderId)).thenReturn(true);

            assertThatThrownBy(() -> service.applyCouponToOrder(cafeId, orderId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already has an applied coupon");
        }

        @Test
        @DisplayName("Should remove coupon from order and delete redemption snapshot")
        void shouldRemoveCouponFromOrder() {
            Order order = new Order(orderId, cafeId, posSessionId, null, "ORD-001", OrderType.TAKEAWAY, "USD");
            order.addOrUpdateLine(UUID.randomUUID(), "Coffee", Money.of(50.00), 1, false);
            order.setDiscount(Money.of(5.00));

            CouponRedemption redemption = new CouponRedemption(cafeId, couponId, orderId);

            when(orderRepository.findByIdAndCafeId(orderId, cafeId)).thenReturn(Optional.of(order));
            when(couponRedemptionRepository.findByOrderId(orderId)).thenReturn(Optional.of(redemption));
            when(orderRepository.save(order)).thenReturn(order);

            OrderResponse response = service.removeCouponFromOrder(cafeId, orderId);

            assertThat(response.discountAmount()).isEqualByComparingTo("0.0000");
            assertThat(response.totalAmount()).isEqualByComparingTo("50.0000");

            verify(couponRedemptionRepository).delete(redemption);
            verify(orderRepository).save(order);
        }
    }

    @Nested
    @DisplayName("Pricing Preview Use Case Tests")
    class PricingPreviewTests {

        @Test
        @DisplayName("Should calculate pricing preview with applicable coupon without saving changes")
        void shouldCalculatePricingPreviewWithCoupon() {
            Order order = new Order(orderId, cafeId, posSessionId, null, "ORD-001", OrderType.TAKEAWAY, "USD");
            order.addOrUpdateLine(UUID.randomUUID(), "Burger", Money.of(20.00), 2, false);

            Coupon coupon = new Coupon(couponId, cafeId, "FLAT5", DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(5), BigDecimal.ZERO, null, null, null);

            PricingPreviewRequest request = new PricingPreviewRequest("FLAT5", null, null);

            when(orderRepository.findByIdAndCafeId(orderId, cafeId)).thenReturn(Optional.of(order));
            when(couponRepository.findByCafeIdAndCodeIgnoreCase(cafeId, "FLAT5")).thenReturn(Optional.of(coupon));

            PricingPreviewResponse preview = service.previewPricing(cafeId, orderId, request);

            assertThat(preview).isNotNull();
            assertThat(preview.subtotal()).isEqualByComparingTo("40.0000");
            assertThat(preview.discountAmount()).isEqualByComparingTo("5.0000");
            assertThat(preview.totalAmount()).isEqualByComparingTo("35.0000");
            assertThat(preview.couponApplied()).isTrue();
            assertThat(preview.couponCode()).isEqualTo("FLAT5");
        }

        @Test
        @DisplayName("Should return preview without discount if coupon is invalid or not found")
        void shouldReturnPreviewWithoutDiscountForInvalidCoupon() {
            Order order = new Order(orderId, cafeId, posSessionId, null, "ORD-001", OrderType.TAKEAWAY, "USD");
            order.addOrUpdateLine(UUID.randomUUID(), "Burger", Money.of(20.00), 1, false);

            PricingPreviewRequest request = new PricingPreviewRequest("INVALID", null, null);

            when(orderRepository.findByIdAndCafeId(orderId, cafeId)).thenReturn(Optional.of(order));
            when(couponRepository.findByCafeIdAndCodeIgnoreCase(cafeId, "INVALID")).thenReturn(Optional.empty());

            PricingPreviewResponse preview = service.previewPricing(cafeId, orderId, request);

            assertThat(preview.subtotal()).isEqualByComparingTo("20.0000");
            assertThat(preview.discountAmount()).isEqualByComparingTo("0.0000");
            assertThat(preview.totalAmount()).isEqualByComparingTo("20.0000");
            assertThat(preview.couponApplied()).isFalse();
        }
    }
}
