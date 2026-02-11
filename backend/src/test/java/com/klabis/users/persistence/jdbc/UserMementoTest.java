package com.klabis.users.persistence.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.users.User;
import com.klabis.users.AccountStatus;
import com.klabis.users.UserAssert;
import com.klabis.users.UserCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UserMemento class.
 * <p>
 * Tests the conversion between User domain entity and UserMemento persistence object:
 * - from(User) - creates memento from domain entity
 * - toUser() - reconstructs domain entity from memento
 * - Round-trip: User → Memento → User preserves all data
 */
@DisplayName("UserMemento Tests")
class UserMementoTest {

    private User createTestUser() {
        return User.create(
                "ZBM9001",
                "$2a$10$hashvalue"
        );
    }

    private User createTestUserWithAudit() {
        User user = createTestUser();
        AuditMetadata auditMetadata = new AuditMetadata(
                Instant.now(),
                "test-user",
                Instant.now(),
                "test-user",
                0L
        );
        user.updateAuditMetadata(auditMetadata);
        return user;
    }

    private User createTestUserWithAllFields() {
        return User.create(
                "ZBM9002",
                "$2a$10$hashvalue"
        );
    }

    @Nested
    @DisplayName("from(User) method")
    class FromUserMethod {

        @Test
        @DisplayName("should create memento from user with basic fields")
        void shouldCreateMementoFromUserWithBasicFields() {
            // Arrange
            User user = createTestUser();

            // Act
            UserMemento memento = UserMemento.from(user);

            // Assert - convert back to domain to verify
            User reconstructed = memento.toUser();
            assertThat(memento.getId()).isEqualTo(user.getId().uuid());
            UserAssert.assertThat(reconstructed)
                    .hasUsername("ZBM9001")
                    .hasPasswordHash("$2a$10$hashvalue")
                    .hasAccountStatus(AccountStatus.ACTIVE);
        }

        @Test
        @DisplayName("should create memento from user with all fields")
        void shouldCreateMementoFromUserWithAllFields() {
            // Arrange
            User user = createTestUserWithAllFields();

            // Act
            UserMemento memento = UserMemento.from(user);

            // Assert - convert back to domain to verify
            User reconstructed = memento.toUser();
            UserAssert.assertThat(reconstructed)
                    .hasUsername("ZBM9002")
                    .hasPasswordHash("$2a$10$hashvalue")
                    .isActiveUser();
        }

        @Test
        @DisplayName("should create memento from user with PENDING_ACTIVATION status")
        void shouldCreateMementoFromUserWithPendingActivationStatus() {
            // Arrange
            User user = User.createPendingActivation(
                    "ZBM9003",
                    "$2a$10$temphash"
            );

            // Act
            UserMemento memento = UserMemento.from(user);

            // Assert - convert back to domain to verify
            User reconstructed = memento.toUser();
            UserAssert.assertThat(reconstructed)
                    .isPendingActivationUser();
        }

        @Test
        @DisplayName("should copy audit metadata when present")
        void shouldCopyAuditMetadataWhenPresent() {
            // Arrange
            User user = createTestUserWithAudit();

            // Act
            UserMemento memento = UserMemento.from(user);

            // Assert - convert back to domain to verify
            User reconstructed = memento.toUser();
            UserAssert.assertThat(reconstructed)
                    .hasCreatedAtNotNull()
                    .hasLastModifiedAtNotNull()
                    .hasVersion(0L);
        }

        @Test
        @DisplayName("should set isNew=true for new user without audit metadata")
        void shouldSetIsNewTrueForNewUserWithoutAuditMetadata() {
            // Arrange
            User user = createTestUser(); // No audit metadata

            // Act
            UserMemento memento = UserMemento.from(user);

            // Assert
            assertThat(memento.isNew()).isTrue();
        }

        @Test
        @DisplayName("should set isNew=false for existing user with audit metadata")
        void shouldSetIsNewFalseForExistingUserWithAuditMetadata() {
            // Arrange
            User user = createTestUserWithAudit();

            // Act
            UserMemento memento = UserMemento.from(user);

            // Assert
            assertThat(memento.isNew()).isFalse();
        }
    }

    @Nested
    @DisplayName("toUser() method")
    class ToUserMethod {

        @Test
        @DisplayName("should reconstruct user from memento with basic fields")
        void shouldReconstructUserFromMementoWithBasicFields() {
            // Arrange
            User originalUser = createTestUser();
            UserMemento memento = UserMemento.from(originalUser);

            // Act
            User reconstructed = memento.toUser();

            // Assert
            UserAssert.assertThat(reconstructed)
                    .hasId(originalUser.getId())
                    .hasUsername("ZBM9001")
                    .hasPasswordHash("$2a$10$hashvalue")
                    .isActiveUser();
        }

        @Test
        @DisplayName("should reconstruct user from memento with all fields")
        void shouldReconstructUserFromMementoWithAllFields() {
            // Arrange
            User originalUser = createTestUserWithAllFields();
            UserMemento memento = UserMemento.from(originalUser);

            // Act
            User reconstructed = memento.toUser();

            // Assert
            UserAssert.assertThat(reconstructed)
                    .hasUsername("ZBM9002")
                    .hasPasswordHash("$2a$10$hashvalue")
                    .isActiveUser();
        }

        @Test
        @DisplayName("should reconstruct user with PENDING_ACTIVATION status")
        void shouldReconstructUserWithPendingActivationStatus() {
            // Arrange
            User originalUser = User.createPendingActivation(
                    "ZBM9003",
                    "$2a$10$temphash"
            );
            UserMemento memento = UserMemento.from(originalUser);

            // Act
            User reconstructed = memento.toUser();

            // Assert
            UserAssert.assertThat(reconstructed)
                    .isPendingActivationUser();
        }

