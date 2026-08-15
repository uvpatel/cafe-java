package com.cafepos.loyalty.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, UUID> {
    List<LoyaltyTransaction> findByCafeIdAndLoyaltyAccountIdOrderByCreatedAtDesc(UUID cafeId, UUID loyaltyAccountId);
}
