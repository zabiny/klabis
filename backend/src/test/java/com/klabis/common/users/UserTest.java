package com.klabis.common.users;

import com.klabis.common.users.testdata.UserTestDataBuilder;
import com.klabis.common.users.testdata.UserTestDataConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User aggregate tests")
class UserTest {

    @Nested
    @DisplayName("createdUser(username, passwordHash) factory validation")
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
            assertThatThrownBy(() -> User.createdUser(null, UserTestDataConstants.DEFAULT_PASSWORD_HASH))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Username is required");
        }

        @Test
        @DisplayName("should reject null password hash")
        void shouldRejectNullPasswordHash() {
            assertThatThrownBy(() -> User.createdUser(UserTestDataConstants.DEFAULT_ADMIN_USERNAME, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Password hash is required");
        }

        @Test
        @DisplayName("should reject empty password hash")
        void shouldRejectEmptyPasswordHash() {
            assertThatThrownBy(() -> User.createdUser(UserTestDataConstants.DEFAULT_ADMIN_USERNAME, "  "))
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
    @DisplayName("createdUser(username) with PENDING_ACTIVATION status")
    class CreateWithPendingActivationMethod {

        @Test
        @DisplayName("should create user with pending activation")
        void shouldCreateUserWithPendingActivation() {
            // When
            User user = UserTestDataBuilder.aPendingUser().build();

            // Then
            UserAssert.assertThat(user)
                    .hasIdNotNull()
                    .hasUsername(UserTestDataConstants.DEFAULT_MEMBER_USERNAME)
                    .isPendingActivationUser();
        }

        @Test
        @DisplayName("should require user name")
        void shouldRequireRegistrationNumberForPendingActivation() {
            assertThatThrownBy(() -> User.createdUser(null))
                    .isInstanceOf(NullPointerException.class);
        }
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
        @DisplayName("should be authenticatable after activation")
        void shouldBeAuthenticatableAfterActivation() {
            // Given
            User pendingUser = UserTestDataBuilder.aPendingUser().build();
            String newPasswordHash = "$2a$10$newPasswordHash...";

            // When
            User activatedUser = pendingUser.activateWithPassword(newPasswordHash);

            // Then
            UserAssert.assertThat(activatedUser)
                    .isAuthenticatable();
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
        @DisplayName("should not be authenticatable until password is set")
        void shouldNotBeEnabled() {
            User user = User.createdUser(UserTestDataConstants.DEFAULT_MEMBER_USERNAME);

            assertThat(user.isAuthenticatable()).isFalse();
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
        @DisplayName("should be authenticatable immediately")
        void shouldBeEnabled() {
            User user = User.createdUser(
                    UserTestDataConstants.DEFAULT_ADMIN_USERNAME,
                    UserTestDataConstants.DEFAULT_PASSWORD_HASH
            );

            assertThat(user.isAuthenticatable()).isTrue();
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

    @Nested
    @DisplayName("suspend() method")
    class SuspendMethod {

        @Test
        @DisplayName("should suspend active user")
        void shouldSuspendActiveUser() {
            // Given
            User activeUser = UserTestDataBuilder.anAdminUser().build();

            // When
            User suspendedUser = activeUser.suspend();

            // Then
            UserAssert.assertThat(suspendedUser)
                    .hasSameIdentityAs(activeUser)
                    .hasAccountStatus(AccountStatus.SUSPENDED)
                    .isNotEnabled();
        }

        @Test
        @DisplayName("should create new instance when suspending")
        void shouldCreateNewInstanceWhenSuspending() {
            // Given
            User activeUser = UserTestDataBuilder.anAdminUser().build();

            // When
            User suspendedUser = activeUser.suspend();

            // Then - immutability: original user unchanged
            UserAssert.assertThat(activeUser)
                    .hasAccountStatus(AccountStatus.ACTIVE)
                    .isAuthenticatable();
            assertThat(suspendedUser).isNotSameAs(activeUser);
        }

        @Test
        @DisplayName("should not be authenticatable after suspending")
        void shouldMaintainNonAuthenticationFlagsWhenSuspending() {
            // Given
            User activeUser = UserTestDataBuilder.anAdminUser().build();

            // When
            User suspendedUser = activeUser.suspend();

            // Then
            UserAssert.assertThat(suspendedUser)
                    .isNotAuthenticatable();
        }
    }

    @Nested
    @DisplayName("reactivate() method")
    class ReactivateMethod {

        @Test
        @DisplayName("should reactivate suspended user")
        void shouldReactivateSuspendedUser() {
            // Given
            User suspendedUser = UserTestDataBuilder.anAdminUser()
                    .status(AccountStatus.SUSPENDED)
                    .build();

            // When
            User reactivatedUser = suspendedUser.reactivate();

            // Then
            UserAssert.assertThat(reactivatedUser)
                    .hasSameIdentityAs(suspendedUser)
                    .hasAccountStatus(AccountStatus.ACTIVE)
                    .isAuthenticatable();
        }

        @Test
        @DisplayName("should create new instance when reactivating")
        void shouldCreateNewInstanceWhenReactivating() {
            // Given
            User suspendedUser = UserTestDataBuilder.anAdminUser()
                    .status(AccountStatus.SUSPENDED)
                    .build();

            // When
            User reactivatedUser = suspendedUser.reactivate();

            // Then - immutability: original user unchanged
            UserAssert.assertThat(suspendedUser)
                    .hasAccountStatus(AccountStatus.SUSPENDED)
                    .isNotAuthenticatable();
            assertThat(reactivatedUser).isNotSameAs(suspendedUser);
        }

        @Test
        @DisplayName("should be authenticatable after reactivating")
        void shouldMaintainNonAuthenticationFlagsWhenReactivating() {
            // Given
            User suspendedUser = UserTestDataBuilder.anAdminUser()
                    .status(AccountStatus.SUSPENDED)
                    .build();

            // When
            User reactivatedUser = suspendedUser.reactivate();

            // Then
            UserAssert.assertThat(reactivatedUser)
                    .isAuthenticatable();
        }
    }

    @Nested
    @DisplayName("isAuthenticatable() with suspended status")
    class IsAuthenticatableWithSuspendedStatus {

        @Test
        @DisplayName("should not be authenticatable after suspend()")
        void shouldNotBeAuthenticatableAfterSuspend() {
            // Given
            User activeUser = UserTestDataBuilder.anAdminUser().build();

            // When
            User suspendedUser = activeUser.suspend();

            // Then
            UserAssert.assertThat(suspendedUser)
                    .isNotAuthenticatable();
        }

        @Test
        @DisplayName("should be authenticatable after reactivate()")
        void shouldBeAuthenticatableAfterReactivate() {
            // Given
            User suspendedUser = UserTestDataBuilder.anAdminUser()
                    .status(AccountStatus.SUSPENDED)
                    .build();

            // When
            User reactivatedUser = suspendedUser.reactivate();

            // Then
            UserAssert.assertThat(reactivatedUser)
                    .isAuthenticatable();
        }
    }
}
