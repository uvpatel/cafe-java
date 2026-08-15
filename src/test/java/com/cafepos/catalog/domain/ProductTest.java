package com.cafepos.catalog.domain;

import com.cafepos.shared.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Nested
    @DisplayName("Creation & Defaults")
    class CreationTests {

        @Test
        @DisplayName("Should create Product with all valid fields")
        void shouldCreateProductWithAllFields() {
            UUID id = UUID.randomUUID();
            UUID cafeId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            Money price = Money.of(4.50);

            Product product = new Product(id, cafeId, categoryId, "Espresso", "ESP-001", price, UnitOfMeasure.UNIT, true);

            assertThat(product.getId()).isEqualTo(id);
            assertThat(product.getCafeId()).isEqualTo(cafeId);
            assertThat(product.getCategoryId()).isEqualTo(categoryId);
            assertThat(product.getName()).isEqualTo("Espresso");
            assertThat(product.getSku()).isEqualTo("ESP-001");
            assertThat(product.getPrice()).isEqualTo(price);
            assertThat(product.getUnitOfMeasure()).isEqualTo(UnitOfMeasure.UNIT);
            assertThat(product.isKitchenRouted()).isTrue();
            assertThat(product.isArchived()).isFalse();
        }

        @Test
        @DisplayName("Should use default unit of measure and kitchen routing when using convenience constructor")
        void shouldUseDefaults() {
            UUID cafeId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            Money price = Money.of(3.00);

            Product product = new Product(cafeId, categoryId, "Croissant", price);

            assertThat(product.getId()).isNotNull();
            assertThat(product.getSku()).isNull();
            assertThat(product.getUnitOfMeasure()).isEqualTo(UnitOfMeasure.UNIT);
            assertThat(product.isKitchenRouted()).isTrue();
            assertThat(product.isArchived()).isFalse();
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("Should reject null cafe ID")
        void shouldRejectNullCafeId() {
            assertThatThrownBy(() -> new Product(null, UUID.randomUUID(), "Latte", Money.of(5.00)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cafe ID must not be null");
        }

        @Test
        @DisplayName("Should reject null category ID")
        void shouldRejectNullCategoryId() {
            assertThatThrownBy(() -> new Product(UUID.randomUUID(), null, "Latte", Money.of(5.00)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Category ID must not be null");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Should reject empty product name")
        void shouldRejectEmptyName(String invalidName) {
            assertThatThrownBy(() -> new Product(UUID.randomUUID(), UUID.randomUUID(), invalidName, Money.of(5.00)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Product name must not be empty");
        }

        @Test
        @DisplayName("Should reject product name exceeding 100 characters")
        void shouldRejectOverlongName() {
            String longName = "A".repeat(101);
            assertThatThrownBy(() -> new Product(UUID.randomUUID(), UUID.randomUUID(), longName, Money.of(5.00)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Product name must not exceed 100 characters");
        }

        @Test
        @DisplayName("Should reject null price")
        void shouldRejectNullPrice() {
            assertThatThrownBy(() -> new Product(UUID.randomUUID(), UUID.randomUUID(), "Cappuccino", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Price must not be null");
        }

        @Test
        @DisplayName("Should reject negative price amount")
        void shouldRejectNegativePrice() {
            Money negativePrice = new Money(BigDecimal.valueOf(-1.50), Currency.getInstance("USD"));
            assertThatThrownBy(() -> new Product(UUID.randomUUID(), UUID.randomUUID(), "Mocha", negativePrice))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Price amount cannot be negative");
        }
    }

    @Nested
    @DisplayName("Domain Operations & State Mutations")
    class DomainOperationTests {

        @Test
        @DisplayName("Should update product details")
        void shouldUpdateProductDetails() {
            Product product = new Product(UUID.randomUUID(), UUID.randomUUID(), "Iced Tea", Money.of(2.50));
            UUID newCatId = UUID.randomUUID();
            Money newPrice = Money.of(3.50);

            product.updateDetails("Iced Green Tea", "TEA-002", newCatId, newPrice, UnitOfMeasure.SERVING, false);

            assertThat(product.getName()).isEqualTo("Iced Green Tea");
            assertThat(product.getSku()).isEqualTo("TEA-002");
            assertThat(product.getCategoryId()).isEqualTo(newCatId);
            assertThat(product.getPrice()).isEqualTo(newPrice);
            assertThat(product.getUnitOfMeasure()).isEqualTo(UnitOfMeasure.SERVING);
            assertThat(product.isKitchenRouted()).isFalse();
        }

        @Test
        @DisplayName("Should archive and unarchive product")
        void shouldArchiveAndUnarchive() {
            Product product = new Product(UUID.randomUUID(), UUID.randomUUID(), "Bagel", Money.of(2.00));

            assertThat(product.isArchived()).isFalse();

            product.archive();
            assertThat(product.isArchived()).isTrue();

            product.unarchive();
            assertThat(product.isArchived()).isFalse();
        }

        @Test
        @DisplayName("Should update price and validate non-negative constraint")
        void shouldUpdatePriceWithValidation() {
            Product product = new Product(UUID.randomUUID(), UUID.randomUUID(), "Muffin", Money.of(3.00));

            product.setPrice(Money.of(3.50));
            assertThat(product.getPrice()).isEqualTo(Money.of(3.50));

            Money negativePrice = new Money(BigDecimal.valueOf(-2.00), Currency.getInstance("USD"));
            assertThatThrownBy(() -> product.setPrice(negativePrice))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should attach and detach modifier groups")
        void shouldAttachAndDetachModifierGroups() {
            Product product = new Product(UUID.randomUUID(), UUID.randomUUID(), "Latte", Money.of(4.00));
            UUID groupId = UUID.randomUUID();

            assertThat(product.getModifierGroupIds()).isEmpty();

            product.attachModifierGroup(groupId);
            assertThat(product.getModifierGroupIds()).containsExactly(groupId);

            product.detachModifierGroup(groupId);
            assertThat(product.getModifierGroupIds()).isEmpty();
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
            UUID catId = UUID.randomUUID();

            Product prod1 = new Product(id, cafeId, catId, "P1", "SKU1", Money.of(5.0), UnitOfMeasure.UNIT, true);
            Product prod2 = new Product(id, cafeId, catId, "P2", "SKU2", Money.of(10.0), UnitOfMeasure.KG, false);

            assertThat(prod1).isEqualTo(prod2);
            assertThat(prod1.hashCode()).isEqualTo(prod2.hashCode());
        }
    }
}
