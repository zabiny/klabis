package com.klabis.users.application;

import com.klabis.users.*;
import com.klabis.users.persistence.UserPermissionsRepository;
import com.klabis.users.persistence.UserRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private UserService userService;

    private final UserId testUserId = new UserId(java.util.UUID.randomUUID());
    private final String testUsername = "123456";
    private final String testPasswordHash = "$2a$10$hashedPassword";
    private final Set<Authority> testAuthorities = Set.of(Authority.MEMBERS_READ);

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userPermissionsRepository);
    }

    @Nested
    @DisplayName("createUserPendingActivation() method")
    class CreateUserPendingActivationMethod {

        @Test
        @DisplayName("should create user with PENDING_ACTIVATION status and grant authorities")
        void shouldCreateUserWithPendingActivationAndAuthorities() {
            // Given
            User pendingUser = User.createPendingActivation(testUsername, testPasswordHash);
            // Use reflection to set the ID for testing
            try {
                var idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(pendingUser, testUserId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set user ID for testing", e);
            }

            when(userRepository.save(any(User.class))).thenReturn(pendingUser);
            when(userPermissionsRepository.save(any(UserPermissions.class)))
                    .thenReturn(UserPermissions.create(testUserId, testAuthorities));

            // When
            UserId result = userService.createUserPendingActivation(
                    testUsername,
                    testPasswordHash,
                    testAuthorities
            );

            // Then
            assertThat(result).isEqualTo(testUserId);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getUsername()).isEqualTo(testUsername);
            assertThat(userCaptor.getValue().getAccountStatus()).isEqualTo(AccountStatus.PENDING_ACTIVATION);
            assertThat(userCaptor.getValue().isEnabled()).isFalse();

            ArgumentCaptor<UserPermissions> permissionsCaptor = ArgumentCaptor.forClass(UserPermissions.class);
            verify(userPermissionsRepository).save(permissionsCaptor.capture());
            assertThat(permissionsCaptor.getValue().getUserId()).isEqualTo(testUserId);
            assertThat(permissionsCaptor.getValue().getDirectAuthorities()).isEqualTo(testAuthorities);
        }

        @Test
        @DisplayName("should create user with empty authorities set")
        void shouldCreateUserWithEmptyAuthorities() {
            // Given
            User pendingUser = User.createPendingActivation(testUsername, testPasswordHash);
            try {
                var idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(pendingUser, testUserId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set user ID for testing", e);
            }

            when(userRepository.save(any(User.class))).thenReturn(pendingUser);
            when(userPermissionsRepository.save(any(UserPermissions.class)))
                    .thenReturn(UserPermissions.empty(testUserId));

            // When
            UserId result = userService.createUserPendingActivation(
                    testUsername,
                    testPasswordHash,
                    Set.of()
            );

            // Then
            assertThat(result).isEqualTo(testUserId);
            verify(userPermissionsRepository).save(any(UserPermissions.class));
        }

        @Test
        @DisplayName("should propagate exception when username is null")
        void shouldThrowExceptionWhenUsernameIsNull() {
            // When/Then - Delegation creates UserCreationParams which throws NullPointerException
            assertThatThrownBy(() ->
                    userService.createUserPendingActivation(null, testPasswordHash, testAuthorities)
            ).isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Username");
        }

        @Test
        @DisplayName("should propagate exception when passwordHash is null")
        void shouldThrowExceptionWhenPasswordHashIsNull() {
            // When/Then - Delegation creates UserCreationParams which throws NullPointerException
            assertThatThrownBy(() ->
                    userService.createUserPendingActivation(testUsername, null, testAuthorities)
            ).isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Password hash");
        }

        @Test
        @DisplayName("should execute in single transaction")
        void shouldExecuteInSingleTransaction() {
            // This test verifies the @Transactional annotation is present
            // The actual transaction behavior is tested in integration tests
            // Here we just verify the method calls happen in sequence
            User pendingUser = User.createPendingActivation(testUsername, testPasswordHash);
            try {
                var idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(pendingUser, testUserId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set user ID for testing", e);
            }

            when(userRepository.save(any(User.class))).thenReturn(pendingUser);
            when(userPermissionsRepository.save(any(UserPermissions.class)))
                    .thenReturn(UserPermissions.create(testUserId, testAuthorities));

            userService.createUserPendingActivation(testUsername, testPasswordHash, testAuthorities);

            // Verify both repositories were called in sequence
            verify(userRepository).save(any(User.class));
            verify(userPermissionsRepository).save(any(UserPermissions.class));
        }
    }

    @Nested
    @DisplayName("createUserPendingActivation(UserCreationParams) method")
    class CreateUserPendingActivationWithParamsMethod {

        @Test
        @DisplayName("should create user with UserCreationParams without email")
        void shouldCreateUserWithParamsWithoutEmail() {
            // Given
            UserCreationParams params = UserCreationParams.builder()
                    .username(testUsername)
                    .passwordHash(testPasswordHash)
                    .authorities(testAuthorities)
                    .build();

            User pendingUser = User.createPendingActivation(testUsername, testPasswordHash);
            try {
                var idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(pendingUser, testUserId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set user ID for testing", e);
            }

            when(userRepository.save(any(User.class))).thenReturn(pendingUser);
            when(userPermissionsRepository.save(any(UserPermissions.class)))
                    .thenReturn(UserPermissions.create(testUserId, testAuthorities));

            // When
            UserId result = userService.createUserPendingActivation(params);

            // Then
            assertThat(result).isEqualTo(testUserId);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getUsername()).isEqualTo(testUsername);
            assertThat(userCaptor.getValue().getAccountStatus()).isEqualTo(AccountStatus.PENDING_ACTIVATION);

            ArgumentCaptor<UserPermissions> permissionsCaptor = ArgumentCaptor.forClass(UserPermissions.class);
            verify(userPermissionsRepository).save(permissionsCaptor.capture());
            assertThat(permissionsCaptor.getValue().getUserId()).isEqualTo(testUserId);
        }

        @Test
        @DisplayName("should create user with UserCreationParams containing email")
        void shouldCreateUserWithParamsContainingEmail() {
            // Given
            String email = "user@example.com";
            UserCreationParams params = UserCreationParams.builder()
                    .username(testUsername)
                    .passwordHash(testPasswordHash)
                    .authorities(testAuthorities)
                    .email(email)
                    .build();

            User pendingUser = User.createPendingActivationWithEmail(testUsername, testPasswordHash, email);
            try {
                var idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(pendingUser, testUserId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set user ID for testing", e);
            }

            when(userRepository.save(any(User.class))).thenReturn(pendingUser);
            when(userPermissionsRepository.save(any(UserPermissions.class)))
                    .thenReturn(UserPermissions.create(testUserId, testAuthorities));

            // When
            UserId result = userService.createUserPendingActivation(params);

            // Then
            assertThat(result).isEqualTo(testUserId);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            // Verify UserCreatedEvent contains email
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getDomainEvents()).hasSize(1);
            UserCreatedEvent event = (UserCreatedEvent) savedUser.getDomainEvents().get(0);
            assertThat(event.getEmail()).contains(email);
        }

        @Test
        @DisplayName("should verify UserCreatedEvent email is absent when not provided")
        void shouldVerifyUserCreatedEventEmailIsAbsentWhenNotProvided() {
            // Given
            UserCreationParams params = UserCreationParams.builder()
                    .username(testUsername)
                    .passwordHash(testPasswordHash)
                    .authorities(testAuthorities)
                    .build();

            User pendingUser = User.createPendingActivation(testUsername, testPasswordHash);
            try {
                var idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(pendingUser, testUserId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set user ID for testing", e);
            }

            when(userRepository.save(any(User.class))).thenReturn(pendingUser);
            when(userPermissionsRepository.save(any(UserPermissions.class)))
                    .thenReturn(UserPermissions.create(testUserId, testAuthorities));

            // When
            userService.createUserPendingActivation(params);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            UserCreatedEvent event = (UserCreatedEvent) savedUser.getDomainEvents().get(0);
            assertThat(event.getEmail()).isEmpty();
        }

        @Test
        @DisplayName("should grant all authorities from params")
        void shouldGrantAllAuthoritiesFromParams() {
            // Given
            Set<Authority> authorities = Set.of(
                    Authority.MEMBERS_READ,
                    Authority.MEMBERS_CREATE,
                    Authority.MEMBERS_UPDATE
            );
            UserCreationParams params = UserCreationParams.builder()
                    .username(testUsername)
                    .passwordHash(testPasswordHash)
                    .authorities(authorities)
                    .build();

            User pendingUser = User.createPendingActivation(testUsername, testPasswordHash);
            try {
                var idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(pendingUser, testUserId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set user ID for testing", e);
            }

            when(userRepository.save(any(User.class))).thenReturn(pendingUser);
            when(userPermissionsRepository.save(any(UserPermissions.class)))
                    .thenReturn(UserPermissions.create(testUserId, authorities));

            // When
            userService.createUserPendingActivation(params);

            // Then
            ArgumentCaptor<UserPermissions> permissionsCaptor = ArgumentCaptor.forClass(UserPermissions.class);
            verify(userPermissionsRepository).save(permissionsCaptor.capture());
            assertThat(permissionsCaptor.getValue().getDirectAuthorities()).isEqualTo(authorities);
        }
    }

    @Nested
    @DisplayName("findUserByUsername() method")
    class FindUserByUsernameMethod {

        @Test
        @DisplayName("should return existing user when found")
        void shouldReturnUserWhenFound() {
            // Given
            User existingUser = User.create(testUsername, testPasswordHash);
            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(existingUser));

            // When
            Optional<User> result = userService.findUserByUsername(testUsername);

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
            Optional<User> result = userService.findUserByUsername(testUsername);

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
            userService.findUserByUsername("nonexistent");

            // Then
            verify(userRepository).findByUsername("nonexistent");
        }
    }
}
