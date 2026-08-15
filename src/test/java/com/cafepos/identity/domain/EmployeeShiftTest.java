package com.cafepos.identity.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmployeeShiftTest {

    private final UUID cafeId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();

    @Test
    @DisplayName("Should create active employee shift upon clock in")
    void createActiveShift() {
        EmployeeShift shift = new EmployeeShift(cafeId, employeeId, "Morning shift");

        assertThat(shift.getId()).isNotNull();
        assertThat(shift.getCafeId()).isEqualTo(cafeId);
        assertThat(shift.getEmployeeId()).isEqualTo(employeeId);
        assertThat(shift.getStatus()).isEqualTo(ShiftStatus.ACTIVE);
        assertThat(shift.getClockInAt()).isNotNull();
        assertThat(shift.getClockOutAt()).isNull();
        assertThat(shift.getNotes()).isEqualTo("Morning shift");
    }

    @Test
    @DisplayName("Should transition from ACTIVE -> ON_BREAK -> ACTIVE -> COMPLETED")
    void shiftLifecycleTransitions() {
        EmployeeShift shift = new EmployeeShift(cafeId, employeeId, null);

        shift.startBreak();
        assertThat(shift.getStatus()).isEqualTo(ShiftStatus.ON_BREAK);
        assertThat(shift.getBreakStartAt()).isNotNull();

        shift.endBreak();
        assertThat(shift.getStatus()).isEqualTo(ShiftStatus.ACTIVE);
        assertThat(shift.getBreakEndAt()).isNotNull();

        shift.clockOut();
        assertThat(shift.getStatus()).isEqualTo(ShiftStatus.COMPLETED);
        assertThat(shift.getClockOutAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when starting break on non-active shift")
    void startBreakInvalidStatusThrows() {
        EmployeeShift shift = new EmployeeShift(cafeId, employeeId, null);
        shift.clockOut();

        assertThatThrownBy(shift::startBreak)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot start break on shift with status: COMPLETED");
    }
}
