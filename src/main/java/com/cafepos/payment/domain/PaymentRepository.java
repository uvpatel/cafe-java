package com.cafepos.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(UUID orderId);
    Optional<Payment> findByCafeIdAndIdempotencyKey(UUID cafeId, String idempotencyKey);
    List<Payment> findByCafeId(UUID cafeId);
}
