package com.cafepos.organization.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CafeRepository extends JpaRepository<Cafe, UUID> {

    Optional<Cafe> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
