package com.cafepos.shared.events;

public enum OutboxEventStatus {
    PENDING,
    PROCESSED,
    FAILED
}
