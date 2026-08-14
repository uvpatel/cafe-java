package com.cafepos.identity.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAccountTest {

    private final UUID employeeId = UUID.randomUUID();
    private final String passwordHash = "$2a$10$abcdefghijklmnopqrstuu";

    @Nested
    @DisplayName("Creation and Validation")
    class CreationAndValidation {

        @Test
        @DisplayName("Should create user account with valid parameters")
        void createValidUserAccount() {
            UserAccount account = new UserAccount(employeeId, passwordHash);

            assertThat(account.getId()).isNotNull();
            assertThat(account.getEmployeeId()).isEqualTo(employeeId);
            assertThat(account.getPasswordHash()).isEqualTo(passwordHash);
            assertThat(account.getFailedAttempts()).isZero();
            assertThat(account.getLockedUntil()).isNull();
            assertThat(account.getLastLoginAt()).isNull();
        }

        @Test
        @DisplayName("Should throw exception when employeeId is null")
        void nullEmployeeIdThrows() {
            assertThatThrownBy(() -> new UserAccount(null, passwordHash))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee ID must not be null");
        }

        @Test
        @DisplayName("Should throw exception when passwordHash is null or blank")
        void invalidPasswordHashThrows() {
            assertThatThrownBy(() -> new UserAccount(employeeId, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Password hash must not be empty");

            assertThatThrownBy(() -> new UserAccount(employeeId, "  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Password hash must not be empty");
        }
    }

    @Nested
    @DisplayName("Failed Attempts and Account Locking")
    class FailedAttemptsAndLocking {

        @Test
        @DisplayName("Should increment failed attempts count on failed login attempt")
        void incrementFailedAttempts() {
            UserAccount account = new UserAccount(employeeId, passwordHash);
            Instant now = Instant.now();

            account.recordFailedAttempt(now);

            assertThat(account.getFailedAttempts()).isEqualTo(1);
            assertThat(account.isLocked(now)).isFalse();
        }

        @Test
        @DisplayName("Should lock account for 15 minutes after 5 failed attempts")
        void lockAccountAfterMaxFailedAttempts() {
            UserAccount account = new UserAccount(employeeId, passwordHash);
            Instant now = Instant.now();

            for (int i = 0; i < 5; i++) {
                account.recordFailedAttempt(now);
            }

            assertThat(account.getFailedAttempts()).isEqualTo(5);
            assertThat(account.getLockedUntil()).isEqualTo(now.plus(UserAccount.LOCK_DURATION));
            assertThat(account.isLocked(now)).isTrue();
            assertThat(account.isLocked(now.plus(Duration.ofMinutes(16)))).isFalse();
        }

        @Test
        @DisplayName("Should unlock account after lock duration expires")
        void autoUnlockAfterLockExpiry() {
            UserAccount account = new UserAccount(employeeId, passwordHash);
            Instant now = Instant.now();

            for (int i = 0; i < 5; i++) {
                account.recordFailedAttempt(now);
            }

            Instant afterLockExpired = now.plus(Duration.ofMinutes(16));
            assertThat(account.isLocked(afterLockExpired)).isFalse();
        }
    }

    @Nested
    @DisplayName("Lock Reset and Successful Login")
    class LockResetAndSuccessfulLogin {

        @Test
        @DisplayName("Should reset failed attempts and locked status on successful login")
        void resetOnSuccessfulLogin() {
            UserAccount account = new UserAccount(employeeId, passwordHash);
            Instant now = Instant.now();

            account.recordFailedAttempt(now);
            account.recordFailedAttempt(now);

            Instant loginTime = now.plus(Duration.ofMinutes(1));
            account.recordSuccessfulLogin(loginTime);

            assertThat(account.getFailedAttempts()).isZero();
            assertThat(account.getLockedUntil()).isNull();
            assertThat(account.getLastLoginAt()).isEqualTo(loginTime);
        }

        @Test
        @DisplayName("Should reset lock status when unlock is explicitly called")
        void explicitUnlock() {
            UserAccount account = new UserAccount(employeeId, passwordHash);
            Instant now = Instant.now();

            for (int i = 0; i < 5; i++) {
                account.recordFailedAttempt(now);
            }
            assertThat(account.isLocked(now)).isTrue();

            account.unlock();

            assertThat(account.getFailedAttempts()).isZero();
            assertThat(account.getLockedUntil()).isNull();
            assertThat(account.isLocked(now)).isFalse();
        }
    }

    @Nested
    @DisplayName("Password Update")
    class PasswordUpdate {

        @Test
        @DisplayName("Should update password hash")
        void updatePasswordSuccessfully() {
            UserAccount account = new UserAccount(employeeId, passwordHash);
            String newHash = "$2a$10$newhashvalue1234567890123";

            account.updatePassword(newHash);

            assertThat(account.getPasswordHash()).isEqualTo(newHash);
        }

        @Test
        @DisplayName("Should throw exception when updating password with invalid hash")
        void invalidNewPasswordHashThrows() {
            UserAccount account = new UserAccount(employeeId, passwordHash);

            assertThatThrownBy(() -> account.updatePassword(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("New password hash must not be empty");

            assertThatThrownBy(() -> account.updatePassword("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("New password hash must not be empty");
        }
    }

    @Nested
    @DisplayName("Equals and HashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("Accounts with same ID should be equal")
        void equalsAndHashCodeById() {
            UUID id = UUID.randomUUID();
            UserAccount account1 = new UserAccount(id, employeeId, passwordHash);
            UserAccount account2 = new UserAccount(id, UUID.randomUUID(), passwordHash);

            assertThat(account1).isEqualTo(account2);
            assertThat(account1.hashCode()).isEqualTo(account2.hashCode());
        }
    }
}
