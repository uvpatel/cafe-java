package com.cafepos.booking.api;

import com.cafepos.booking.domain.TableBooking;
import org.springframework.stereotype.Component;

@Component
public class BookingDtoMapper {

    public BookingResponse toBookingResponse(TableBooking booking) {
        if (booking == null) {
            return null;
        }
        return new BookingResponse(
                booking.getId(),
                booking.getCafeId(),
                booking.getDiningTableId(),
                booking.getCustomerName(),
                booking.getCustomerPhone(),
                booking.getPartySize(),
                booking.getBookingStart(),
                booking.getBookingEnd(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}
