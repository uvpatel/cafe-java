package com.cafepos.floor.application;

import com.cafepos.floor.domain.DiningTable;
import com.cafepos.floor.domain.DiningTableRepository;
import com.cafepos.shared.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FloorApplicationServiceTest {

    @Mock
    private DiningTableRepository diningTableRepository;

    private FloorApplicationService floorApplicationService;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID floorId = UUID.randomUUID();
    private final UUID sourceTableId = UUID.randomUUID();
    private final UUID targetTableId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        floorApplicationService = new FloorApplicationService(diningTableRepository);
    }

    @Test
    @DisplayName("Should successfully merge source table into target table")
    void shouldMergeTablesSuccessfully() {
        DiningTable sourceTable = new DiningTable(sourceTableId, cafeId, floorId, "T-1", 4, true);
        DiningTable targetTable = new DiningTable(targetTableId, cafeId, floorId, "T-2", 6, true);

        when(diningTableRepository.findByIdAndCafeId(sourceTableId, cafeId)).thenReturn(Optional.of(sourceTable));
        when(diningTableRepository.findByIdAndCafeId(targetTableId, cafeId)).thenReturn(Optional.of(targetTable));
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(i -> i.getArgument(0));

        DiningTable result = floorApplicationService.mergeTables(cafeId, sourceTableId, targetTableId);

        assertThat(result.isMerged()).isTrue();
        assertThat(result.getMergedIntoTableId()).isEqualTo(targetTableId);
        verify(diningTableRepository).save(sourceTable);
    }

    @Test
    @DisplayName("Should throw BusinessException when trying to merge table into itself")
    void shouldThrowWhenMergingTableIntoItself() {
        assertThatThrownBy(() -> floorApplicationService.mergeTables(cafeId, sourceTableId, sourceTableId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot merge table into itself")
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(400);
    }

    @Test
    @DisplayName("Should successfully unmerge table")
    void shouldUnmergeTableSuccessfully() {
        DiningTable sourceTable = new DiningTable(sourceTableId, cafeId, floorId, "T-1", 4, true);
        sourceTable.mergeInto(targetTableId);

        when(diningTableRepository.findByIdAndCafeId(sourceTableId, cafeId)).thenReturn(Optional.of(sourceTable));
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(i -> i.getArgument(0));

        DiningTable result = floorApplicationService.unmergeTable(cafeId, sourceTableId);

        assertThat(result.isMerged()).isFalse();
        assertThat(result.getMergedIntoTableId()).isNull();
        verify(diningTableRepository).save(sourceTable);
    }

    @Test
    @DisplayName("Should get merged child tables for target table")
    void shouldGetMergedTablesForTarget() {
        DiningTable childTable = new DiningTable(sourceTableId, cafeId, floorId, "T-1", 4, true);
        childTable.mergeInto(targetTableId);

        when(diningTableRepository.findByCafeIdAndMergedIntoTableId(cafeId, targetTableId)).thenReturn(List.of(childTable));

        List<DiningTable> mergedTables = floorApplicationService.getMergedTablesForTarget(cafeId, targetTableId);

        assertThat(mergedTables).hasSize(1);
        assertThat(mergedTables.get(0).getId()).isEqualTo(sourceTableId);
    }
}
