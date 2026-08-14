package com.cafepos.catalog.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductCategoryTest {

    @Nested
    @DisplayName("Creation & Defaults")
    class CreationTests {

        @Test
        @DisplayName("Should create ProductCategory with valid details")
        void shouldCreateCategoryWithValidDetails() {
            UUID id = UUID.randomUUID();
            UUID cafeId = UUID.randomUUID();
            ProductCategory category = new ProductCategory(id, cafeId, "Beverages", "#FF5733", 10);

            assertThat(category.getId()).isEqualTo(id);
            assertThat(category.getCafeId()).isEqualTo(cafeId);
            assertThat(category.getName()).isEqualTo("Beverages");
            assertThat(category.getColorHex()).isEqualTo("#FF5733");
            assertThat(category.getSortOrder()).isEqualTo(10);
            assertThat(category.isArchived()).isFalse();
        }

        @Test
        @DisplayName("Should use default color and sort order when omitted")
        void shouldUseDefaults() {
            UUID cafeId = UUID.randomUUID();
            ProductCategory category = new ProductCategory(cafeId, "Desserts");

            assertThat(category.getId()).isNotNull();
            assertThat(category.getCafeId()).isEqualTo(cafeId);
            assertThat(category.getName()).isEqualTo("Desserts");
            assertThat(category.getColorHex()).isEqualTo(ProductCategory.DEFAULT_COLOR_HEX);
            assertThat(category.getSortOrder()).isEqualTo(0);
            assertThat(category.isArchived()).isFalse();
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("Should reject null cafe ID")
        void shouldRejectNullCafeId() {
            assertThatThrownBy(() -> new ProductCategory(null, null, "Beverages", "#123456", 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cafe ID must not be null");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Should reject empty category name")
        void shouldRejectEmptyName(String invalidName) {
            assertThatThrownBy(() -> new ProductCategory(UUID.randomUUID(), invalidName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Category name must not be empty");
        }

        @Test
        @DisplayName("Should reject category name exceeding 100 characters")
        void shouldRejectOverlongName() {
            String longName = "A".repeat(101);
            assertThatThrownBy(() -> new ProductCategory(UUID.randomUUID(), longName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Category name must not exceed 100 characters");
        }

        @ParameterizedTest
        @ValueSource(strings = {"123456", "#12345", "#1234567", "#GGGGGG", "blue"})
        @DisplayName("Should reject invalid color hex format")
        void shouldRejectInvalidColorHex(String invalidColor) {
            assertThatThrownBy(() -> new ProductCategory(UUID.randomUUID(), "Snacks", invalidColor, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Color hex must be a valid 7-character hex string");
        }
    }

    @Nested
    @DisplayName("Domain Operations & State Mutations")
    class DomainOperationTests {

        @Test
        @DisplayName("Should update category name, color, and sort order")
        void shouldUpdateCategory() {
            ProductCategory category = new ProductCategory(UUID.randomUUID(), "Main Course", "#112233", 5);

            category.update("Entrees", "#445566", 2);

            assertThat(category.getName()).isEqualTo("Entrees");
            assertThat(category.getColorHex()).isEqualTo("#445566");
            assertThat(category.getSortOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should archive and unarchive category")
        void shouldArchiveAndUnarchive() {
            ProductCategory category = new ProductCategory(UUID.randomUUID(), "Specials");

            assertThat(category.isArchived()).isFalse();

            category.archive();
            assertThat(category.isArchived()).isTrue();

            category.unarchive();
            assertThat(category.isArchived()).isFalse();

            category.setArchived(true);
            assertThat(category.isArchived()).isTrue();
        }

        @Test
        @DisplayName("Should validate name on setName")
        void shouldValidateOnSetName() {
            ProductCategory category = new ProductCategory(UUID.randomUUID(), "Hot Drinks");

            category.setName("Warm Drinks");
            assertThat(category.getName()).isEqualTo("Warm Drinks");

            assertThatThrownBy(() -> category.setName(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should validate color hex on setColorHex")
        void shouldValidateOnSetColorHex() {
            ProductCategory category = new ProductCategory(UUID.randomUUID(), "Cold Drinks");

            category.setColorHex("#001122");
            assertThat(category.getColorHex()).isEqualTo("#001122");

            assertThatThrownBy(() -> category.setColorHex("invalid"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Equals & HashCode")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal if IDs match")
        void shouldBeEqualIfIdsMatch() {
            UUID id = UUID.randomUUID();
            UUID cafeId = UUID.randomUUID();

            ProductCategory cat1 = new ProductCategory(id, cafeId, "Cat 1", "#111111", 1);
            ProductCategory cat2 = new ProductCategory(id, cafeId, "Cat 2", "#222222", 2);

            assertThat(cat1).isEqualTo(cat2);
            assertThat(cat1.hashCode()).isEqualTo(cat2.hashCode());
        }
    }
}
