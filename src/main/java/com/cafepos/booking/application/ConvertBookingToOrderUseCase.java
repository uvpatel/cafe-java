package com.cafepos.booking.application;

import com.cafepos.booking.api.ConvertToOrderResponse;

import java.util.UUID;

public interface ConvertBookingToOrderUseCase {
    ConvertToOrderResponse convertBookingToOrder(UUID cafeId, UUID bookingId, UUID posSessionId);
}
