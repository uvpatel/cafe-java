package com.cafepos.identity.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    List<Employee> findByCafeId(UUID cafeId);

    Optional<Employee> findByCafeIdAndEmail(UUID cafeId, String email);

    boolean existsByCafeIdAndEmail(UUID cafeId, String email);
}
