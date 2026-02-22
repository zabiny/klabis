package com.klabis.users;

import com.klabis.users.testdata.UserTestDataBuilder;
import com.klabis.users.testdata.UserTestDataConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User aggregate tests")
class UserTest {

    @Nested
    @DisplayName("create() method")
    class CreateMethod {

        @Test
        @DisplayName("should create user with ROLE_ADMIN")
        void shouldCreateAdminUser() {
            User user = UserTestDataBuilder.anAdminUser().build();

            UserAssert.assertThat(user)
                    .hasIdNotNull()
                    .hasUsername(UserTestDataConstants.DEFAULT_ADMIN_USERNAME)
                    .hasPasswordHash(UserTestDataConstants.DEFAULT_PASSWORD_HASH)
                    .isActiveUser();
        }

        @Test
        @DisplayName("should create user with ROLE_MEMBER")
        void shouldCreateMemberUser() {
            User user = UserTestDataBuilder.aMemberUser().build();

            UserAssert.assertThat(user)
                    .hasAccountStatus(AccountStatus.ACTIVE)
                    .isActiveUser();
        }

        @Test
        @DisplayName("should create user with custom account status")
        void shouldCreateUserWithCustomAccountStatus() {
            User user = UserTestDataBuilder.anAdminUser()
                    .status(AccountStatus.PENDING_ACTIVATION)
                    .build();

            UserAssert.assertThat(user)
                    .hasAccountStatus(AccountStatus.PENDING_ACTIVATION);
        }

        @Test
        @DisplayName("should have unique ID for each user")
        void shouldHaveUniqueId() {
            User user1 = UserTestDataBuilder.anAdminUser().build();
            User user2 = UserTestDataBuilder.aMemberUser().build();

            // IDs should be different for different users
            assertThat(user1.getId()).isNotEqualTo(user2.getId());
        }

        @Test
        @DisplayName("should reject null user name")
        void shouldRejectNullUserName() {
            assertThatThrownBy(() -> User.create(null, UserTestDataConstants.DEFAULT_PASSWORD_HASH))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("User name is required");
        }

