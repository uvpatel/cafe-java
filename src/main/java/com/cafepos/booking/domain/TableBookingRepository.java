package com.cafepos.booking.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TableBookingRepository extends JpaRepository<TableBooking, UUID> {

    List<TableBooking> findByCafeId(UUID cafeId);

    List<TableBooking> findByCafeIdAndDiningTableId(UUID cafeId, UUID diningTableId);

    List<TableBooking> findByCafeIdAndStatus(UUID cafeId, BookingStatus status);

    Optional<TableBooking> findByIdAndCafeId(UUID id, UUID cafeId);

    @Query("""
        SELECT b FROM TableBooking b
        WHERE b.cafeId = :cafeId
          AND b.diningTableId = :diningTableId
          AND b.status = com.cafepos.booking.domain.BookingStatus.CONFIRMED
          AND b.bookingStart < :endTime
          AND b.bookingEnd > :startTime
        """)
    List<TableBooking> findOverlappingBookings(
        @Param("cafeId") UUID cafeId,
        @Param("diningTableId") UUID diningTableId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    @Query("""
        SELECT b FROM TableBooking b
        WHERE b.cafeId = :cafeId
          AND b.diningTableId = :diningTableId
          AND b.status = com.cafepos.booking.domain.BookingStatus.CONFIRMED
          AND b.bookingStart < :endTime
          AND b.bookingEnd > :startTime
          AND (:excludeBookingId IS NULL OR b.id <> :excludeBookingId)
        """)
    List<TableBooking> findOverlappingBookingsExcluding(
        @Param("cafeId") UUID cafeId,
        @Param("diningTableId") UUID diningTableId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        @Param("excludeBookingId") UUID excludeBookingId
    );
}
