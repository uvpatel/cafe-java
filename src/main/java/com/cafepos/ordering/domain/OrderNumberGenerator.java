package com.cafepos.ordering.domain;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

@Service
public class OrderNumberGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final OrderRepository orderRepository;

    public OrderNumberGenerator(OrderRepository orderRepository) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
    }

    public synchronized String generateOrderNumber(UUID cafeId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        String datePart = LocalDate.now(ZoneOffset.UTC).format(DATE_FORMATTER);
        String prefix = "ORD-" + datePart + "-";

        int sequence = 1;
        while (sequence < 10000) {
            String candidate = String.format("%s%04d", prefix, sequence);
            if (!orderRepository.existsByCafeIdAndOrderNumberIgnoreCase(cafeId, candidate)) {
                return candidate;
            }
            sequence++;
        }
        return prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
