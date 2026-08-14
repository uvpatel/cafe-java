package com.cafepos.session.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PosSessionTest {

    private final UUID cafeId = UUID.randomUUID();
    private final UUID terminalId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();

    @Nested
    @DisplayName("Session Opening & Invariants")
    class SessionOpeningTests {

        @Test
        @DisplayName("Should successfully open a new session when terminal has no open session")
        void shouldOpenSessionSuccessfully() {
            PosSession session = PosSession.openSession(cafeId, terminalId, employeeId, new BigDecimal("100.0000"), false);

            assertThat(session).isNotNull();
            assertThat(session.getId()).isNotNull();
            assertThat(session.getCafeId()).isEqualTo(cafeId);
            assertThat(session.getTerminalId()).isEqualTo(terminalId);
            assertThat(session.getOpenedByEmployeeId()).isEqualTo(employeeId);
            assertThat(session.getStatus()).isEqualTo(PosSessionStatus.OPEN);
            assertThat(session.getOpeningBalanceAmount()).isEqualTo(new BigDecimal("100.0000"));
            assertThat(session.getOpenedAt()).isNotNull();
            assertThat(session.isOpen()).isTrue();
            assertThat(session.isClosed()).isFalse();
            assertThat(session.getClosedByEmployeeId()).isNull();
            assertThat(session.getClosedAt()).isNull();
        }

        @Test
        @DisplayName("Should reject opening a session if terminal already has an active OPEN session")
        void shouldRejectOpeningIfTerminalAlreadyHasOpenSession() {
            assertThatThrownBy(() -> PosSession.openSession(cafeId, terminalId, employeeId, new BigDecimal("50.00"), true))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Terminal already has an active OPEN session");
        }

        @Test
        @DisplayName("Should validate null parameters on session creation")
        void shouldValidateNullParametersOnCreation() {
            assertThatThrownBy(() -> new PosSession(null, terminalId, employeeId, BigDecimal.TEN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cafe ID must not be null");

            assertThatThrownBy(() -> new PosSession(cafeId, null, employeeId, BigDecimal.TEN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Terminal ID must not be null");

            assertThatThrownBy(() -> new PosSession(cafeId, terminalId, null, BigDecimal.TEN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Opened-by employee ID must not be null");

            assertThatThrownBy(() -> new PosSession(cafeId, terminalId, employeeId, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Opening balance amount must not be null");
        }

        @Test
        @DisplayName("Should reject negative opening balance")
        void shouldRejectNegativeOpeningBalance() {
            assertThatThrownBy(() -> new PosSession(cafeId, terminalId, employeeId, new BigDecimal("-10.00")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Opening balance amount must not be negative");
        }
    }

    @Nested
    @DisplayName("Cash Movements on Session")
    class CashMovementInvariantsTests {

        @Test
        @DisplayName("Should record cash movement on OPEN session successfully")
        void shouldRecordCashMovementOnOpenSession() {
            PosSession session = new PosSession(cafeId, terminalId, employeeId, new BigDecimal("100.00"));
            CashMovement movement = session.recordCashMovement(employeeId, CashMovementType.CASH_IN, new BigDecimal("25.50"), "Mid-day float top up");

            assertThat(movement).isNotNull();
            assertThat(movement.getCafeId()).isEqualTo(cafeId);
            assertThat(movement.getSessionId()).isEqualTo(session.getId());
            assertThat(movement.getEmployeeId()).isEqualTo(employeeId);
            assertThat(movement.getType()).isEqualTo(CashMovementType.CASH_IN);
            assertThat(movement.getAmount()).isEqualTo(new BigDecimal("25.50"));
            assertThat(movement.getReason()).isEqualTo("Mid-day float top up");
        }

        @Test
        @DisplayName("Should reject recording cash movement on CLOSED session")
        void shouldRejectCashMovementOnClosedSession() {
            PosSession session = new PosSession(cafeId, terminalId, employeeId, new BigDecimal("100.00"));
            session.closeSession(employeeId, new BigDecimal("100.00"), new BigDecimal("100.00"));

            assertThatThrownBy(() -> session.recordCashMovement(employeeId, CashMovementType.CASH_OUT, new BigDecimal("20.00"), "Petty cash payout"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cash movement cannot be recorded on a CLOSED session");
        }
    }

    @Nested
    @DisplayName("Session Closing & Variance Calculation")
    class SessionClosingTests {

        @Test
        @DisplayName("Should calculate exact variance and transition to CLOSED on session close")
        void shouldCloseSessionAndCalculateVariance() {
            PosSession session = new PosSession(cafeId, terminalId, employeeId, new BigDecimal("100.0000"));
            UUID closingEmployeeId = UUID.randomUUID();

            BigDecimal expected = new BigDecimal("250.0000");
            BigDecimal counted = new BigDecimal("240.0000"); // 10 short (variance -10.0000)

            session.closeSession(closingEmployeeId, expected, counted);

            assertThat(session.getStatus()).isEqualTo(PosSessionStatus.CLOSED);
            assertThat(session.isClosed()).isTrue();
            assertThat(session.isOpen()).isFalse();
            assertThat(session.getClosedByEmployeeId()).isEqualTo(closingEmployeeId);
            assertThat(session.getClosingExpectedAmount()).isEqualTo(expected);
            assertThat(session.getClosingCountedAmount()).isEqualTo(counted);
            assertThat(session.getClosingVarianceAmount()).isEqualTo(new BigDecimal("-10.0000"));
            assertThat(session.getClosedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should calculate positive variance when counted exceeds expected")
        void shouldCalculatePositiveVariance() {
            PosSession session = new PosSession(cafeId, terminalId, employeeId, new BigDecimal("100.0000"));

            session.closeSession(employeeId, new BigDecimal("200.0000"), new BigDecimal("215.5000"));

            assertThat(session.getClosingVarianceAmount()).isEqualTo(new BigDecimal("15.5000"));
        }

        @Test
        @DisplayName("Should handle idempotency when closing an already closed session")
        void shouldBeIdempotentWhenClosingAlreadyClosedSession() {
            PosSession session = new PosSession(cafeId, terminalId, employeeId, new BigDecimal("100.00"));
            session.closeSession(employeeId, new BigDecimal("150.00"), new BigDecimal("150.00"));

            Instant initialClosedAt = session.getClosedAt();
            assertThat(session.getStatus()).isEqualTo(PosSessionStatus.CLOSED);

            // Re-closing idempotently
            session.closeSession(employeeId, new BigDecimal("150.00"), new BigDecimal("150.00"));

            assertThat(session.getStatus()).isEqualTo(PosSessionStatus.CLOSED);
            assertThat(session.getClosedAt()).isEqualTo(initialClosedAt);
        }

        @Test
        @DisplayName("Should validate arguments on closeSession")
        void shouldValidateCloseSessionArguments() {
            PosSession session = new PosSession(cafeId, terminalId, employeeId, BigDecimal.TEN);

            assertThatThrownBy(() -> session.closeSession(null, BigDecimal.TEN, BigDecimal.TEN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Closed-by employee ID must not be null");

            assertThatThrownBy(() -> session.closeSession(employeeId, null, BigDecimal.TEN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Expected amount must not be null");

            assertThatThrownBy(() -> session.closeSession(employeeId, BigDecimal.TEN, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Counted amount must not be null");

            assertThatThrownBy(() -> session.closeSession(employeeId, new BigDecimal("-1.00"), BigDecimal.TEN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Expected amount must not be negative");

            assertThatThrownBy(() -> session.closeSession(employeeId, BigDecimal.TEN, new BigDecimal("-5.00")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Counted amount must not be negative");
        }
    }

    @Nested
    @DisplayName("Equals and HashCode")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should verify entity equality by ID")
        void shouldVerifyEqualsAndHashCode() {
            UUID sessionId = UUID.randomUUID();
            PosSession s1 = new PosSession(sessionId, cafeId, terminalId, employeeId, BigDecimal.TEN);
            PosSession s2 = new PosSession(sessionId, cafeId, terminalId, employeeId, BigDecimal.TEN);
            PosSession s3 = new PosSession(UUID.randomUUID(), cafeId, terminalId, employeeId, BigDecimal.TEN);

            assertThat(s1).isEqualTo(s2);
            assertThat(s1).isNotEqualTo(s3);
            assertThat(s1.hashCode()).isEqualTo(s2.hashCode());
        }
    }
}
