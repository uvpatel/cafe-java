package com.cafepos.promotion.application;

import com.cafepos.ordering.api.OrderDtoMapper;
import com.cafepos.ordering.api.OrderResponse;
import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderRepository;
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
import com.cafepos.promotion.domain.PromotionEvaluationService;
import com.cafepos.shared.error.BusinessException;
import com.cafepos.shared.money.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class PromotionApplicationService implements ManageCouponUseCase, ApplyCouponToOrderUseCase, RemoveCouponFromOrderUseCase, PricingPreviewUseCase {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final OrderRepository orderRepository;
    private final PromotionEvaluationService promotionEvaluationService;
    private final PromotionDtoMapper promotionDtoMapper;
    private final OrderDtoMapper orderDtoMapper;

    public PromotionApplicationService(CouponRepository couponRepository,
                                       CouponRedemptionRepository couponRedemptionRepository,
                                       OrderRepository orderRepository,
                                       PromotionEvaluationService promotionEvaluationService,
                                       PromotionDtoMapper promotionDtoMapper,
                                       OrderDtoMapper orderDtoMapper) {
        this.couponRepository = Objects.requireNonNull(couponRepository, "couponRepository must not be null");
        this.couponRedemptionRepository = Objects.requireNonNull(couponRedemptionRepository, "couponRedemptionRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.promotionEvaluationService = Objects.requireNonNull(promotionEvaluationService, "promotionEvaluationService must not be null");
        this.promotionDtoMapper = Objects.requireNonNull(promotionDtoMapper, "promotionDtoMapper must not be null");
        this.orderDtoMapper = Objects.requireNonNull(orderDtoMapper, "orderDtoMapper must not be null");
    }

    @Override
    @Transactional
    public CouponResponse createCoupon(UUID cafeId, CreateCouponRequest request) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        String normalizedCode = promotionEvaluationService.normalizeCode(request.code());
        if (couponRepository.existsByCafeIdAndCodeIgnoreCase(cafeId, normalizedCode)) {
            throw new BusinessException("COUPON_CODE_EXISTS", "Coupon with code " + normalizedCode + " already exists for this cafe", 400);
        }

        Coupon coupon = new Coupon(
                UUID.randomUUID(),
                cafeId,
                normalizedCode,
                request.discountType(),
                request.discountValue(),
                request.minOrderAmount(),
                request.maxRedemptions(),
                request.startsAt(),
                request.expiresAt()
        );

        Coupon savedCoupon = couponRepository.save(coupon);
        return promotionDtoMapper.toCouponResponse(savedCoupon);
    }

    @Override
    @Transactional
    public CouponResponse toggleCouponActiveStatus(UUID cafeId, UUID couponId, boolean active) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(couponId, "couponId must not be null");

        Coupon coupon = couponRepository.findByIdAndCafeId(couponId, cafeId)
                .orElseThrow(() -> new BusinessException("COUPON_NOT_FOUND", "Coupon not found with id " + couponId, 404));

        if (active) {
            coupon.activate();
        } else {
            coupon.deactivate();
        }

        Coupon updatedCoupon = couponRepository.save(coupon);
        return promotionDtoMapper.toCouponResponse(updatedCoupon);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getCouponsForCafe(UUID cafeId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        return couponRepository.findByCafeId(cafeId).stream()
                .map(promotionDtoMapper::toCouponResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getCouponById(UUID cafeId, UUID couponId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(couponId, "couponId must not be null");

        Coupon coupon = couponRepository.findByIdAndCafeId(couponId, cafeId)
                .orElseThrow(() -> new BusinessException("COUPON_NOT_FOUND", "Coupon not found with id " + couponId, 404));

        return promotionDtoMapper.toCouponResponse(coupon);
    }

    @Override
    @Transactional
    public OrderResponse applyCouponToOrder(UUID cafeId, UUID orderId, ApplyCouponRequest request) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        Order order = orderRepository.findByIdAndCafeId(orderId, cafeId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found with id " + orderId, 404));

        if (couponRedemptionRepository.existsByOrderId(orderId)) {
            throw new BusinessException("COUPON_ALREADY_APPLIED", "Order already has an applied coupon. Remove existing coupon first.", 400);
        }

        String normalizedCode = promotionEvaluationService.normalizeCode(request.code());
        Coupon coupon = couponRepository.findByCafeIdAndCodeIgnoreCase(cafeId, normalizedCode)
                .orElseThrow(() -> new BusinessException("COUPON_NOT_FOUND", "Coupon not found with code " + normalizedCode, 404));

        promotionEvaluationService.validateCouponApplicable(coupon, order.getSubtotal(), Instant.now());

        Money discount = promotionEvaluationService.calculateDiscount(coupon, order.getSubtotal());

        order.setDiscount(discount);
        orderRepository.save(order);

        coupon.incrementTimesRedeemed();
        couponRepository.save(coupon);

        CouponRedemption redemption = new CouponRedemption(
                UUID.randomUUID(),
                cafeId,
                coupon.getId(),
                order.getId(),
                request.customerId(),
                Instant.now()
        );
        couponRedemptionRepository.save(redemption);

        return orderDtoMapper.toOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse removeCouponFromOrder(UUID cafeId, UUID orderId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");

        Order order = orderRepository.findByIdAndCafeId(orderId, cafeId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found with id " + orderId, 404));

        Optional<CouponRedemption> redemptionOpt = couponRedemptionRepository.findByOrderId(orderId);
        if (redemptionOpt.isPresent()) {
            couponRedemptionRepository.delete(redemptionOpt.get());
        }

        order.setDiscount(Money.zero(order.getCurrency()));
        orderRepository.save(order);

        return orderDtoMapper.toOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PricingPreviewResponse previewPricing(UUID cafeId, UUID orderId, PricingPreviewRequest request) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");

        Order order = orderRepository.findByIdAndCafeId(orderId, cafeId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found with id " + orderId, 404));

        Money subtotal = order.getSubtotal();
        Money taxAmount = order.getTax();
        String code = request != null ? request.getEffectiveCode() : null;

        BigDecimal potentialDiscountAmount = BigDecimal.ZERO;
        boolean couponApplied = false;
        String appliedCouponCode = null;

        if (code != null && !code.isBlank()) {
            String normalizedCode = promotionEvaluationService.normalizeCode(code);
            Optional<Coupon> couponOpt = couponRepository.findByCafeIdAndCodeIgnoreCase(cafeId, normalizedCode);

            if (couponOpt.isPresent()) {
                Coupon coupon = couponOpt.get();
                if (promotionEvaluationService.isCouponApplicable(coupon, subtotal, Instant.now())) {
                    Money discountMoney = promotionEvaluationService.calculateDiscount(coupon, subtotal);
                    potentialDiscountAmount = discountMoney.amount();
                    couponApplied = true;
                    appliedCouponCode = coupon.getCode();
                } else {
                    appliedCouponCode = normalizedCode;
                }
            } else {
                appliedCouponCode = normalizedCode;
            }
        } else {
            potentialDiscountAmount = order.getDiscount().amount();
            if (potentialDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
                couponApplied = true;
            }
        }

        Money discountMoney = new Money(potentialDiscountAmount, order.getCurrency());
        Money netSubtotal = subtotal.amount().compareTo(potentialDiscountAmount) < 0
                ? Money.zero(order.getCurrency())
                : subtotal.subtract(discountMoney);
        Money total = netSubtotal.add(taxAmount);

        return new PricingPreviewResponse(
                subtotal.amount(),
                taxAmount.amount(),
                potentialDiscountAmount,
                total.amount(),
                order.getCurrency().getCurrencyCode(),
                appliedCouponCode,
                couponApplied
        );
    }
}
