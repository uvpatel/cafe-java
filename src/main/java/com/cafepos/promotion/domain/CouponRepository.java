package com.cafepos.promotion.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    List<Coupon> findByCafeId(UUID cafeId);

    Optional<Coupon> findByIdAndCafeId(UUID id, UUID cafeId);

    Optional<Coupon> findByCafeIdAndCodeIgnoreCase(UUID cafeId, String code);

    boolean existsByCafeIdAndCodeIgnoreCase(UUID cafeId, String code);
}
