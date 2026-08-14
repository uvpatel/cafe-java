package com.cafepos.booking.domain;

import com.cafepos.shared.error.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "table_bookings")
public class TableBooking {

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "dining_table_id", nullable = false)
    private UUID diningTableId;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 30)
    private String customerPhone;

    @Column(name = "party_size", nullable = false)
    private int partySize;

    @Column(name = "booking_start", nullable = false)
    private Instant bookingStart;

    @Column(name = "booking_end", nullable = false)
    private Instant bookingEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TableBooking() {
        // JPA standard constructor
    }

    public TableBooking(UUID id, UUID cafeId, UUID diningTableId, String customerName, String customerPhone,
                        int partySize, Instant bookingStart, Instant bookingEnd, BookingStatus status) {
        validateInputs(cafeId, diningTableId, customerName, customerPhone, partySize, bookingStart, bookingEnd);
        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.diningTableId = diningTableId;
        this.customerName = customerName.trim();
        this.customerPhone = customerPhone.trim();
        this.partySize = partySize;
        this.bookingStart = bookingStart;
        this.bookingEnd = bookingEnd;
        this.status = status != null ? status : BookingStatus.CONFIRMED;
    }

    public TableBooking(UUID cafeId, UUID diningTableId, String customerName, String customerPhone,
                        int partySize, Instant bookingStart, Instant bookingEnd) {
        this(UUID.randomUUID(), cafeId, diningTableId, customerName, customerPhone, partySize, bookingStart, bookingEnd, BookingStatus.CONFIRMED);
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = BookingStatus.CONFIRMED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public static void validateInputs(UUID cafeId, UUID diningTableId, String customerName, String customerPhone,
                                       int partySize, Instant bookingStart, Instant bookingEnd) {
        if (cafeId == null) {
            throw new IllegalArgumentException("Cafe ID must not be null");
        }
        if (diningTableId == null) {
            throw new IllegalArgumentException("Dining table ID must not be null");
        }
        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("Customer name must not be empty");
        }
        if (customerName.trim().length() > 100) {
            throw new IllegalArgumentException("Customer name must not exceed 100 characters");
        }
        if (customerPhone == null || customerPhone.isBlank()) {
            throw new IllegalArgumentException("Customer phone must not be empty");
        }
        if (customerPhone.trim().length() > 30) {
            throw new IllegalArgumentException("Customer phone must not exceed 30 characters");
        }
        if (partySize <= 0) {
            throw new IllegalArgumentException("Party size must be greater than 0");
        }
        if (bookingStart == null) {
            throw new IllegalArgumentException("Booking start time must not be null");
        }
        if (bookingEnd == null) {
            throw new IllegalArgumentException("Booking end time must not be null");
        }
        if (!bookingStart.isBefore(bookingEnd)) {
            throw new BusinessException("INVALID_BOOKING_INTERVAL", "Booking start time must be before end time", 400);
        }
    }

    public void convertToOrder() {
        if (this.status != BookingStatus.CONFIRMED) {
            throw new BusinessException("BOOKING_NOT_CONFIRMED", "Only CONFIRMED bookings can be converted to an order", 400);
        }
        this.status = BookingStatus.CONVERTED_TO_ORDER;
    }

    public void cancel() {
        if (this.status != BookingStatus.CONFIRMED) {
            throw new BusinessException("BOOKING_NOT_CONFIRMED", "Only CONFIRMED bookings can be cancelled", 400);
        }
        this.status = BookingStatus.CANCELLED;
    }

    public void updateDetails(String customerName, String customerPhone, int partySize, Instant bookingStart, Instant bookingEnd) {
        if (this.status != BookingStatus.CONFIRMED) {
            throw new BusinessException("BOOKING_NOT_CONFIRMED", "Only CONFIRMED bookings can be updated", 400);
        }
        validateInputs(this.cafeId, this.diningTableId, customerName, customerPhone, partySize, bookingStart, bookingEnd);
        this.customerName = customerName.trim();
        this.customerPhone = customerPhone.trim();
        this.partySize = partySize;
        this.bookingStart = bookingStart;
        this.bookingEnd = bookingEnd;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCafeId() {
        return cafeId;
    }

    public UUID getDiningTableId() {
        return diningTableId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public int getPartySize() {
        return partySize;
    }

    public Instant getBookingStart() {
        return bookingStart;
    }

    public Instant getBookingEnd() {
        return bookingEnd;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableBooking that = (TableBooking) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
