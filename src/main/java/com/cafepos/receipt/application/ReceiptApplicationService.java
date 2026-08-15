package com.cafepos.receipt.application;

import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderRepository;
import com.cafepos.ordering.domain.OrderStatus;
import com.cafepos.receipt.api.ReceiptDtoMapper;
import com.cafepos.receipt.api.ReceiptResponse;
import com.cafepos.receipt.domain.Receipt;
import com.cafepos.receipt.domain.ReceiptRepository;
import com.cafepos.shared.error.BusinessException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class ReceiptApplicationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ReceiptRepository receiptRepository;
    private final OrderRepository orderRepository;
    private final ReceiptDtoMapper mapper;
    private final ObjectMapper objectMapper;

    public ReceiptApplicationService(ReceiptRepository receiptRepository,
                                     OrderRepository orderRepository,
                                     ReceiptDtoMapper mapper,
                                     ObjectMapper objectMapper) {
        this.receiptRepository = Objects.requireNonNull(receiptRepository, "receiptRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public ReceiptResponse generateReceiptForOrder(UUID cafeId, UUID orderId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");

        return receiptRepository.findByOrderId(orderId)
                .map(mapper::toResponse)
                .orElseGet(() -> {
                    Order order = orderRepository.findByIdAndCafeId(orderId, cafeId)
                            .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found with id " + orderId, 404));

                    if (order.getStatus() != OrderStatus.PAID) {
                        throw new IllegalStateException("Cannot generate receipt for order that is not PAID. Current status: " + order.getStatus());
                    }

                    String receiptNumber = generateReceiptNumber(cafeId, order);
                    String taxJson = buildTaxBreakdownJson(order);

                    Receipt receipt = new Receipt(cafeId, orderId, receiptNumber, order, taxJson);
                    Receipt saved = receiptRepository.save(receipt);
                    return mapper.toResponse(saved);
                });
    }

    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptByOrderId(UUID cafeId, UUID orderId) {
        return receiptRepository.findByOrderId(orderId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new BusinessException("RECEIPT_NOT_FOUND", "Receipt not found for order id " + orderId, 404));
    }

    private String generateReceiptNumber(UUID cafeId, Order order) {
        String datePrefix = LocalDate.now().format(DATE_FORMATTER);
        String orderSuffix = order.getOrderNumber();
        return "RCP-" + datePrefix + "-" + orderSuffix;
    }

    private String buildTaxBreakdownJson(Order order) {
        try {
            return objectMapper.writeValueAsString(order.getTaxLines());
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
