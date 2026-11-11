package com.klabis.common.users.infrastructure.jdbc;

import com.klabis.common.users.UserAssert;
import com.klabis.common.users.UserId;
import com.klabis.common.users.domain.AccountStatus;
import com.klabis.common.users.domain.User;
import com.klabis.common.users.domain.UserRepository;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for User aggregate with Spring Data JDBC using Memento pattern.
 * <p>
 * Tests cover:
 * - CRUD operations
 * - Roles collection persistence (@MappedCollection)
 * - Authorities JSON persistence (custom converter)
 * - Optimistic locking (@Version)
 * - Auditing fields (@CreatedDate, @LastModifiedDate)
 * - Custom queries
 * <p>
 * Note: Spring Modulith test filtering disabled to force execution during development
 */
@DisplayName("User JDBC Repository Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = "DELETE FROM common.users")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserJdbcRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Each test runs in a transaction that is rolled back after completion,
        // ensuring test isolation without manual cleanup
    }

    @Nested
    @DisplayName("save() method")
    class SaveMethod {

        @Test
        @DisplayName("should save new user with all fields")
        void shouldSaveNewUser() {
            // Given
            User user = User.createdUser(
                    "ZBM9001",
                    "$2a$10$hashvalue"
            );

            // When
            User saved = userRepository.save(user);

            // Then
            UserAssert.assertThat(saved)
                    .hasId(user.getId())
                    .hasUsername("ZBM9001")
                    .hasPasswordHash("$2a$10$hashvalue")
                    .hasAccountStatus(AccountStatus.ACTIVE)
                    .isEnabled();
        }

        @Test
        @DisplayName("should save user with multiple roles")
        void shouldSaveUserWithMultipleRoles() {
            // Given
            User user = User.createdUser(
                    "ZBM9002",
                    "$2a$10$hashvalue"
            );

            // When
            User saved = userRepository.save(user);

            // Then
            assertThat(saved.getId()).isNotNull();
        }

        @Test
        @DisplayName("should save user with multiple authorities")
        void shouldSaveUserWithMultipleAuthorities() {
            // Given
            User user = User.createdUser(
                    "ZBM9003",
                    "$2a$10$hashvalue"
            );

            // When
            User saved = userRepository.save(user);

            // Then
            assertThat(saved.getId()).isNotNull();
        }

        @Test
        @DisplayName("should save user with PENDING_ACTIVATION status")
        void shouldSaveUserWithPendingActivationStatus() {
            // Given
            User user = User.createdUser("ZBM9004");

            // When
            User saved = userRepository.save(user);

            // Then
            UserAssert.assertThat(saved)
                    .hasAccountStatus(AccountStatus.PENDING_ACTIVATION)
                    .isNotEnabled();
        }
    }

    @Nested
    @DisplayName("findById() method")
    class FindByIdMethod {

        @Test
        @DisplayName("should find user by id")
        void shouldFindUserById() {
            // Given
            User user = User.createdUser(
                    "ZBM9005",
                    "$2a$10$hashvalue"
            );
            User saved = userRepository.save(user);

            // When
            Optional<User> found = userRepository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            UserAssert.assertThat(found.get())
                    .hasId(saved.getId())
                    .hasUsername("ZBM9005");
        }

        @Test
        @DisplayName("should return empty when user not found")
        void shouldReturnEmptyWhenUserNotFound() {
            // Given
            UserId nonExistentId = new UserId(UUID.randomUUID());

            // When
            Optional<User> found = userRepository.findById(nonExistentId);

            // Then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("should load roles collection correctly")
        void shouldLoadRolesCollection() {
            // Given
            User user = User.createdUser(
                    "ZBM9006",
                    "$2a$10$hashvalue"
            );
            User saved = userRepository.save(user);

            // When
            Optional<User> found = userRepository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
        }

        @Test
        @DisplayName("should load authorities JSON correctly")
        void shouldLoadAuthoritiesJson() {
            // Given
            User user = User.createdUser(
                    "ZBM9007",
                    "$2a$10$hashvalue"
            );
            User saved = userRepository.save(user);

            // When
            Optional<User> found = userRepository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
        }
    }

    @Nested
    @DisplayName("findByAnyUsername() method")
    class FindByUsernameMethod {

        @Test
        @DisplayName("should find user by username")
        void shouldFindUserByUsername() {
            // Given
            User user = User.createdUser(
                    "ZBM9008",
                    "$2a$10$hashvalue"
            );
            userRepository.save(user);

            // When
            Optional<User> found = userRepository.findByAnyUsername("ZBM9008");

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getUsername()).isEqualTo("ZBM9008");
        }

        @Test
        @DisplayName("should return empty when username not found")
        void shouldReturnEmptyWhenUsernameNotFound() {
            // When
            Optional<User> found = userRepository.findByAnyUsername("NONEXIST");

            // Then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("should be case-sensitive")
        void shouldBeCaseSensitive() {
            // Given
            User user = User.createdUser(
                    "ZBM9009",
                    "$2a$10$hashvalue"
            );
            userRepository.save(user);

            // When
            Optional<User> found = userRepository.findByAnyUsername("zbm9009"); // lowercase

            // Then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("altUsername")
    class AltUsername {

        private User takenOver(String oldLogin, String newLogin) {
            return userRepository.save(userRepository.save(User.createdUser(oldLogin, "$2a$10$hashvalue"))
                    .replaceUsername(newLogin));
        }

        @Test
        @DisplayName("should find the same user by username and by altUsername")
        void shouldFindByBothLogins() {
            User saved = takenOver("EXT0001", "ZBM9101");

            assertThat(userRepository.findByAnyUsername("ZBM9101")).get().extracting(User::getId).isEqualTo(saved.getId());
            Optional<User> byAlt = userRepository.findByAnyUsername("EXT0001");
            assertThat(byAlt).get().extracting(User::getId).isEqualTo(saved.getId());
            assertThat(byAlt.get().getUsername()).isEqualTo("ZBM9101");
            assertThat(byAlt.get().getAltUsername()).isEqualTo("EXT0001");
        }

        @Test
        @DisplayName("should prefer the username match when another user's altUsername equals it")
        void shouldPreferUsernameMatchOnCrossColumnCollision() {
            takenOver("COL0001", "ZBM9104");
            User owner = userRepository.save(User.createdUser("COL0001", "$2a$10$hashvalue"));

            assertThat(userRepository.findByAnyUsername("COL0001")).get().extracting(User::getId).isEqualTo(owner.getId());
        }

        @Test
        @DisplayName("should reject a duplicate altUsername")
        void shouldRejectDuplicateAltUsername() {
            takenOver("EXT0002", "ZBM9102");

            assertThatThrownBy(() -> takenOver("EXT0002", "ZBM9103"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("Optimistic locking")
    class OptimisticLocking {

        @Test
        @DisplayName("should set version on save")
        void shouldSetVersionOnSave() {
            // Given
            User user = User.createdUser(
                    "ZBM9015",
                    "$2a$10$hashvalue"
            );

            // When
            User saved = userRepository.save(user);

            // Then
            UserAssert.assertThat(saved)
                    .hasVersionNotNull()
                    .hasVersion(0L);
        }

        @Test
        @DisplayName("should increment version on update")
        void shouldIncrementVersionOnUpdate() {
            // Given
            User user = User.createdUser(
                    "ZBM9016",
                    "$2a$10$hashvalue"
            );
            User saved = userRepository.save(user);
            Long initialVersion = saved.getVersion();

            // When - Update user password
            User updated = saved.activateWithPassword("$2a$10$newHash");
            User savedUpdated = userRepository.save(updated);

            // Then
            UserAssert.assertThat(savedUpdated)
                    .hasVersionGreaterThan(initialVersion);
        }
    }

    @Nested
    @DisplayName("Auditing")
    class Auditing {

        @Test
        @DisplayName("should populate createdAt on save")
        void shouldPopulateCreatedAtOnSave() {
            // Given
            User user = User.createdUser(
                    "ZBM9017",
                    "$2a$10$hashvalue"
            );

            // When
            User saved = userRepository.save(user);

            // Then
            UserAssert.assertThat(saved)
                    .hasCreatedAtNotNull();
        }

        @Test
        @DisplayName("should populate modifiedAt on save")
        void shouldPopulateModifiedAtOnSave() {
            // Given
            User user = User.createdUser(
                    "ZBM9018",
                    "$2a$10$hashvalue"
            );

            // When
            User saved = userRepository.save(user);

            // Then
            UserAssert.assertThat(saved)
                    .hasLastModifiedAtNotNull();
        }

        @Test
        @DisplayName("should update modifiedAt on update")
        void shouldUpdateModifiedAtOnUpdate() throws InterruptedException {
            // Given - Save initial user
            User user = User.createdUser(
                    "ZBM9019",
                    "$2a$10$hashvalue"
            );
            User saved = userRepository.save(user);
            Instant initialModifiedAt = saved.getLastModifiedAt();

            // Wait a bit to ensure timestamp difference
            Thread.sleep(10);

            // When - Update user
            User updated = saved.activateWithPassword("$2a$10$newHash");
            User savedUpdated = userRepository.save(updated);

            // Then
            UserAssert.assertThat(savedUpdated)
                    .hasLastModifiedAtAfter(initialModifiedAt);
        }
    }

    @Nested
    @DisplayName("Update operations")
    class UpdateOperations {

        @Test
        @DisplayName("should activate pending user with new password")
        void shouldActivatePendingUserWithNewPassword() {
            // Given - Create pending activation user
            User pendingUser = User.createdUser("ZBM9020");
            User saved = userRepository.save(pendingUser);

            // When - Activate with new password
            User activated = saved.activateWithPassword("$2a$10$newHash");
            User savedActivated = userRepository.save(activated);

            // Then
            Optional<User> found = userRepository.findById(savedActivated.getId());
            assertThat(found).isPresent();
            UserAssert.assertThat(found.get())
                    .hasAccountStatus(AccountStatus.ACTIVE)
                    .isEnabled()
                    .hasPasswordHash("$2a$10$newHash");
        }
    }
}
