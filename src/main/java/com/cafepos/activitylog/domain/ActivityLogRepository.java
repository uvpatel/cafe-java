package com.cafepos.activitylog.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

    Page<ActivityLog> findByCafeId(UUID cafeId, Pageable pageable);

    Page<ActivityLog> findByCafeIdOrderByCreatedAtDesc(UUID cafeId, Pageable pageable);

    Page<ActivityLog> findByCafeIdAndAction(UUID cafeId, String action, Pageable pageable);

    Page<ActivityLog> findByCafeIdAndEntityType(UUID cafeId, String entityType, Pageable pageable);
}
