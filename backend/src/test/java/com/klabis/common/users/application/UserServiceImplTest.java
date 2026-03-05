package com.klabis.common.users.application;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.common.users.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPermissionsRepository userPermissionsRepository;

    private UserService testedSubject;

    private final UserId testUserId = new UserId(java.util.UUID.randomUUID());
    private final String testUsername = "123456";
    private final String testPasswordHash = "$2a$10$hashedPassword";
    private final Set<Authority> testAuthorities = Set.of(Authority.MEMBERS_READ);

    @BeforeEach
    void setUp() {
        testedSubject = new UserServiceImpl(userRepository, userPermissionsRepository);
    }

    @Nested
    @DisplayName("createUser(username, email, authorities) method")
    class CreateUserWithEmailMethod {

        @Test
        @DisplayName("should create user with PENDING_ACTIVATION status when email provided")
        void shouldCreateUserWithPendingActivationWhenEmailProvided() {
            // Given
            String email = "user@example.com";
            User pendingUser = User.reconstruct(
                    testUserId,
                    testUsername,
                    java.util.UUID.randomUUID().toString(),
                    AccountStatus.PENDING_ACTIVATION
            );

            when(userRepository.save(any(User.class))).thenReturn(pendingUser);
            when(userPermissionsRepository.save(any(UserPermissions.class)))
                    .thenReturn(UserPermissions.create(testUserId, testAuthorities));

            // When
            UserId result = testedSubject.createUser(testUsername, email, testAuthorities);

            // Then
            assertThat(result).isEqualTo(testUserId);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getUsername()).isEqualTo(testUsername);
            assertThat(userCaptor.getValue().getAccountStatus()).isEqualTo(AccountStatus.PENDING_ACTIVATION);
            assertThat(userCaptor.getValue().isAuthenticatable()).isFalse();
        }

        @Test
        @DisplayName("should include email in UserCreatedEvent for password setup flow")
        void shouldIncludeEmailInUserCreatedEvent() {
            // Given
            String email = "user@example.com";
            User pendingUser = User.reconstruct(
                    testUserId,
                    testUsername,
                    java.util.UUID.randomUUID().toString(),
                    AccountStatus.PENDING_ACTIVATION
            );

            when(userRepository.save(any(User.class))).thenReturn(pendingUser);
            when(userPermissionsRepository.save(any(UserPermissions.class)))
                    .thenReturn(UserPermissions.create(testUserId, testAuthorities));

            // When
            testedSubject.createUser(testUsername, email, testAuthorities);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getDomainEvents()).hasSize(1);
            UserCreatedEvent event = (UserCreatedEvent) savedUser.getDomainEvents().get(0);
            assertThat(event.email()).contains(email);
        }

        @Test
        @DisplayName("should grant authorities on the created user")
        void shouldGrantAuthoritiesOnCreatedUser() {
            // Given
            String email = "user@example.com";
            User pendingUser = User.reconstruct(
                    testUserId,
                    testUsername,
                    java.util.UUID.randomUUID().toString(),
                    AccountStatus.PENDING_ACTIVATION
            );

            when(userRepository.save(any(User.class))).thenReturn(pendingUser);
            when(userPermissionsRepository.save(any(UserPermissions.class)))
                    .thenReturn(UserPermissions.create(testUserId, testAuthorities));

            // When
            testedSubject.createUser(testUsername, email, testAuthorities);

            // Then
            ArgumentCaptor<UserPermissions> permissionsCaptor = ArgumentCaptor.forClass(UserPermissions.class);
            verify(userPermissionsRepository).save(permissionsCaptor.capture());
            assertThat(permissionsCaptor.getValue().getDirectAuthorities()).isEqualTo(testAuthorities);
        }
    }

    @Nested
    @DisplayName("createActiveUser(username, passwordHash, authorities) method")
    class CreateActiveUserWithPasswordHashMethod {

        @Test
        @DisplayName("should create user with ACTIVE status when passwordHash provided")
        void shouldCreateUserWithActiveStatusWhenPasswordHashProvided() {
            // Given
            User activeUser = User.reconstruct(testUserId, testUsername, testPasswordHash, AccountStatus.ACTIVE);

            when(userRepository.save(any(User.class))).thenReturn(activeUser);
            when(userPermissionsRepository.save(any(UserPermissions.class)))
                    .thenReturn(UserPermissions.create(testUserId, testAuthorities));

            // When
            UserId result = testedSubject.createActiveUser(testUsername, testPasswordHash, testAuthorities);

            // Then
            assertThat(result).isEqualTo(testUserId);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getUsername()).isEqualTo(testUsername);
            assertThat(userCaptor.getValue().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(userCaptor.getValue().isAuthenticatable()).isTrue();
        }

        @Test
        @DisplayName("should not publish UserCreatedEvent so no password setup flow is triggered")
        void shouldNotPublishUserCreatedEvent() {
            // Given
            User activeUser = User.reconstruct(testUserId, testUsername, testPasswordHash, AccountStatus.ACTIVE);

            when(userRepository.save(any(User.class))).thenReturn(activeUser);
            when(userPermissionsRepository.save(any(UserPermissions.class)))
                    .thenReturn(UserPermissions.create(testUserId, testAuthorities));

            // When
            testedSubject.createActiveUser(testUsername, testPasswordHash, testAuthorities);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getDomainEvents()).isEmpty();
        }

        @Test
        @DisplayName("should grant authorities on the created user")
        void shouldGrantAuthoritiesOnCreatedUser() {
            // Given
            User activeUser = User.reconstruct(testUserId, testUsername, testPasswordHash, AccountStatus.ACTIVE);

            when(userRepository.save(any(User.class))).thenReturn(activeUser);
            when(userPermissionsRepository.save(any(UserPermissions.class)))
                    .thenReturn(UserPermissions.create(testUserId, testAuthorities));

            // When
            testedSubject.createActiveUser(testUsername, testPasswordHash, testAuthorities);

            // Then
            ArgumentCaptor<UserPermissions> permissionsCaptor = ArgumentCaptor.forClass(UserPermissions.class);
            verify(userPermissionsRepository).save(permissionsCaptor.capture());
            assertThat(permissionsCaptor.getValue().getDirectAuthorities()).isEqualTo(testAuthorities);
        }
    }

    @Nested
    @DisplayName("findUserByUsername() method")
    class FindUserByUsernameMethod {

        @Test
        @DisplayName("should return existing user when found")
        void shouldReturnUserWhenFound() {
            // Given
            User existingUser = User.createdUser(testUsername, testPasswordHash);
            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(existingUser));

            // When
            Optional<User> result = testedSubject.findUserByUsername(testUsername);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo(testUsername);
            verify(userRepository).findByUsername(testUsername);
        }

        @Test
        @DisplayName("should return empty optional when user not found")
        void shouldReturnEmptyWhenNotFound() {
            // Given
            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.empty());

            // When
            Optional<User> result = testedSubject.findUserByUsername(testUsername);

            // Then
            assertThat(result).isEmpty();
            verify(userRepository).findByUsername(testUsername);
        }

        @Test
        @DisplayName("should delegate to repository")
        void shouldDelegateToRepository() {
            // Given
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // When
            testedSubject.findUserByUsername("nonexistent");

            // Then
            verify(userRepository).findByUsername("nonexistent");
        }
    }

    @Nested
    @DisplayName("suspendUser() method")
    class SuspendUserMethod {

        @Test
        @DisplayName("should suspend existing user")
        void shouldSuspendExistingUser() {
            // Given
            User activeUser = User.reconstruct(testUserId, testUsername, testPasswordHash, AccountStatus.ACTIVE);
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(activeUser));
            when(userRepository.save(any(User.class))).thenReturn(activeUser);

            // When
            testedSubject.suspendUser(testUserId);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
            assertThat(userCaptor.getValue().isAuthenticatable()).isFalse();
        }

        @Test
        @DisplayName("should be idempotent - skip if already suspended")
        void shouldBeIdempotentIfAlreadySuspended() {
            // Given
            User suspendedUser = User.reconstruct(testUserId, testUsername, testPasswordHash, AccountStatus.SUSPENDED);
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(suspendedUser));

            // When
            testedSubject.suspendUser(testUserId);

            // Then - should not call save since already suspended
            verify(userRepository).findById(testUserId);
            verify(userRepository, org.mockito.Mockito.never()).save(any(User.class));
        }

        @Test
        @DisplayName("should handle non-existent user gracefully")
        void shouldHandleNonExistentUserGracefully() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

            // When - should not throw exception
            testedSubject.suspendUser(testUserId);

            // Then - should not call save
            verify(userRepository).findById(testUserId);
            verify(userRepository, org.mockito.Mockito.never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("resumeUser() method")
    class ResumeUserMethod {

        @Test
        @DisplayName("should resume suspended user")
        void shouldResumeSuspendedUser() {
            // Given
            User suspendedUser = User.reconstruct(testUserId, testUsername, testPasswordHash, AccountStatus.SUSPENDED);
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(suspendedUser));
            when(userRepository.save(any(User.class))).thenReturn(suspendedUser);

            // When
            testedSubject.resumeUser(testUserId);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(userCaptor.getValue().isAuthenticatable()).isTrue();
        }

        @Test
        @DisplayName("should be idempotent - skip if already active")
        void shouldBeIdempotentIfAlreadyActive() {
            // Given
            User activeUser = User.reconstruct(testUserId, testUsername, testPasswordHash, AccountStatus.ACTIVE);
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(activeUser));

            // When
            testedSubject.resumeUser(testUserId);

            // Then - should not call save since already active
            verify(userRepository).findById(testUserId);
            verify(userRepository, org.mockito.Mockito.never()).save(any(User.class));
        }

        @Test
        @DisplayName("should handle non-existent user gracefully")
        void shouldHandleNonExistentUserGracefully() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

            // When - should not throw exception
            testedSubject.resumeUser(testUserId);

            // Then - should not call save
            verify(userRepository).findById(testUserId);
            verify(userRepository, org.mockito.Mockito.never()).save(any(User.class));
        }
    }
}
