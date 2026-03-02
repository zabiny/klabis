package com.klabis.common.users;

import com.klabis.common.users.domain.PasswordSetupToken;
import com.klabis.common.users.domain.TokenHash;
import com.klabis.common.users.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PasswordSetupToken Aggregate Root Tests")
class PasswordSetupTokenTest {

    private static final String USER_NAME = "admin";
    private static final String PASSWORD_HASH = "$2a$10$dXJ3SW6G7P50eS6xFXyO6u8KMNZfXH3d2S7yQv6uHEjj3.6xCWH46";

    @Nested
    @DisplayName("generateFor() method")
    class GenerateForMethod {

        @Test
        @DisplayName("should generate token for user with validity period")
        void shouldGenerateTokenForUserWithValidityPeriod() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);
            Duration validity = Duration.ofHours(4);

            // When
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), validity);

            // Then
            assertThat(token).isNotNull();
            assertThat(token.getId()).isNotNull();
            assertThat(token.getUserId()).isEqualTo(user.getId());
            assertThat(token.getTokenHash()).isNotNull();
            assertThat(token.getCreatedAt()).isNotNull();
            assertThat(token.getExpiresAt()).isAfter(token.getCreatedAt());
            assertThat(token.getPlainText()).isNotNull(); // Available for email
            assertThat(token.isUsed()).isFalse();
            assertThat(token.isExpired()).isFalse();
            assertThat(token.isValid()).isTrue();
        }

        @Test
        @DisplayName("should generate unique token IDs")
        void shouldGenerateUniqueTokenIds() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);
            Duration validity = Duration.ofHours(4);

            // When
            PasswordSetupToken token1 = PasswordSetupToken.generateFor(user.getId(), validity);
            PasswordSetupToken token2 = PasswordSetupToken.generateFor(user.getId(), validity);

            // Then
            assertThat(token1.getId()).isNotEqualTo(token2.getId());
            assertThat(token1.getPlainText()).isNotEqualTo(token2.getPlainText());
        }

        @Test
        @DisplayName("should calculate correct expiration time")
        void shouldCalculateCorrectExpirationTime() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);
            Duration validity = Duration.ofHours(4);

            // When
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), validity);

            // Then
            assertThat(token.getExpiresAt())
                    .isEqualTo(token.getCreatedAt().plus(validity));
        }

        @Test
        @DisplayName("should handle very short validity period")
        void shouldHandleVeryShortValidityPeriod() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);
            Duration oneSecond = Duration.ofSeconds(1);

            // When
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), oneSecond);

            // Then
            assertThat(token.isExpired()).isFalse(); // Should still be valid immediately
            assertThat(token.getExpiresAt()).isAfter(token.getCreatedAt());
        }

        @Test
        @DisplayName("should handle very long validity period")
        void shouldHandleVeryLongValidityPeriod() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);
            Duration oneWeek = Duration.ofDays(7);

            // When
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), oneWeek);

            // Then
            assertThat(token.isExpired()).isFalse();
            assertThat(token.getExpiresAt()).isAfter(token.getCreatedAt().plus(java.time.Duration.ofDays(6)));
        }

        @Test
        @DisplayName("should reject null user")
        void shouldRejectNullUser() {
            assertThatThrownBy(() -> PasswordSetupToken.generateFor(null, Duration.ofHours(4)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User is required");
        }

        @Test
        @DisplayName("should reject null validity period")
        void shouldRejectNullValidityPeriod() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);

            assertThatThrownBy(() -> PasswordSetupToken.generateFor(user.getId(), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Validity period is required");
        }

        @Test
        @DisplayName("should reject zero validity period")
        void shouldRejectZeroValidityPeriod() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);

            assertThatThrownBy(() -> PasswordSetupToken.generateFor(user.getId(), Duration.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Validity period must be positive");
        }

        @Test
        @DisplayName("should reject negative validity period")
        void shouldRejectNegativeValidityPeriod() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);

            assertThatThrownBy(() -> PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(-1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Validity period must be positive");
        }
    }

    @Nested
    @DisplayName("markAsUsed() method")
    class MarkAsUsedMethod {

        @Test
        @DisplayName("should mark token as used")
        void shouldMarkTokenAsUsed() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));
            String ipAddress = "192.168.1.100";

            // When
            token.markAsUsed(ipAddress);

            // Then
            assertThat(token.isUsed()).isTrue();
            assertThat(token.getUsedAt()).isNotNull();
            assertThat(token.getUsedByIp()).isEqualTo(ipAddress);
            assertThat(token.isValid()).isFalse(); // Used tokens are not valid
        }

        @Test
        @DisplayName("should reject marking already used token")
        void shouldRejectMarkingAlreadyUsedToken() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));
            token.markAsUsed("192.168.1.100");

            // When/Then
            assertThatThrownBy(() -> token.markAsUsed("192.168.1.101"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Token has already been used");
        }

        @Test
        @DisplayName("should reject marking expired token as used")
        void shouldRejectMarkingExpiredTokenAsUsed() {
            // Given - create expired token (in the past)
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);
            UUID tokenId = UUID.randomUUID();
            UserId userId = user.getId();
            TokenHash hash = TokenHash.hash("test-token");
            // Created 5 hours ago, expired 1 hour ago
            java.time.Instant past = java.time.Instant.now().minus(java.time.Duration.ofHours(5));
            java.time.Instant expired = java.time.Instant.now().minus(java.time.Duration.ofHours(1));

            PasswordSetupToken token = PasswordSetupToken.reconstruct(tokenId, userId, hash, past, expired, null, null, null);

            // When/Then
            assertThatThrownBy(() -> token.markAsUsed("192.168.1.100"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Token has expired");
        }

        @Test
        @DisplayName("should reject marking token as used with null IP")
        void shouldRejectMarkingTokenAsUsedWithNullIp() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));

            assertThatThrownBy(() -> token.markAsUsed(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("IP address is required");
        }

        @Test
        @DisplayName("should reject marking token as used with empty IP")
        void shouldRejectMarkingTokenAsUsedWithEmptyIp() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));

            assertThatThrownBy(() -> token.markAsUsed(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("IP address is required");
        }
    }

    @Nested
    @DisplayName("isExpired() method")
    class IsExpiredMethod {

        @Test
        @DisplayName("should detect expired token")
        void shouldDetectExpiredToken() {
            // Given - expired token (created 5 hours ago, expired 1 hour ago)
            UUID tokenId = UUID.randomUUID();
            UserId userId = new UserId(UUID.randomUUID());
            TokenHash hash = TokenHash.hash("test-token");
            java.time.Instant past = java.time.Instant.now().minus(java.time.Duration.ofHours(5));
            java.time.Instant expired = java.time.Instant.now().minus(java.time.Duration.ofHours(1));

            PasswordSetupToken token = PasswordSetupToken.reconstruct(tokenId, userId, hash, past, expired, null, null, null);

            // Then
            assertThat(token.isExpired()).isTrue();
            assertThat(token.isValid()).isFalse();
        }

        @Test
        @DisplayName("should detect non-expired token")
        void shouldDetectNonExpiredToken() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));

            // Then
            assertThat(token.isExpired()).isFalse();
        }
    }

    @Nested
    @DisplayName("isUsed() method")
    class IsUsedMethod {

        @Test
        @DisplayName("should detect used token")
        void shouldDetectUsedToken() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));
            token.markAsUsed("192.168.1.100");

            // Then
            assertThat(token.isUsed()).isTrue();
        }

        @Test
        @DisplayName("should detect unused token")
        void shouldDetectUnusedToken() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));

            // Then
            assertThat(token.isUsed()).isFalse();
        }
    }

    @Nested
    @DisplayName("isValid() method")
    class IsValidMethod {

        @Test
        @DisplayName("should validate token when not expired and not used")
        void shouldValidateTokenWhenNotExpiredAndNotUsed() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));

            // Then
            assertThat(token.isValid()).isTrue();
        }

        @Test
        @DisplayName("should not validate token when expired")
        void shouldNotValidateTokenWhenExpired() {
            // Given - expired token
            UUID tokenId = UUID.randomUUID();
            UserId userId = new UserId(UUID.randomUUID());
            TokenHash hash = TokenHash.hash("test-token");
            java.time.Instant past = java.time.Instant.now().minus(java.time.Duration.ofHours(5));
            java.time.Instant expired = java.time.Instant.now().minus(java.time.Duration.ofHours(1));

            PasswordSetupToken token = PasswordSetupToken.reconstruct(tokenId, userId, hash, past, expired, null, null, null);

            // Then
            assertThat(token.isValid()).isFalse();
        }

        @Test
        @DisplayName("should not validate token when used")
        void shouldNotValidateTokenWhenUsed() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));
            token.markAsUsed("192.168.1.100");

            // Then
            assertThat(token.isValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("verify() method")
    class VerifyMethod {

        @Test
        @DisplayName("should verify matching token")
        void shouldVerifyMatchingToken() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));
            String plainToken = token.getPlainText();

            // When/Then
            assertThat(token.verify(plainToken)).isTrue();
        }

        @Test
        @DisplayName("should not verify non-matching token")
        void shouldNotVerifyNonMatchingToken() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));

            // When/Then
            assertThat(token.verify("wrong-token")).isFalse();
        }

        @Test
        @DisplayName("should not verify null token")
        void shouldNotVerifyNullToken() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));

            // When/Then
            assertThat(token.verify(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("getPlainText() method")
    class GetPlainTextMethod {

        @Test
        @DisplayName("should provide plain text token for email")
        void shouldProvidePlainTextTokenForEmail() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));

            // Then
            assertThat(token.getPlainText()).isNotNull();
            assertThat(token.getPlainText()).isNotEmpty();
            assertThat(token.getPlainText()).hasSize(36); // UUID format
        }
    }

    @Nested
    @DisplayName("Local datetime getters")
    class LocalDatetimeGetters {

        @Test
        @DisplayName("should provide local datetime variants")
        void shouldProvideLocalDatetimeVariants() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));

            // When/Then
            assertThat(token.getCreatedAtLocal()).isNotNull();
            assertThat(token.getExpiresAtLocal()).isNotNull();
            assertThat(token.getUsedAtLocal()).isNull(); // Not used yet

            // After marking as used
            token.markAsUsed("192.168.1.100");
            assertThat(token.getUsedAtLocal()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Object methods (equals, hashCode, toString)")
    class ObjectMethods {

        @Test
        @DisplayName("should implement equals based on ID")
        void shouldImplementEqualsBasedOnId() {
            // Given
            UUID sameId = UUID.randomUUID();
            UserId userId = new UserId(UUID.randomUUID());
            TokenHash hash = TokenHash.hash("test-token");
            java.time.Instant now = java.time.Instant.now();
            java.time.Instant later = now.plus(java.time.Duration.ofHours(4));

            PasswordSetupToken token1 = PasswordSetupToken.reconstruct(sameId, userId, hash, now, later, null, null, null);
            PasswordSetupToken token2 = PasswordSetupToken.reconstruct(sameId, userId, hash, now, later, null, null, null);
            PasswordSetupToken token3 = PasswordSetupToken.reconstruct(UUID.randomUUID(),
                    userId,
                    hash,
                    now,
                    later,
                    null,
                    null,
                    null);

            // Then
            assertThat(token1).isEqualTo(token2);
            assertThat(token1).isNotEqualTo(token3);
        }

        @Test
        @DisplayName("should implement hashCode based on ID")
        void shouldImplementHashCodeBasedOnId() {
            // Given
            UUID sameId = UUID.randomUUID();
            UserId userId = new UserId(UUID.randomUUID());
            TokenHash hash = TokenHash.hash("test-token");
            java.time.Instant now = java.time.Instant.now();
            java.time.Instant later = now.plus(java.time.Duration.ofHours(4));

            PasswordSetupToken token1 = PasswordSetupToken.reconstruct(sameId, userId, hash, now, later, null, null, null);
            PasswordSetupToken token2 = PasswordSetupToken.reconstruct(sameId, userId, hash, now, later, null, null, null);

            // Then
            assertThat(token1.hashCode()).isEqualTo(token2.hashCode());
        }

        @Test
        @DisplayName("should have meaningful toString")
        void shouldHaveMeaningfulToString() {
            // Given
            User user = User.createdUser(USER_NAME, PASSWORD_HASH);
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));

            // When
            String toString = token.toString();

            // Then
            assertThat(toString).contains("PasswordSetupToken");
            assertThat(toString).contains("id=");
            assertThat(toString).contains("userId=");
            assertThat(toString).contains("createdAt=");
            assertThat(toString).contains("expiresAt=");
            // Should not expose the hash
            assertThat(toString).doesNotContain("tokenHash");
        }
    }
}
