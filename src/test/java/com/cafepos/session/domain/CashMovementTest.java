package com.cafepos.session.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CashMovementTest {

    private final UUID cafeId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();

    @Nested
    @DisplayName("Cash Movement Creation and Validation")
    class CreationAndValidationTests {

        @Test
        @DisplayName("Should create valid cash movement")
        void shouldCreateValidCashMovement() {
            CashMovement movement = new CashMovement(cafeId, sessionId, employeeId, CashMovementType.CASH_IN, new BigDecimal("50.0000"), "Till float refill");

            assertThat(movement.getId()).isNotNull();
            assertThat(movement.getCafeId()).isEqualTo(cafeId);
            assertThat(movement.getSessionId()).isEqualTo(sessionId);
            assertThat(movement.getEmployeeId()).isEqualTo(employeeId);
            assertThat(movement.getType()).isEqualTo(CashMovementType.CASH_IN);
            assertThat(movement.getAmount()).isEqualTo(new BigDecimal("50.0000"));
            assertThat(movement.getReason()).isEqualTo("Till float refill");
        }

        @Test
        @DisplayName("Should validate required parameters")
        void shouldValidateNullParameters() {
            assertThatThrownBy(() -> new CashMovement(null, sessionId, employeeId, CashMovementType.CASH_IN, BigDecimal.TEN, "Reason"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cafe ID must not be null");

            assertThatThrownBy(() -> new CashMovement(cafeId, null, employeeId, CashMovementType.CASH_IN, BigDecimal.TEN, "Reason"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Session ID must not be null");

            assertThatThrownBy(() -> new CashMovement(cafeId, sessionId, null, CashMovementType.CASH_IN, BigDecimal.TEN, "Reason"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee ID must not be null");

            assertThatThrownBy(() -> new CashMovement(cafeId, sessionId, employeeId, null, BigDecimal.TEN, "Reason"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cash movement type must not be null");

            assertThatThrownBy(() -> new CashMovement(cafeId, sessionId, employeeId, CashMovementType.CASH_IN, null, "Reason"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount must not be null");
        }

        @Test
        @DisplayName("Should reject zero or negative amount")
        void shouldRejectNonPositiveAmount() {
            assertThatThrownBy(() -> new CashMovement(cafeId, sessionId, employeeId, CashMovementType.CASH_IN, BigDecimal.ZERO, "Reason"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount must be greater than zero");

            assertThatThrownBy(() -> new CashMovement(cafeId, sessionId, employeeId, CashMovementType.CASH_IN, new BigDecimal("-5.00"), "Reason"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount must be greater than zero");
        }

        @Test
        @DisplayName("Should validate reason length and empty string")
        void shouldValidateReason() {
            assertThatThrownBy(() -> new CashMovement(cafeId, sessionId, employeeId, CashMovementType.CASH_IN, BigDecimal.TEN, "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Reason must not be empty");

            String longReason = "a".repeat(256);
            assertThatThrownBy(() -> new CashMovement(cafeId, sessionId, employeeId, CashMovementType.CASH_IN, BigDecimal.TEN, longReason))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Reason must not exceed 255 characters");
        }
    }

    @Nested
    @DisplayName("Equals and HashCode")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should verify entity equality by ID")
        void shouldVerifyEqualsAndHashCode() {
            UUID movementId = UUID.randomUUID();
            CashMovement m1 = new CashMovement(movementId, cafeId, sessionId, employeeId, CashMovementType.CASH_IN, BigDecimal.TEN, "Reason");
            CashMovement m2 = new CashMovement(movementId, cafeId, sessionId, employeeId, CashMovementType.CASH_IN, BigDecimal.TEN, "Reason");
            CashMovement m3 = new CashMovement(UUID.randomUUID(), cafeId, sessionId, employeeId, CashMovementType.CASH_IN, BigDecimal.TEN, "Reason");

            assertThat(m1).isEqualTo(m2);
            assertThat(m1).isNotEqualTo(m3);
            assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        }
    }
}