        @Test
        @DisplayName("should reconstruct audit metadata when present")
        void shouldReconstructAuditMetadataWhenPresent() {
            // Arrange
            User originalUser = createTestUserWithAudit();
            UserMemento memento = UserMemento.from(originalUser);

            // Act
            User reconstructed = memento.toUser();

            // Assert
            UserAssert.assertThat(reconstructed)
                    .hasCreatedAtNotNull()
                    .hasLastModifiedAtNotNull()
                    .hasVersion(0L);
        }
    }

    @Nested
    @DisplayName("Round-trip conversion")
    class RoundTripConversion {

        @Test
        @DisplayName("should preserve all data in round-trip conversion")
        void shouldPreserveAllDataInRoundTripConversion() {
            // Arrange
            User original = createTestUserWithAllFields();

            // Act - Convert to memento and back
            UserMemento memento = UserMemento.from(original);
            User reconstructed = memento.toUser();

            // Assert - All fields preserved
            UserAssert.assertThat(reconstructed)
                    .hasSameIdentityAs(original)
                    .hasAccountStatus(original.getAccountStatus())
                    .isAccountNonExpired()
                    .isAccountNonLocked()
                    .isCredentialsNonExpired();
        }

        @Test
        @DisplayName("should preserve audit metadata in round-trip conversion")
        void shouldPreserveAuditMetadataInRoundTripConversion() {
            // Arrange
            User original = createTestUserWithAudit();

            // Act - Convert to memento and back
            UserMemento memento = UserMemento.from(original);
            User reconstructed = memento.toUser();

            // Assert - Audit metadata preserved
            UserAssert.assertThat(reconstructed)
                    .hasCreatedAt(original.getCreatedAt())
                    .hasLastModifiedAtNotNull()
                    .hasVersion(original.getVersion());
        }
    }

    @Nested
    @DisplayName("getAuditMetadata() method")
    class GetAuditMetadataMethod {

        @Test
        @DisplayName("should return audit metadata value object")
        void shouldReturnAuditMetadataValueObject() {
            // Arrange
            User user = createTestUserWithAudit();
            UserMemento memento = UserMemento.from(user);

            // Act
            AuditMetadata auditMetadata = memento.getAuditMetadata();

            // Assert
            assertThat(auditMetadata).isNotNull();
            assertThat(auditMetadata.createdAt()).isEqualTo(user.getCreatedAt());
            assertThat(auditMetadata.lastModifiedAt()).isEqualTo(user.getLastModifiedAt());
            assertThat(auditMetadata.version()).isEqualTo(user.getVersion());
        }

        @Test
        @DisplayName("should return null for new user without audit metadata")
        void shouldReturnNullForNewUserWithoutAuditMetadata() {
            // Arrange
            User user = createTestUser(); // No audit metadata
            UserMemento memento = UserMemento.from(user);

            // Act
            AuditMetadata auditMetadata = memento.getAuditMetadata();

            // Assert
            assertThat(auditMetadata).isNull();
        }
    }

    @Nested
    @DisplayName("Domain event delegation")
    class DomainEventDelegation {

        @Test
        @DisplayName("should delegate getDomainEvents to user")
        void shouldDelegateGetDomainEventsToUser() {
            // Arrange
            User user = createTestUser();
            UserMemento memento = UserMemento.from(user);

            // Act
            var events = memento.getDomainEvents();

            // Assert - User has a UserCreatedEvent
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(UserCreatedEvent.class);
        }

        @Test
        @DisplayName("should delegate clearDomainEvents to user")
        void shouldDelegateClearDomainEventsToUser() {
            // Arrange
            User user = createTestUser();
            UserMemento memento = UserMemento.from(user);

            // Act
            memento.clearDomainEvents();

            // Assert
            assertThat(user.getDomainEvents()).isEmpty();
        }

        @Test
        @DisplayName("should handle null user reference gracefully")
        void shouldHandleNullUserReferenceGracefully() {
            // Arrange - Create memento without user reference
            UserMemento memento = new UserMemento();

            // Act
            var events = memento.getDomainEvents();

            // Assert - Returns empty list when user is null
            assertThat(events).isEmpty();
        }

        @Test
        @DisplayName("should handle clearDomainEvents with null user reference")
        void shouldHandleClearDomainEventsWithNullUserReference() {
            // Arrange - Create memento without user reference
            UserMemento memento = new UserMemento();

            // Act - Should not throw exception
            memento.clearDomainEvents();

            // Assert - No exception thrown
            assertThat(true).isTrue();
        }
    }

    @Nested
    @DisplayName("Persistable implementation")
    class PersistableImplementation {

        @Test
        @DisplayName("should return correct ID")
        void shouldReturnCorrectId() {
            // Arrange
            User user = createTestUser();
            UserMemento memento = UserMemento.from(user);

            // Act
            UUID id = memento.getId();

            // Assert
            assertThat(id).isEqualTo(user.getId().uuid());
        }

        @Test
        @DisplayName("should return isNew=true for new memento")
        void shouldReturnIsNewTrueForNewMemento() {
            // Arrange
            User user = createTestUser(); // No audit metadata
            UserMemento memento = UserMemento.from(user);

            // Act
            boolean isNew = memento.isNew();

            // Assert - New mementos are marked as new
            assertThat(isNew).isTrue();
        }

        @Test
        @DisplayName("should return isNew=false for existing memento")
        void shouldReturnIsNewFalseForExistingMemento() {
            // Arrange
            User user = createTestUserWithAudit();
            UserMemento memento = UserMemento.from(user);

            // Act
            boolean isNew = memento.isNew();

            // Assert - Existing mementos are not marked as new
            assertThat(isNew).isFalse();
        }
    }
}
