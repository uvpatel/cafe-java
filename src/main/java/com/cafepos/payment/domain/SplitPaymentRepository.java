package com.cafepos.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SplitPaymentRepository extends JpaRepository<SplitPayment, UUID> {
    List<SplitPayment> findByCafeIdAndOrderIdOrderBySplitIndexAsc(UUID cafeId, UUID orderId);
}
