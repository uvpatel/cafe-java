package com.cafepos.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByCafeId(UUID cafeId);

    List<Product> findByCategoryId(UUID categoryId);

    List<Product> findByCafeIdAndCategoryId(UUID cafeId, UUID categoryId);

    List<Product> findByCafeIdAndArchivedFalse(UUID cafeId);

    Optional<Product> findByIdAndCafeId(UUID id, UUID cafeId);

    Optional<Product> findByCafeIdAndSku(UUID cafeId, String sku);

    boolean existsByCafeIdAndSku(UUID cafeId, String sku);
}
