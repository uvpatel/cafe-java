package com.cafepos.promotion.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, UUID> {

    List<CouponRedemption> findByCafeId(UUID cafeId);

    Optional<CouponRedemption> findByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);

    List<CouponRedemption> findByCouponId(UUID couponId);

    long countByCouponId(UUID couponId);
}
