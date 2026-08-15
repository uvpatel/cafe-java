package com.cafepos.identity.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeShiftRepository extends JpaRepository<EmployeeShift, UUID> {
    List<EmployeeShift> findByCafeIdAndEmployeeId(UUID cafeId, UUID employeeId);
    Optional<EmployeeShift> findByCafeIdAndEmployeeIdAndStatusIn(UUID cafeId, UUID employeeId, List<ShiftStatus> statuses);
    List<EmployeeShift> findByCafeId(UUID cafeId);
}
