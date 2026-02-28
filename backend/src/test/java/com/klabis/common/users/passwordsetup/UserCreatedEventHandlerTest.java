package com.klabis.common.users.passwordsetup;

import com.klabis.common.users.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserCreatedEventHandler tests")
class UserCreatedEventHandlerTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordSetupService passwordSetupService;

    private UserCreatedEventHandler handler;

    private final UUID userId = UUID.randomUUID();
    private final String username = "ZBM0501";
    private final String email = "user@example.com";

    @BeforeEach
    void setUp() {
        handler = new UserCreatedEventHandler(userService, passwordSetupService);
    }

    @Nested
    @DisplayName("onUserCreated() method")
    class OnUserCreatedMethod {

        @Test
        @DisplayName("should send password setup email when event has email and PENDING_ACTIVATION status")
        void shouldSendPasswordSetupEmailWhenEmailPresentAndPendingActivation() {
            // Given
            UserCreatedEvent event = new UserCreatedEvent(
                    UUID.randomUUID(),
                    new UserId(userId),
                    username,
                    AccountStatus.PENDING_ACTIVATION,
                    Instant.now(),
                    email
            );

            User mockUser = User.createdUser(username);
            try {
                var idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(mockUser, new UserId(userId));
            } catch (Exception e) {
                throw new RuntimeException("Failed to set user ID for testing", e);
            }

            GeneratedTokenResult tokenResult = new GeneratedTokenResult(
                    mock(PasswordSetupToken.class),
                    "plain-token-123"
            );

            when(userService.findUserByUsername(username)).thenReturn(Optional.of(mockUser));
            when(passwordSetupService.generateToken(mockUser)).thenReturn(tokenResult);

            // When
            handler.onUserCreated(event);

            // Then
            verify(userService).findUserByUsername(username);
            verify(passwordSetupService).generateToken(mockUser);

            ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);

            verify(passwordSetupService).sendPasswordSetupEmailWithUsername(
                    usernameCaptor.capture(),
                    emailCaptor.capture(),
                    tokenCaptor.capture()
            );

            assertThat(usernameCaptor.getValue()).isEqualTo(username);
            assertThat(emailCaptor.getValue()).isEqualTo(email);
            assertThat(tokenCaptor.getValue()).isEqualTo("plain-token-123");
        }

        @Test
        @DisplayName("should not send email when event has no email (Optional is empty)")
        void shouldNotSendEmailWhenNoEmailInEvent() {
            // Given
            UserCreatedEvent event = new UserCreatedEvent(
                    UUID.randomUUID(),
                    new UserId(userId),
                    username,
                    AccountStatus.PENDING_ACTIVATION,
                    Instant.now(),
                    null  // No email
            );

            // When
            handler.onUserCreated(event);

            // Then
            verify(userService, never()).findUserByUsername(any());
            verify(passwordSetupService, never()).generateToken(any());
            verify(passwordSetupService, never()).sendPasswordSetupEmailWithUsername(any(), any(), any());
        }

        @Test
        @DisplayName("should not send email when account status is not PENDING_ACTIVATION")
        void shouldNotSendEmailWhenNotPendingActivation() {
            // Given
            UserCreatedEvent event = new UserCreatedEvent(
                    UUID.randomUUID(),
                    new UserId(userId),
                    username,
                    AccountStatus.ACTIVE,  // Already active
                    Instant.now(),
                    email
            );

            // When
            handler.onUserCreated(event);

            // Then
            verify(userService, never()).findUserByUsername(any());
            verify(passwordSetupService, never()).generateToken(any());
            verify(passwordSetupService, never()).sendPasswordSetupEmailWithUsername(any(), any(), any());
        }

        @Test
        @DisplayName("should throw when user not found in repository")
        void shouldThrowWhenUserNotFound() {
            // Given
            UserCreatedEvent event = new UserCreatedEvent(
                    UUID.randomUUID(),
                    new UserId(userId),
                    username,
                    AccountStatus.PENDING_ACTIVATION,
                    Instant.now(),
                    email
            );

            when(userService.findUserByUsername(username)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> handler.onUserCreated(event))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("User not found");

            verify(passwordSetupService, never()).generateToken(any());
        }

        @Test
        @DisplayName("should propagate exception from password setup service")
        void shouldPropagatePasswordSetupException() {
            // Given
            UserCreatedEvent event = new UserCreatedEvent(
                    UUID.randomUUID(),
                    new UserId(userId),
                    username,
                    AccountStatus.PENDING_ACTIVATION,
                    Instant.now(),
                    email
            );

            User mockUser = User.createdUser(username);
            try {
                var idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(mockUser, new UserId(userId));
            } catch (Exception e) {
                throw new RuntimeException("Failed to set user ID for testing", e);
            }

            GeneratedTokenResult tokenResult = new GeneratedTokenResult(
                    mock(PasswordSetupToken.class),
                    "plain-token-123"
            );

            when(userService.findUserByUsername(username)).thenReturn(Optional.of(mockUser));
            when(passwordSetupService.generateToken(mockUser)).thenReturn(tokenResult);
            doThrow(new RuntimeException("Email service error"))
                    .when(passwordSetupService)
                    .sendPasswordSetupEmailWithUsername(any(), any(), any());

            // When/Then
            assertThatThrownBy(() -> handler.onUserCreated(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Email service error");

            // Verify token was generated before exception
            verify(passwordSetupService).generateToken(mockUser);
        }

        @Test
        @DisplayName("should use factory method fromUserWithEmail when email is present")
        void shouldUseFactoryMethodWhenEmailPresent() {
            // This test verifies the correct event creation path
            UserCreatedEvent event = UserCreatedEvent.fromUserWithEmail(
                    User.createdUser(username),
                    email
            );

            assertThat(event.getEmail()).contains(email);
            assertThat(event.getAccountStatus()).isEqualTo(AccountStatus.PENDING_ACTIVATION);
        }
    }

    @Nested
    @DisplayName("Event filtering logic")
    class EventFilteringLogic {

        @Test
        @DisplayName("should handle ACTIVE status gracefully (no action)")
        void shouldHandleActiveStatusGracefully() {
            // Given
            UserCreatedEvent event = new UserCreatedEvent(
                    UUID.randomUUID(),
                    new UserId(userId),
                    username,
                    AccountStatus.ACTIVE,
                    Instant.now(),
                    email
            );

            // When
            handler.onUserCreated(event);

            // Then - no interactions with services
            verifyNoInteractions(userService, passwordSetupService);
        }

        @Test
        @DisplayName("should handle no email gracefully (no action)")
        void shouldHandleNoEmailGracefully() {
            // Given
            UserCreatedEvent event = new UserCreatedEvent(
                    UUID.randomUUID(),
                    new UserId(userId),
                    username,
                    AccountStatus.PENDING_ACTIVATION,
                    Instant.now()
                    // email is null
            );

            // When
            handler.onUserCreated(event);

            // Then - no interactions with services
            verifyNoInteractions(userService, passwordSetupService);
        }
    }
}
