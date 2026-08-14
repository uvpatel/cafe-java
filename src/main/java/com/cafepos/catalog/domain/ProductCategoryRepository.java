package com.cafepos.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {

    List<ProductCategory> findByCafeId(UUID cafeId);

    List<ProductCategory> findByCafeIdOrderBySortOrderAsc(UUID cafeId);

    List<ProductCategory> findByCafeIdAndArchivedFalseOrderBySortOrderAsc(UUID cafeId);

    Optional<ProductCategory> findByIdAndCafeId(UUID id, UUID cafeId);

    boolean existsByCafeIdAndNameIgnoreCase(UUID cafeId, String name);
}
