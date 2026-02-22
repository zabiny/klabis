package com.klabis.users.persistence.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.users.PasswordSetupToken;
import com.klabis.users.TokenHash;
import com.klabis.users.User;
import com.klabis.users.UserId;
import com.klabis.users.persistence.PasswordSetupTokenRepository;
import com.klabis.users.persistence.UserRepository;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PasswordSetupToken aggregate with Spring Data JDBC using Memento pattern.
 * <p>
 * Tests cover:
 * - CRUD operations
 * - Token hash persistence
 * - Instant fields persistence (createdAt, expiresAt, usedAt)
 * - Active token queries (unused, not expired)
 * - Token invalidation and cleanup
 * - Custom queries
 * <p>
 * Note: Spring Modulith test filtering disabled to force execution during development
 */
@DisplayName("PasswordSetupToken JDBC Repository Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
//@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = "DELETE FROM password_setup_tokens")
class PasswordSetupTokenJdbcRepositoryTest {

    @Autowired
    private PasswordSetupTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Each test runs in a transaction that is rolled back after completion,
        // ensuring test isolation without manual cleanup
    }

    private User createTestUser(String username) {
        User user = User.createdUser(
                username,
                "$2a$10$hashvalue"
        );
        return userRepository.save(user);
    }

    @Nested
    @DisplayName("save() method")
    class SaveMethod {

        @Test
        @DisplayName("should save new token with all fields")
        void shouldSaveNewToken() {
            // Given
            jdbcTemplate.queryForList("SELECT * FROM users")
                    .stream()
                    .map(e -> e.toString())
                    .forEach(System.out::println);

            User user = createTestUser("ZBM9001");
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));

            // When
            PasswordSetupToken saved = tokenRepository.save(token);

            // Then
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isEqualTo(token.getId());
            assertThat(saved.getUserId()).isEqualTo(user.getId());
            assertThat(saved.getTokenHash()).isEqualTo(token.getTokenHash());
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getExpiresAt()).isNotNull();
            assertThat(saved.isUsed()).isFalse();
        }

        @Test
        @DisplayName("should save token with used status")
        void shouldSaveTokenWithUsedStatus() {
            // Given
            User user = createTestUser("ZBM9002");
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));
            token.markAsUsed("192.168.1.100");

            // When
            PasswordSetupToken saved = tokenRepository.save(token);

            // Then
            assertThat(saved.isUsed()).isTrue();
            assertThat(saved.getUsedAt()).isNotNull();
            assertThat(saved.getUsedByIp()).isEqualTo("192.168.1.100");
        }

        @Test
        @DisplayName("should populate createdAt on save")
        void shouldPopulateCreatedAtOnSave() {
            // Given
            User user = createTestUser("ZBM9003");
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));

            // When
            PasswordSetupToken saved = tokenRepository.save(token);

            // Then
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getCreatedAt()).isBefore(Instant.now().plusSeconds(1));
        }
    }

    @Nested
    @DisplayName("findById() method")
    class FindByIdMethod {

        @Test
        @DisplayName("should find token by id")
        void shouldFindTokenById() {
            // Given
            User user = createTestUser("ZBM9004");
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));
            PasswordSetupToken saved = tokenRepository.save(token);

            // When
            Optional<PasswordSetupToken> found = tokenRepository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
            assertThat(found.get().getUserId()).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("should return empty when token not found")
        void shouldReturnEmptyWhenTokenNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();

            // When
            Optional<PasswordSetupToken> found = tokenRepository.findById(nonExistentId);

            // Then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("should load token hash correctly")
        void shouldLoadTokenHashCorrectly() {
            // Given
            User user = createTestUser("ZBM9005");
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));
            PasswordSetupToken saved = tokenRepository.save(token);
            TokenHash originalHash = token.getTokenHash();

            // When
            Optional<PasswordSetupToken> found = tokenRepository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getTokenHash()).isEqualTo(originalHash);
            assertThat(found.get().verify(token.getPlainText())).isTrue();
        }
    }

    @Nested
    @DisplayName("findByTokenHash() method")
    class FindByTokenHashMethod {

        @Test
        @DisplayName("should find token by hash")
        void shouldFindTokenByHash() {
            // Given
            User user = createTestUser("ZBM9006");
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));
            tokenRepository.save(token);
            TokenHash tokenHash = token.getTokenHash();

            // When
            Optional<PasswordSetupToken> found = tokenRepository.findByTokenHash(tokenHash);

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(token.getId());
        }

        @Test
        @DisplayName("should return empty when hash not found")
        void shouldReturnEmptyWhenHashNotFound() {
            // Given
            TokenHash nonExistentHash = TokenHash.hash("non-existent-token");

            // When
            Optional<PasswordSetupToken> found = tokenRepository.findByTokenHash(nonExistentHash);

            // Then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findActiveTokensForUser() method")
    class FindActiveTokensForUserMethod {

        @Test
        @DisplayName("should find active (unused, not expired) tokens for user")
        void shouldFindActiveTokensForUser() {
            // Given
            User user = createTestUser("ZBM9007");
            PasswordSetupToken activeToken = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));
            tokenRepository.save(activeToken);

            // When
            List<PasswordSetupToken> activeTokens = tokenRepository.findActiveTokensForUser(user.getId());

            // Then
            assertThat(activeTokens).hasSize(1);
            assertThat(activeTokens.get(0).getId()).isEqualTo(activeToken.getId());
            assertThat(activeTokens.get(0).isValid()).isTrue();
        }

        @Test
        @DisplayName("should not find expired tokens")
        void shouldNotFindExpiredTokens() {
            // Given - Create expired token (created 5 hours ago, expired 1 hour ago)
            User user = createTestUser("ZBM9008");
            UUID tokenId = UUID.randomUUID();
            UserId userId = user.getId();
            TokenHash hash = TokenHash.hash("test-token");
            Instant past = Instant.now().minus(Duration.ofHours(5));
            Instant expired = Instant.now().minus(Duration.ofHours(1));

            PasswordSetupToken expiredToken = PasswordSetupToken.reconstruct(
                    tokenId, userId, hash, past, expired, null, null, null
            );
            tokenRepository.save(expiredToken);

            // When
            List<PasswordSetupToken> activeTokens = tokenRepository.findActiveTokensForUser(user.getId());

            // Then
            assertThat(activeTokens).isEmpty();
        }

        @Test
        @DisplayName("should not find used tokens")
        void shouldNotFindUsedTokens() {
            // Given
            User user = createTestUser("ZBM9009");
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));
            token.markAsUsed("192.168.1.100");
            tokenRepository.save(token);

            // When
            List<PasswordSetupToken> activeTokens = tokenRepository.findActiveTokensForUser(user.getId());

            // Then
            assertThat(activeTokens).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when no active tokens")
        void shouldReturnEmptyListWhenNoActiveTokens() {
            // Given
            User user = createTestUser("ZBM9010");

            // When
            List<PasswordSetupToken> activeTokens = tokenRepository.findActiveTokensForUser(user.getId());

            // Then
            assertThat(activeTokens).isEmpty();
        }

        @Test
        @DisplayName("should find only active tokens when mixed tokens exist")
        void shouldFindOnlyActiveTokensWhenMixedTokensExist() {
            // Given
            User user = createTestUser("ZBM9011");

            // Active token
            PasswordSetupToken activeToken = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));
            tokenRepository.save(activeToken);

            // Used token
            PasswordSetupToken usedToken = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));
            usedToken.markAsUsed("192.168.1.100");
            tokenRepository.save(usedToken);

            // Expired token
            UUID expiredTokenId = UUID.randomUUID();
            TokenHash expiredHash = TokenHash.hash("expired-token");
            Instant past = Instant.now().minus(Duration.ofHours(5));
            Instant expired = Instant.now().minus(Duration.ofHours(1));
            PasswordSetupToken expiredToken = PasswordSetupToken.reconstruct(
                    expiredTokenId, user.getId(), expiredHash, past, expired, null, null, null
            );
            tokenRepository.save(expiredToken);

            // When
            List<PasswordSetupToken> activeTokens = tokenRepository.findActiveTokensForUser(user.getId());

            // Then
            assertThat(activeTokens).hasSize(1);
            assertThat(activeTokens.get(0).getId()).isEqualTo(activeToken.getId());
        }
    }

    @Nested
    @DisplayName("invalidateAllForUser() method")
    class InvalidateAllForUserMethod {

        @Test
        @DisplayName("should delete all tokens for user")
        void shouldDeleteAllTokensForUser() {
            // Given
            User user = createTestUser("ZBM9012");
            PasswordSetupToken token1 = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));
            PasswordSetupToken token2 = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));
            tokenRepository.save(token1);
            tokenRepository.save(token2);

            // When
            tokenRepository.invalidateAllForUser(user.getId());

            // Then
            List<PasswordSetupToken> remaining = tokenRepository.findAll();
            assertThat(remaining).isEmpty();
        }

        @Test
        @DisplayName("should only delete tokens for specific user")
        void shouldOnlyDeleteTokensForSpecificUser() {
            // Given
            User user1 = createTestUser("ZBM9013");
            User user2 = createTestUser("ZBM9014");
            PasswordSetupToken token1 = PasswordSetupToken.generateFor(user1.getId(), Duration.ofHours(4));
            PasswordSetupToken token2 = PasswordSetupToken.generateFor(user2.getId(), Duration.ofHours(4));
            tokenRepository.save(token1);
            tokenRepository.save(token2);

            // When
            tokenRepository.invalidateAllForUser(user1.getId());

            // Then
            List<PasswordSetupToken> remaining = tokenRepository.findAll();
            assertThat(remaining).hasSize(1);
            assertThat(remaining.get(0).getUserId()).isEqualTo(user2.getId());
        }
    }

    @Nested
    @DisplayName("deleteExpiredTokens() method")
    class DeleteExpiredTokensMethod {

        @Test
        @DisplayName("should delete expired tokens")
        void shouldDeleteExpiredTokens() {
            // Given - Create expired token
            User user = createTestUser("ZBM9015");
            UUID tokenId = UUID.randomUUID();
            UserId userId = user.getId();
            TokenHash hash = TokenHash.hash("expired-token");
            Instant past = Instant.now().minus(Duration.ofHours(5));
            Instant expired = Instant.now().minus(Duration.ofHours(1));

            PasswordSetupToken expiredToken = PasswordSetupToken.reconstruct(
                    tokenId, userId, hash, past, expired, null, null, null
            );
            tokenRepository.save(expiredToken);

            // When
            int deletedCount = tokenRepository.deleteExpiredTokens();

            // Then
            assertThat(deletedCount).isEqualTo(1);
            List<PasswordSetupToken> remaining = tokenRepository.findAll();
            assertThat(remaining).isEmpty();
        }

        @Test
        @DisplayName("should not delete active tokens")
        void shouldNotDeleteActiveTokens() {
            // Given
            User user = createTestUser("ZBM9016");
            PasswordSetupToken activeToken = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));
            tokenRepository.save(activeToken);

            // When
            int deletedCount = tokenRepository.deleteExpiredTokens();

            // Then
            assertThat(deletedCount).isZero();
            List<PasswordSetupToken> remaining = tokenRepository.findAll();
            assertThat(remaining).hasSize(1);
        }

        @Test
        @DisplayName("should return count of deleted tokens")
        void shouldReturnCountOfDeletedTokens() {
            // Given - Create multiple expired tokens
            User user = createTestUser("ZBM9017");
            Instant past1 = Instant.now().minus(Duration.ofHours(5));
            Instant past2 = Instant.now().minus(Duration.ofHours(5)).minusSeconds(1);
            Instant expired = Instant.now().minus(Duration.ofHours(1));

            PasswordSetupToken expiredToken1 = PasswordSetupToken.reconstruct(
                    UUID.randomUUID(), user.getId(), TokenHash.hash("expired1"), past1, expired, null, null, null
            );
            PasswordSetupToken expiredToken2 = PasswordSetupToken.reconstruct(
                    UUID.randomUUID(), user.getId(), TokenHash.hash("expired2"), past2, expired, null, null, null
            );
            tokenRepository.save(expiredToken1);
            tokenRepository.save(expiredToken2);

            // Active token
            PasswordSetupToken activeToken = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));
            tokenRepository.save(activeToken);

            // When
            int deletedCount = tokenRepository.deleteExpiredTokens();

            // Then
            assertThat(deletedCount).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("findAll() method")
    class FindAllMethod {

        @Test
        @DisplayName("should find all tokens")
        void shouldFindAllTokens() {
            // Given
            User user1 = createTestUser("ZBM9018");
            User user2 = createTestUser("ZBM9019");
            PasswordSetupToken token1 = PasswordSetupToken.generateFor(user1.getId(), Duration.ofHours(4));
            PasswordSetupToken token2 = PasswordSetupToken.generateFor(user2.getId(), Duration.ofHours(4));
            tokenRepository.save(token1);
            tokenRepository.save(token2);

            // When
            List<PasswordSetupToken> allTokens = tokenRepository.findAll();

            // Then
            assertThat(allTokens).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no tokens exist")
        void shouldReturnEmptyListWhenNoTokensExist() {
            // When
            List<PasswordSetupToken> allTokens = tokenRepository.findAll();

            // Then
            assertThat(allTokens).isEmpty();
        }
    }

    @Nested
    @DisplayName("Instant fields")
    class InstantFields {

        @Test
        @DisplayName("should persist createdAt correctly")
        void shouldPersistCreatedAtCorrectly() {
            // Given
            User user = createTestUser("ZBM9020");
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));
            Instant originalCreatedAt = token.getCreatedAt();

            // When
            PasswordSetupToken saved = tokenRepository.save(token);
            Optional<PasswordSetupToken> found = tokenRepository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            // H2 stores timestamps with microsecond precision (6 decimal places), not nanosecond (9)
            // Use isBetween to allow for microsecond precision differences
            assertThat(found.get().getCreatedAt())
                    .isBetween(originalCreatedAt.minusNanos(1000), originalCreatedAt.plusNanos(1000));
        }

        @Test
        @DisplayName("should persist expiresAt correctly")
        void shouldPersistExpiresAtCorrectly() {
            // Given
            User user = createTestUser("ZBM9021");
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));
            Instant originalExpiresAt = token.getExpiresAt();

            // When
            PasswordSetupToken saved = tokenRepository.save(token);
            Optional<PasswordSetupToken> found = tokenRepository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            // H2 stores timestamps with microsecond precision (6 decimal places), not nanosecond (9)
            // Use isBetween to allow for microsecond precision differences
            assertThat(found.get().getExpiresAt())
                    .isBetween(originalExpiresAt.minusNanos(1000), originalExpiresAt.plusNanos(1000));
        }

        @Test
        @DisplayName("should persist usedAt correctly when marked as used")
        void shouldPersistUsedAtCorrectlyWhenMarkedAsUsed() {
            // Given
            User user = createTestUser("ZBM9022");
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));
            token.markAsUsed("192.168.1.100");
            Instant originalUsedAt = token.getUsedAt();

            // When
            PasswordSetupToken saved = tokenRepository.save(token);
            Optional<PasswordSetupToken> found = tokenRepository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            // H2 stores timestamps with microsecond precision (6 decimal places), not nanosecond (9)
            // Use isBetween to allow for microsecond precision differences
            assertThat(found.get().getUsedAt())
                    .isBetween(originalUsedAt.minusNanos(1000), originalUsedAt.plusNanos(1000));
            assertThat(found.get().getUsedByIp()).isEqualTo("192.168.1.100");
        }

        @Test
        @DisplayName("should calculate correct expiration time")
        void shouldCalculateCorrectExpirationTime() {
            // Given
            User user = createTestUser("ZBM9023");
            Duration validity = Duration.ofHours(4);
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), validity);

            // When
            PasswordSetupToken saved = tokenRepository.save(token);
            Optional<PasswordSetupToken> found = tokenRepository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getExpiresAt())
                    .isEqualTo(found.get().getCreatedAt().plus(validity));
        }
    }
}
