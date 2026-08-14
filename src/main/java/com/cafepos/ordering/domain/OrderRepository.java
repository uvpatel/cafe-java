package com.cafepos.ordering.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByCafeId(UUID cafeId);

    Optional<Order> findByIdAndCafeId(UUID id, UUID cafeId);

    List<Order> findByCafeIdAndPosSessionId(UUID cafeId, UUID posSessionId);

    List<Order> findByCafeIdAndStatus(UUID cafeId, OrderStatus status);

    Optional<Order> findByCafeIdAndDiningTableIdAndStatusIn(UUID cafeId, UUID diningTableId, List<OrderStatus> statuses);

    boolean existsByCafeIdAndOrderNumberIgnoreCase(UUID cafeId, String orderNumber);
}
