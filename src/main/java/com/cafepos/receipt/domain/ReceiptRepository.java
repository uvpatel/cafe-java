package com.cafepos.receipt.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {
    Optional<Receipt> findByOrderId(UUID orderId);
    Optional<Receipt> findByCafeIdAndReceiptNumber(UUID cafeId, String receiptNumber);
}
