package com.cafepos.booking.api;

import com.cafepos.ordering.api.OrderResponse;

public record ConvertToOrderResponse(
        BookingResponse booking,
        OrderResponse order
) {
}
