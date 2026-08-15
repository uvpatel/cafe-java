package com.cafepos.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModifierGroupRepository extends JpaRepository<ModifierGroup, UUID> {

    List<ModifierGroup> findByCafeId(UUID cafeId);

    Optional<ModifierGroup> findByIdAndCafeId(UUID id, UUID cafeId);
}
