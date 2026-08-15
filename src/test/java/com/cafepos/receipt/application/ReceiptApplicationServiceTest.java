package com.cafepos.receipt.application;

import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderRepository;

import com.cafepos.ordering.domain.OrderType;
import com.cafepos.receipt.api.ReceiptDtoMapper;
import com.cafepos.receipt.api.ReceiptResponse;
import com.cafepos.receipt.domain.Receipt;
import com.cafepos.receipt.domain.ReceiptRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptApplicationServiceTest {

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private OrderRepository orderRepository;

    private final ReceiptDtoMapper mapper = new ReceiptDtoMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ReceiptApplicationService receiptApplicationService;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private Order paidOrder;

    @BeforeEach
    void setUp() {
        receiptApplicationService = new ReceiptApplicationService(receiptRepository, orderRepository, mapper, objectMapper);
        paidOrder = new Order(orderId, cafeId, UUID.randomUUID(), UUID.randomUUID(), "ORD-001", OrderType.DINE_IN, "USD");
        paidOrder.markPaid();
    }

    @Test
    @DisplayName("Should generate receipt successfully for PAID order")
    void generateReceiptSuccessfully() {
        when(receiptRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(orderRepository.findByIdAndCafeId(orderId, cafeId)).thenReturn(Optional.of(paidOrder));
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(inv -> inv.getArgument(0));

        ReceiptResponse response = receiptApplicationService.generateReceiptForOrder(cafeId, orderId);

        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.receiptNumber()).contains("ORD-001");
        verify(receiptRepository).save(any(Receipt.class));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when generating receipt for un-PAID order")
    void generateReceiptUnpaidOrderThrows() {
        Order draftOrder = new Order(cafeId, UUID.randomUUID(), UUID.randomUUID(), "ORD-002", OrderType.DINE_IN);
        when(receiptRepository.findByOrderId(draftOrder.getId())).thenReturn(Optional.empty());
        when(orderRepository.findByIdAndCafeId(draftOrder.getId(), cafeId)).thenReturn(Optional.of(draftOrder));

        assertThatThrownBy(() -> receiptApplicationService.generateReceiptForOrder(cafeId, draftOrder.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot generate receipt for order that is not PAID");
    }
}
