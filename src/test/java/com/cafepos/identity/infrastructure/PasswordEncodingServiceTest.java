package com.cafepos.identity.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordEncodingServiceTest {

    private PasswordEncodingService passwordEncodingService;

    @BeforeEach
    void setUp() {
        passwordEncodingService = new BCryptPasswordEncodingService();
    }

    @Test
    @DisplayName("Should encode password into BCrypt hash")
    void encodePassword() {
        String rawPassword = "securePassword123!";
        String encoded = passwordEncodingService.encode(rawPassword);

        assertThat(encoded).isNotNull();
        assertThat(encoded).isNotEqualTo(rawPassword);
        assertThat(encoded).startsWith("$2a$");
    }

    @Test
    @DisplayName("Should correctly match valid raw password with BCrypt hash")
    void matchValidPassword() {
        String rawPassword = "securePassword123!";
        String encoded = passwordEncodingService.encode(rawPassword);

        assertThat(passwordEncodingService.matches(rawPassword, encoded)).isTrue();
    }

    @Test
    @DisplayName("Should return false when raw password does not match hash")
    void matchInvalidPassword() {
        String rawPassword = "securePassword123!";
        String wrongPassword = "wrongPassword123!";
        String encoded = passwordEncodingService.encode(rawPassword);

        assertThat(passwordEncodingService.matches(wrongPassword, encoded)).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when raw password is null or blank during encoding")
    void encodeInvalidPasswordThrows() {
        assertThatThrownBy(() -> passwordEncodingService.encode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Raw password must not be null or empty");

        assertThatThrownBy(() -> passwordEncodingService.encode("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Raw password must not be null or empty");
    }

    @Test
    @DisplayName("Should return false when raw password or encoded password is null during matching")
    void matchesWithNullsReturnsFalse() {
        String encoded = passwordEncodingService.encode("securePassword123!");

        assertThat(passwordEncodingService.matches(null, encoded)).isFalse();
        assertThat(passwordEncodingService.matches("securePassword123!", null)).isFalse();
    }
}
