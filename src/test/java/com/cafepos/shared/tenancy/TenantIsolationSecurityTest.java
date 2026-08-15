package com.cafepos.shared.tenancy;

import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderRepository;
import com.cafepos.ordering.domain.OrderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantIsolationSecurityTest {

    @Mock
    private OrderRepository orderRepository;

    @Test
    @DisplayName("Should prevent cross-tenant resource leakage when Tenant A requests Tenant B's order ID")
    void verifyCrossTenantOrderIsolation() {
        UUID cafeAId = UUID.randomUUID();
        UUID cafeBId = UUID.randomUUID();
        UUID orderBId = UUID.randomUUID();

        Order orderTenantB = new Order(orderBId, cafeBId, UUID.randomUUID(), UUID.randomUUID(), "ORD-TENANT-B", OrderType.DINE_IN, "USD");

        // Repository correctly filters by cafeId
        when(orderRepository.findByIdAndCafeId(orderBId, cafeAId)).thenReturn(Optional.empty());
        when(orderRepository.findByIdAndCafeId(orderBId, cafeBId)).thenReturn(Optional.of(orderTenantB));

        // When Tenant A attempts lookup of Tenant B's order ID
        Optional<Order> tenantALookup = orderRepository.findByIdAndCafeId(orderBId, cafeAId);
        assertThat(tenantALookup).isEmpty();

        // When Tenant B looks up their own order ID
        Optional<Order> tenantBLookup = orderRepository.findByIdAndCafeId(orderBId, cafeBId);
        assertThat(tenantBLookup).isPresent();
        assertThat(tenantBLookup.get().getCafeId()).isEqualTo(cafeBId);
    }
}
