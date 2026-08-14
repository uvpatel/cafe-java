package com.cafepos.floor.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiningTableTest {

    private final UUID cafeId = UUID.randomUUID();
    private final UUID floorId = UUID.randomUUID();

    @Nested
    @DisplayName("Creation & Validation")
    class CreationAndValidation {

        @Test
        @DisplayName("Should create dining table with default seating capacity and active state")
        void createDiningTableDefaults() {
            DiningTable table = new DiningTable(cafeId, floorId, "T-1");

            assertThat(table.getId()).isNotNull();
            assertThat(table.getCafeId()).isEqualTo(cafeId);
            assertThat(table.getFloorId()).isEqualTo(floorId);
            assertThat(table.getTableNumber()).isEqualTo("T-1");
            assertThat(table.getSeatingCapacity()).isEqualTo(4);
            assertThat(table.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should create dining table with custom parameters")
        void createDiningTableCustom() {
            UUID id = UUID.randomUUID();
            DiningTable table = new DiningTable(id, cafeId, floorId, " T-10 ", 6, false);

            assertThat(table.getId()).isEqualTo(id);
            assertThat(table.getTableNumber()).isEqualTo("T-10");
            assertThat(table.getSeatingCapacity()).isEqualTo(6);
            assertThat(table.isActive()).isFalse();
        }

        @Test
        @DisplayName("Should throw exception when cafe ID is null")
        void nullCafeIdThrowsException() {
            assertThatThrownBy(() -> new DiningTable(null, floorId, "T-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cafe ID must not be null");
        }

        @Test
        @DisplayName("Should throw exception when floor ID is null")
        void nullFloorIdThrowsException() {
            assertThatThrownBy(() -> new DiningTable(cafeId, null, "T-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Floor ID must not be null");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Should throw exception when table number is empty or blank")
        void emptyTableNumberThrowsException(String tableNumber) {
            assertThatThrownBy(() -> new DiningTable(cafeId, floorId, tableNumber))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Table number must not be empty");
        }

        @Test
        @DisplayName("Should throw exception when table number is null")
        void nullTableNumberThrowsException() {
            assertThatThrownBy(() -> new DiningTable(cafeId, floorId, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Table number must not be empty");
        }

        @Test
        @DisplayName("Should throw exception when table number exceeds max length")
        void tableNumberExceedsLengthThrowsException() {
            String longNumber = "T".repeat(31);

            assertThatThrownBy(() -> new DiningTable(cafeId, floorId, longNumber))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Table number must not exceed 30 characters");
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, -5})
        @DisplayName("Should throw exception when seating capacity is non-positive")
        void invalidSeatingCapacityThrowsException(int capacity) {
            assertThatThrownBy(() -> new DiningTable(cafeId, floorId, "T-1", capacity))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Seating capacity must be greater than 0");
        }
    }

    @Nested
    @DisplayName("State Modifications & Equality")
    class StateModificationsAndEquality {

        @Test
        @DisplayName("Should update dining table attributes")
        void updateDiningTable() {
            DiningTable table = new DiningTable(cafeId, floorId, "T-1", 2);
            UUID newFloorId = UUID.randomUUID();

            table.update(newFloorId, "T-01", 8);

            assertThat(table.getFloorId()).isEqualTo(newFloorId);
            assertThat(table.getTableNumber()).isEqualTo("T-01");
            assertThat(table.getSeatingCapacity()).isEqualTo(8);
        }

        @Test
        @DisplayName("Should activate and deactivate dining table")
        void activateDeactivate() {
            DiningTable table = new DiningTable(cafeId, floorId, "T-1");
            assertThat(table.isActive()).isTrue();

            table.deactivate();
            assertThat(table.isActive()).isFalse();

            table.activate();
            assertThat(table.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should verify equality based on ID")
        void equalsAndHashCode() {
            UUID id = UUID.randomUUID();
            DiningTable table1 = new DiningTable(id, cafeId, floorId, "T-1", 4, true);
            DiningTable table2 = new DiningTable(id, cafeId, floorId, "T-2", 2, false);
            DiningTable table3 = new DiningTable(UUID.randomUUID(), cafeId, floorId, "T-1", 4, true);

            assertThat(table1).isEqualTo(table2);
            assertThat(table1.hashCode()).isEqualTo(table2.hashCode());
            assertThat(table1).isNotEqualTo(table3);
        }
    }
}
