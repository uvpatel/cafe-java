package com.cafepos.floor.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FloorTest {

    private final UUID cafeId = UUID.randomUUID();

    @Nested
    @DisplayName("Creation & Validation")
    class CreationAndValidation {

        @Test
        @DisplayName("Should create floor with valid attributes")
        void createFloorSuccess() {
            Floor floor = new Floor(cafeId, "Main Floor", 1);

            assertThat(floor.getId()).isNotNull();
            assertThat(floor.getCafeId()).isEqualTo(cafeId);
            assertThat(floor.getName()).isEqualTo("Main Floor");
            assertThat(floor.getSortOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should trim name on creation")
        void createFloorTrimsName() {
            Floor floor = new Floor(cafeId, "  Terrace Floor  ", 2);

            assertThat(floor.getName()).isEqualTo("Terrace Floor");
        }

        @Test
        @DisplayName("Should throw exception when cafe ID is null")
        void nullCafeIdThrowsException() {
            assertThatThrownBy(() -> new Floor(null, "Main Floor"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cafe ID must not be null");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Should throw exception when name is empty or blank")
        void emptyNameThrowsException(String name) {
            assertThatThrownBy(() -> new Floor(cafeId, name))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Floor name must not be empty");
        }

        @Test
        @DisplayName("Should throw exception when name is null")
        void nullNameThrowsException() {
            assertThatThrownBy(() -> new Floor(cafeId, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Floor name must not be empty");
        }

        @Test
        @DisplayName("Should throw exception when name exceeds max length")
        void nameExceedsLengthThrowsException() {
            String longName = "A".repeat(101);

            assertThatThrownBy(() -> new Floor(cafeId, longName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Floor name must not exceed 100 characters");
        }
    }

    @Nested
    @DisplayName("State Modifications & Equality")
    class StateModificationsAndEquality {

        @Test
        @DisplayName("Should update floor name and sort order")
        void updateFloorSuccess() {
            Floor floor = new Floor(cafeId, "Old Name", 1);
            floor.update("New Name", 5);

            assertThat(floor.getName()).isEqualTo("New Name");
            assertThat(floor.getSortOrder()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should verify equality based on ID")
        void equalsAndHashCode() {
            UUID id = UUID.randomUUID();
            Floor floor1 = new Floor(id, cafeId, "Floor 1", 1);
            Floor floor2 = new Floor(id, cafeId, "Floor 2", 2);
            Floor floor3 = new Floor(UUID.randomUUID(), cafeId, "Floor 1", 1);

            assertThat(floor1).isEqualTo(floor2);
            assertThat(floor1.hashCode()).isEqualTo(floor2.hashCode());
            assertThat(floor1).isNotEqualTo(floor3);
        }
    }
}
