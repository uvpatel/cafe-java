package com.cafepos.loyalty.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, UUID> {
    Optional<LoyaltyAccount> findByCafeIdAndCustomerId(UUID cafeId, UUID customerId);
    Optional<LoyaltyAccount> findByCafeIdAndId(UUID cafeId, UUID id);
}
