package com.cafepos.session.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TerminalTest {

    private final UUID cafeId = UUID.randomUUID();

    @Nested
    @DisplayName("Creation & Validation")
    class CreationAndValidation {

        @Test
        @DisplayName("Should create terminal with valid attributes")
        void createTerminalSuccess() {
            Terminal terminal = new Terminal(cafeId, "Counter Terminal", "POS-01");

            assertThat(terminal.getId()).isNotNull();
            assertThat(terminal.getCafeId()).isEqualTo(cafeId);
            assertThat(terminal.getName()).isEqualTo("Counter Terminal");
            assertThat(terminal.getCode()).isEqualTo("POS-01");
            assertThat(terminal.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should trim name and code on creation")
        void createTerminalTrimsInputs() {
            Terminal terminal = new Terminal(cafeId, "  Drive Thru  ", "  POS-02  ");

            assertThat(terminal.getName()).isEqualTo("Drive Thru");
            assertThat(terminal.getCode()).isEqualTo("POS-02");
        }

        @Test
        @DisplayName("Should throw exception when cafe ID is null")
        void nullCafeIdThrowsException() {
            assertThatThrownBy(() -> new Terminal(null, "Counter Terminal", "POS-01"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cafe ID must not be null");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Should throw exception when name is empty or blank")
        void emptyNameThrowsException(String name) {
            assertThatThrownBy(() -> new Terminal(cafeId, name, "POS-01"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Terminal name must not be empty");
        }

        @Test
        @DisplayName("Should throw exception when name is null")
        void nullNameThrowsException() {
            assertThatThrownBy(() -> new Terminal(cafeId, null, "POS-01"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Terminal name must not be empty");
        }

        @Test
        @DisplayName("Should throw exception when name exceeds max length")
        void nameExceedsLengthThrowsException() {
            String longName = "T".repeat(101);

            assertThatThrownBy(() -> new Terminal(cafeId, longName, "POS-01"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Terminal name must not exceed 100 characters");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Should throw exception when code is empty or blank")
        void emptyCodeThrowsException(String code) {
            assertThatThrownBy(() -> new Terminal(cafeId, "Counter Terminal", code))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Terminal code must not be empty");
        }

        @Test
        @DisplayName("Should throw exception when code is null")
        void nullCodeThrowsException() {
            assertThatThrownBy(() -> new Terminal(cafeId, "Counter Terminal", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Terminal code must not be empty");
        }

        @Test
        @DisplayName("Should throw exception when code exceeds max length")
        void codeExceedsLengthThrowsException() {
            String longCode = "C".repeat(51);

            assertThatThrownBy(() -> new Terminal(cafeId, "Counter Terminal", longCode))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Terminal code must not exceed 50 characters");
        }
    }

    @Nested
    @DisplayName("State Modifications & Equality")
    class StateModificationsAndEquality {

        @Test
        @DisplayName("Should update terminal name and code")
        void updateTerminalSuccess() {
            Terminal terminal = new Terminal(cafeId, "Old Name", "POS-01");
            terminal.update("New Name", "POS-01-REV");

            assertThat(terminal.getName()).isEqualTo("New Name");
            assertThat(terminal.getCode()).isEqualTo("POS-01-REV");
        }

        @Test
        @DisplayName("Should activate and deactivate terminal")
        void activateDeactivate() {
            Terminal terminal = new Terminal(cafeId, "Counter Terminal", "POS-01");
            assertThat(terminal.isActive()).isTrue();

            terminal.deactivate();
            assertThat(terminal.isActive()).isFalse();

            terminal.activate();
            assertThat(terminal.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should verify equality based on ID")
        void equalsAndHashCode() {
            UUID id = UUID.randomUUID();
            Terminal term1 = new Terminal(id, cafeId, "Terminal 1", "P1", true);
            Terminal term2 = new Terminal(id, cafeId, "Terminal 2", "P2", false);
            Terminal term3 = new Terminal(UUID.randomUUID(), cafeId, "Terminal 1", "P1", true);

            assertThat(term1).isEqualTo(term2);
            assertThat(term1.hashCode()).isEqualTo(term2.hashCode());
            assertThat(term1).isNotEqualTo(term3);
        }
    }
}
