package com.cafepos.identity.application;

import com.cafepos.identity.api.EmployeeShiftResponse;
import com.cafepos.identity.api.ShiftDtoMapper;
import com.cafepos.identity.domain.EmployeeShift;
import com.cafepos.identity.domain.EmployeeShiftRepository;
import com.cafepos.identity.domain.ShiftStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class ShiftApplicationService {

    private static final List<ShiftStatus> ACTIVE_STATUSES = List.of(ShiftStatus.ACTIVE, ShiftStatus.ON_BREAK);

    private final EmployeeShiftRepository shiftRepository;
    private final ShiftDtoMapper mapper;

    public ShiftApplicationService(EmployeeShiftRepository shiftRepository, ShiftDtoMapper mapper) {
        this.shiftRepository = Objects.requireNonNull(shiftRepository, "shiftRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    public EmployeeShiftResponse clockIn(UUID cafeId, UUID employeeId, String notes) {
        shiftRepository.findByCafeIdAndEmployeeIdAndStatusIn(cafeId, employeeId, ACTIVE_STATUSES)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Employee already has an active shift in progress");
                });

        EmployeeShift shift = new EmployeeShift(cafeId, employeeId, notes);
        EmployeeShift saved = shiftRepository.save(shift);
        return mapper.toResponse(saved);
    }

    public EmployeeShiftResponse startBreak(UUID cafeId, UUID employeeId) {
        EmployeeShift shift = getActiveShift(cafeId, employeeId);
        shift.startBreak();
        EmployeeShift saved = shiftRepository.save(shift);
        return mapper.toResponse(saved);
    }

    public EmployeeShiftResponse endBreak(UUID cafeId, UUID employeeId) {
        EmployeeShift shift = getActiveShift(cafeId, employeeId);
        shift.endBreak();
        EmployeeShift saved = shiftRepository.save(shift);
        return mapper.toResponse(saved);
    }

    public EmployeeShiftResponse clockOut(UUID cafeId, UUID employeeId) {
        EmployeeShift shift = getActiveShift(cafeId, employeeId);
        shift.clockOut();
        EmployeeShift saved = shiftRepository.save(shift);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EmployeeShiftResponse> getShiftsForEmployee(UUID cafeId, UUID employeeId) {
        return shiftRepository.findByCafeIdAndEmployeeId(cafeId, employeeId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    private EmployeeShift getActiveShift(UUID cafeId, UUID employeeId) {
        return shiftRepository.findByCafeIdAndEmployeeIdAndStatusIn(cafeId, employeeId, ACTIVE_STATUSES)
                .orElseThrow(() -> new IllegalArgumentException("No active shift found for employee ID: " + employeeId));
    }
}
