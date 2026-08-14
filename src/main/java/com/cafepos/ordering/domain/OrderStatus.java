package com.cafepos.ordering.domain;

public enum OrderStatus {
    DRAFT,
    CONFIRMED,
    SENT_TO_KITCHEN,
    READY,
    PAID,
    CANCELLED
}
