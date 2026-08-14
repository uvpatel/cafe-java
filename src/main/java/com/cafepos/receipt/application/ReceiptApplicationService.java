package com.cafepos.receipt.application;

import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderRepository;
import com.cafepos.ordering.domain.OrderStatus;
import com.cafepos.payment.domain.Payment;
import com.cafepos.payment.domain.PaymentRepository;
import com.cafepos.receipt.api.ReceiptDtoMapper;
import com.cafepos.receipt.api.ReceiptResponse;
import com.cafepos.shared.error.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class ReceiptApplicationService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ReceiptDtoMapper receiptDtoMapper;

    public ReceiptApplicationService(OrderRepository orderRepository,
                                     PaymentRepository paymentRepository,
                                     ReceiptDtoMapper receiptDtoMapper) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.receiptDtoMapper = Objects.requireNonNull(receiptDtoMapper, "receiptDtoMapper must not be null");
    }

    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptForOrder(UUID cafeId, UUID orderId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found with id " + orderId, 404));

        if (!order.getCafeId().equals(cafeId)) {
            throw new BusinessException("ORDER_NOT_FOUND", "Order not found with id " + orderId, 404);
        }

        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException("ORDER_NOT_PAID", "Receipt can only be generated for paid orders", 400);
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND", "Payment record not found for order id " + orderId, 404));

        return receiptDtoMapper.toReceiptResponse(order, payment);
    }
}
