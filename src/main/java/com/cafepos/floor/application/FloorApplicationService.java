package com.cafepos.floor.application;

import com.cafepos.floor.domain.DiningTable;
import com.cafepos.floor.domain.DiningTableRepository;
import com.cafepos.shared.error.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class FloorApplicationService {

    private final DiningTableRepository diningTableRepository;

    public FloorApplicationService(DiningTableRepository diningTableRepository) {
        this.diningTableRepository = Objects.requireNonNull(diningTableRepository, "diningTableRepository must not be null");
    }

    public DiningTable mergeTables(UUID cafeId, UUID sourceTableId, UUID targetTableId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(sourceTableId, "sourceTableId must not be null");
        Objects.requireNonNull(targetTableId, "targetTableId must not be null");

        if (sourceTableId.equals(targetTableId)) {
            throw new BusinessException("INVALID_TABLE_MERGE", "Cannot merge table into itself", 400);
        }

        DiningTable sourceTable = diningTableRepository.findByIdAndCafeId(sourceTableId, cafeId)
                .orElseThrow(() -> new BusinessException("TABLE_NOT_FOUND", "Source dining table not found", 404));

        DiningTable targetTable = diningTableRepository.findByIdAndCafeId(targetTableId, cafeId)
                .orElseThrow(() -> new BusinessException("TABLE_NOT_FOUND", "Target dining table not found", 404));

        if (!sourceTable.isActive() || !targetTable.isActive()) {
            throw new BusinessException("TABLE_INACTIVE", "Cannot merge inactive dining tables", 400);
        }

        sourceTable.mergeInto(targetTable.getId());
        return diningTableRepository.save(sourceTable);
    }

    public DiningTable unmergeTable(UUID cafeId, UUID sourceTableId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(sourceTableId, "sourceTableId must not be null");

        DiningTable sourceTable = diningTableRepository.findByIdAndCafeId(sourceTableId, cafeId)
                .orElseThrow(() -> new BusinessException("TABLE_NOT_FOUND", "Dining table not found", 404));

        sourceTable.unmerge();
        return diningTableRepository.save(sourceTable);
    }

    @Transactional(readOnly = true)
    public List<DiningTable> getMergedTablesForTarget(UUID cafeId, UUID targetTableId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(targetTableId, "targetTableId must not be null");

        return diningTableRepository.findByCafeIdAndMergedIntoTableId(cafeId, targetTableId);
    }
}
