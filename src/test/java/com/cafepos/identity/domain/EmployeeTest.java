package com.cafepos.identity.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmployeeTest {

    private final UUID cafeId = UUID.randomUUID();

    @Nested
    @DisplayName("Creation & Defaults")
    class CreationTests {

        @Test
        @DisplayName("Should create Employee entity with valid details")
        void shouldCreateEmployeeWithValidDetails() {
            UUID empId = UUID.randomUUID();
            Employee employee = new Employee(empId, cafeId, "Alice Smith", "ALICE@EXAMPLE.COM", EmployeeRole.CASHIER);

            assertThat(employee.getId()).isEqualTo(empId);
            assertThat(employee.getCafeId()).isEqualTo(cafeId);
            assertThat(employee.getFullName()).isEqualTo("Alice Smith");
            assertThat(employee.getEmail()).isEqualTo("alice@example.com");
            assertThat(employee.getRole()).isEqualTo(EmployeeRole.CASHIER);
            assertThat(employee.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should generate UUID if null")
        void shouldGenerateIdIfNull() {
            Employee employee = new Employee(null, cafeId, "Bob Jones", "bob@example.com", EmployeeRole.OWNER);

            assertThat(employee.getId()).isNotNull();
            assertThat(employee.getRole()).isEqualTo(EmployeeRole.OWNER);
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("Should reject null cafe ID")
        void shouldRejectNullCafeId() {
            assertThatThrownBy(() -> new Employee(UUID.randomUUID(), null, "John Doe", "john@example.com", EmployeeRole.ADMIN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cafe ID must not be null");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Should reject blank full name")
        void shouldRejectBlankFullName(String invalidName) {
            assertThatThrownBy(() -> new Employee(UUID.randomUUID(), cafeId, invalidName, "john@example.com", EmployeeRole.ADMIN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee full name must not be empty");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "invalid-email"})
        @DisplayName("Should reject invalid email format")
        void shouldRejectInvalidEmail(String invalidEmail) {
            assertThatThrownBy(() -> new Employee(UUID.randomUUID(), cafeId, "John Doe", invalidEmail, EmployeeRole.ADMIN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee email must be valid and non-empty");
        }

        @Test
        @DisplayName("Should reject null role")
        void shouldRejectNullRole() {
            assertThatThrownBy(() -> new Employee(UUID.randomUUID(), cafeId, "John Doe", "john@example.com", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee role must not be null");
        }

        @Test
        @DisplayName("Should validate property setters")
        void shouldValidatePropertySetters() {
            Employee employee = new Employee(UUID.randomUUID(), cafeId, "Jane Doe", "jane@example.com", EmployeeRole.KITCHEN_STAFF);

            employee.setFullName(" Jane Smith ");
            assertThat(employee.getFullName()).isEqualTo("Jane Smith");

            employee.setEmail(" JANE.SMITH@EXAMPLE.COM ");
            assertThat(employee.getEmail()).isEqualTo("jane.smith@example.com");

            employee.setRole(EmployeeRole.MANAGER);
            assertThat(employee.getRole()).isEqualTo(EmployeeRole.MANAGER);

            employee.setActive(false);
            assertThat(employee.isActive()).isFalse();

            assertThatThrownBy(() -> employee.setFullName(" "))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> employee.setEmail("no-at-sign"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> employee.setRole(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Equals and HashCode")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal if IDs match")
        void shouldBeEqualIfIdsMatch() {
            UUID id = UUID.randomUUID();
            Employee emp1 = new Employee(id, cafeId, "Name 1", "email1@example.com", EmployeeRole.CASHIER);
            Employee emp2 = new Employee(id, cafeId, "Name 2", "email2@example.com", EmployeeRole.MANAGER);

            assertThat(emp1).isEqualTo(emp2);
            assertThat(emp1.hashCode()).isEqualTo(emp2.hashCode());
        }
    }
}
