package com.cafepos.organization.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CafeTest {

    @Nested
    @DisplayName("Creation & Defaults")
    class CreationTests {

        @Test
        @DisplayName("Should create Cafe entity with valid details")
        void shouldCreateCafeWithValidDetails() {
            UUID id = UUID.randomUUID();
            Cafe cafe = new Cafe(id, "Artisan Roasters", "artisan-roasters", "Asia/Kolkata", "INR");

            assertThat(cafe.getId()).isEqualTo(id);
            assertThat(cafe.getName()).isEqualTo("Artisan Roasters");
            assertThat(cafe.getSlug()).isEqualTo("artisan-roasters");
            assertThat(cafe.getTimezone()).isEqualTo("Asia/Kolkata");
            assertThat(cafe.getCurrency()).isEqualTo("INR");
        }

        @Test
        @DisplayName("Should generate UUID and convert slug to lower case when provided")
        void shouldNormalizeSlugAndGenerateId() {
            Cafe cafe = new Cafe(null, " Cafe Central ", " Cafe-Central ", null, null);

            assertThat(cafe.getId()).isNotNull();
            assertThat(cafe.getName()).isEqualTo("Cafe Central");
            assertThat(cafe.getSlug()).isEqualTo("cafe-central");
            assertThat(cafe.getTimezone()).isEqualTo("UTC");
            assertThat(cafe.getCurrency()).isEqualTo("USD");
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Should reject blank cafe name")
        void shouldRejectBlankName(String invalidName) {
            assertThatThrownBy(() -> new Cafe(UUID.randomUUID(), invalidName, "valid-slug", "UTC", "USD"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cafe name must not be empty");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Should reject blank cafe slug")
        void shouldRejectBlankSlug(String invalidSlug) {
            assertThatThrownBy(() -> new Cafe(UUID.randomUUID(), "Valid Name", invalidSlug, "UTC", "USD"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cafe slug must not be empty");
        }

        @Test
        @DisplayName("Should validate updates to entity properties")
        void shouldValidateUpdates() {
            Cafe cafe = new Cafe(UUID.randomUUID(), "Original Name", "original-slug", "UTC", "USD");

            cafe.setName(" Updated Name ");
            assertThat(cafe.getName()).isEqualTo("Updated Name");

            cafe.setSlug(" UPDATED-SLUG ");
            assertThat(cafe.getSlug()).isEqualTo("updated-slug");

            cafe.setTimezone(" America/New_York ");
            assertThat(cafe.getTimezone()).isEqualTo("America/New_York");

            cafe.setCurrency(" eur ");
            assertThat(cafe.getCurrency()).isEqualTo("EUR");

            assertThatThrownBy(() -> cafe.setName(""))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> cafe.setSlug(""))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> cafe.setTimezone("  "))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> cafe.setCurrency(""))
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
            Cafe cafe1 = new Cafe(id, "Name 1", "slug-1", "UTC", "USD");
            Cafe cafe2 = new Cafe(id, "Name 2", "slug-2", "Asia/Kolkata", "INR");

            assertThat(cafe1).isEqualTo(cafe2);
            assertThat(cafe1.hashCode()).isEqualTo(cafe2.hashCode());
        }
    }
}