        @Test
        @DisplayName("should reject null password hash")
        void shouldRejectNullPasswordHash() {
            assertThatThrownBy(() -> User.create(UserTestDataConstants.DEFAULT_ADMIN_USERNAME, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Password hash is required");
        }

        @Test
        @DisplayName("should reject empty password hash")
        void shouldRejectEmptyPasswordHash() {
            assertThatThrownBy(() -> User.create(UserTestDataConstants.DEFAULT_ADMIN_USERNAME, "  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Password hash is required");
        }

    }

    @Nested
    @DisplayName("isAuthenticatable() method")
    class IsAuthenticatableMethod {

        @Test
        @DisplayName("should be authenticatable when ACTIVE and enabled")
        void shouldBeAuthenticatableWhenActiveAndEnabled() {
            User user = UserTestDataBuilder.anAdminUser().build();

            UserAssert.assertThat(user)
                    .isAuthenticatable();
        }

        @Test
        @DisplayName("should not be authenticatable when SUSPENDED")
        void shouldNotBeAuthenticatableWhenSuspended() {
            User user = UserTestDataBuilder.anAdminUser()
                    .status(AccountStatus.SUSPENDED)
                    .build();

            UserAssert.assertThat(user)
                    .isNotAuthenticatable();
        }

        @Test
        @DisplayName("should not be authenticatable when PENDING_ACTIVATION")
        void shouldNotBeAuthenticatableWhenPendingActivation() {
            User user = UserTestDataBuilder.aPendingUser().build();

            UserAssert.assertThat(user)
                    .isNotAuthenticatable();
        }
    }

    @Nested
    @DisplayName("createPendingActivation() method")
    class CreatePendingActivationMethod {

        @Test
        @DisplayName("should create user with pending activation")
        void shouldCreateUserWithPendingActivation() {
            // When
            User user = UserTestDataBuilder.aPendingUser().build();

            // Then
            UserAssert.assertThat(user)
                    .hasIdNotNull()
                    .hasUsername(UserTestDataConstants.DEFAULT_MEMBER_USERNAME)
                    .hasPasswordHash(UserTestDataConstants.DEFAULT_PASSWORD_HASH)
                    .isPendingActivationUser();
        }

        @Test
        @DisplayName("should require user name for pending activation")
        void shouldRequireRegistrationNumberForPendingActivation() {
            assertThatThrownBy(() -> User.createPendingActivation(null, UserTestDataConstants.DEFAULT_PASSWORD_HASH))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("User name is required");
        }

        @Test
        @DisplayName("should require password hash for pending activation")
        void shouldRequirePasswordHashForPendingActivation() {
            assertThatThrownBy(() -> User.createPendingActivation(UserTestDataConstants.DEFAULT_MEMBER_USERNAME, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Password hash is required");
        }

        // REMOVED: pending activation authorities test
        // Authorities are no longer part of User entity
    }

    @Nested
    @DisplayName("activateWithPassword() method")
    class ActivateWithPasswordMethod {

        @Test
        @DisplayName("should activate user with new password")
        void shouldActivateUserWithNewPassword() {
            // Given - user with pending activation
            User pendingUser = UserTestDataBuilder.aPendingUser().build();
            String newPasswordHash = "$2a$10$newPasswordHashForActivation...";

            // When
            User activatedUser = pendingUser.activateWithPassword(newPasswordHash);

            // Then
            UserAssert.assertThat(activatedUser)
                    .hasSameIdentityAs(pendingUser)
                    .hasPasswordHash(newPasswordHash)
                    .isActiveUser();
        }

        @Test
        @DisplayName("should require new password hash for activation")
        void shouldRequireNewPasswordHashForActivation() {
            // Given
            User pendingUser = UserTestDataBuilder.aPendingUser().build();

            assertThatThrownBy(() -> pendingUser.activateWithPassword((String) null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("New password hash is required");
        }


        @Test
        @DisplayName("should maintain all non-authentication flags when activating")
        void shouldMaintainAllNonAuthenticationFlagsWhenActivating() {
            // Given
            User pendingUser = UserTestDataBuilder.aPendingUser().build();
            String newPasswordHash = "$2a$10$newPasswordHash...";

            // When
            User activatedUser = pendingUser.activateWithPassword(newPasswordHash);

            // Then
            UserAssert.assertThat(activatedUser)
                    .isAccountNonExpired()
                    .isAccountNonLocked()
                    .isCredentialsNonExpired();
        }
    }

    @Nested
    @DisplayName("createdUser(username) method")
    class CreatedUserWithUsernameOnly {

        @Test
        @DisplayName("should create user with PENDING_ACTIVATION status")
        void shouldCreateUserWithPendingActivationStatus() {
            User user = User.createdUser(UserTestDataConstants.DEFAULT_MEMBER_USERNAME);

            UserAssert.assertThat(user)
                    .hasAccountStatus(AccountStatus.PENDING_ACTIVATION);
        }

        @Test
        @DisplayName("should generate a non-blank placeholder password hash internally")
        void shouldGenerateNonBlankPasswordHash() {
            User user = User.createdUser(UserTestDataConstants.DEFAULT_MEMBER_USERNAME);

            assertThat(user.getPasswordHash()).isNotBlank();
        }

        @Test
        @DisplayName("should not be enabled until password is set")
        void shouldNotBeEnabled() {
            User user = User.createdUser(UserTestDataConstants.DEFAULT_MEMBER_USERNAME);

            assertThat(user.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should publish UserCreatedEvent")
        void shouldPublishUserCreatedEvent() {
            User user = User.createdUser(UserTestDataConstants.DEFAULT_MEMBER_USERNAME);

            assertThat(user.getDomainEvents()).hasSize(1)
                    .first()
                    .isInstanceOf(UserCreatedEvent.class);
        }

        @Test
        @DisplayName("should require username")
        void shouldRequireUsername() {
            assertThatThrownBy(() -> User.createdUser(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("createdUser(username, passwordHash) method")
    class CreatedUserWithPasswordHash {

        @Test
        @DisplayName("should create user with ACTIVE status")
        void shouldCreateUserWithActiveStatus() {
            User user = User.createdUser(
                    UserTestDataConstants.DEFAULT_ADMIN_USERNAME,
                    UserTestDataConstants.DEFAULT_PASSWORD_HASH
            );

            UserAssert.assertThat(user)
                    .hasAccountStatus(AccountStatus.ACTIVE);
        }

        @Test
        @DisplayName("should store the provided password hash")
        void shouldStoreProvidedPasswordHash() {
            User user = User.createdUser(
                    UserTestDataConstants.DEFAULT_ADMIN_USERNAME,
                    UserTestDataConstants.DEFAULT_PASSWORD_HASH
            );

            UserAssert.assertThat(user)
                    .hasPasswordHash(UserTestDataConstants.DEFAULT_PASSWORD_HASH);
        }

        @Test
        @DisplayName("should be enabled immediately")
        void shouldBeEnabled() {
            User user = User.createdUser(
                    UserTestDataConstants.DEFAULT_ADMIN_USERNAME,
                    UserTestDataConstants.DEFAULT_PASSWORD_HASH
            );

            assertThat(user.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should not publish UserCreatedEvent")
        void shouldNotPublishUserCreatedEvent() {
            User user = User.createdUser(
                    UserTestDataConstants.DEFAULT_ADMIN_USERNAME,
                    UserTestDataConstants.DEFAULT_PASSWORD_HASH
            );

            assertThat(user.getDomainEvents()).isEmpty();
        }

        @Test
        @DisplayName("should require username")
        void shouldRequireUsername() {
            assertThatThrownBy(() -> User.createdUser(null, UserTestDataConstants.DEFAULT_PASSWORD_HASH))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should require password hash")
        void shouldRequirePasswordHash() {
            assertThatThrownBy(() -> User.createdUser(UserTestDataConstants.DEFAULT_ADMIN_USERNAME, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Equality methods")
    class EqualityMethods {

        @Test
        @DisplayName("should be equal based on ID")
        void shouldBeEqualBasedOnId() {
            User user1 = UserTestDataBuilder.anAdminUser().build();
            User user2 = UserTestDataBuilder.aMemberUser().build();

            // Different users should not be equal
            UserAssert.assertThat(user1).isNotEqualTo(user2);
        }
    }
}
