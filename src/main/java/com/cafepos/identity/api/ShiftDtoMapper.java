package com.cafepos.identity.api;

import com.cafepos.identity.domain.EmployeeShift;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ShiftDtoMapper {

    public EmployeeShiftResponse toResponse(EmployeeShift shift) {
        Objects.requireNonNull(shift, "shift must not be null");
        return new EmployeeShiftResponse(
                shift.getId(),
                shift.getCafeId(),
                shift.getEmployeeId(),
                shift.getClockInAt(),
                shift.getClockOutAt(),
                shift.getBreakStartAt(),
                shift.getBreakEndAt(),
                shift.getStatus(),
                shift.getNotes(),
                shift.getTotalWorkedDuration().toMinutes(),
                shift.getCreatedAt(),
                shift.getUpdatedAt()
        );
    }
}
