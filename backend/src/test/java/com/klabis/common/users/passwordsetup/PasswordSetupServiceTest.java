package com.klabis.common.users.passwordsetup;

import com.klabis.common.email.EmailMessage;
import com.klabis.common.users.PasswordSetupToken;
import com.klabis.common.users.TokenHash;
import com.klabis.common.users.User;
import com.klabis.common.users.testdata.UserTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("PasswordSetupService tests")
class PasswordSetupServiceTest extends PasswordSetupServiceTestBase {

    private PasswordSetupService passwordSetupService;

    @BeforeEach
    void setUp() {
        passwordSetupService = createService();
    }

    @Nested
    @DisplayName("generateToken() tests")
    class GenerateTokenTests {

        @Test
        @DisplayName("should generate token for pending activation user")
        void shouldGenerateTokenForPendingActivationUser() {
            // Given
            User user = createPendingUser();
            when(tokenRepository.save(any(PasswordSetupToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            GeneratedTokenResult result = passwordSetupService.generateToken(user);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.token()).isNotNull();
            assertThat(result.plainToken()).isNotBlank();
            assertThat(result.token().getUserId()).isEqualTo(user.getId());
            assertThat(result.token().isValid()).isTrue();
            assertThat(result.token().getPlainText()).isEqualTo(result.plainToken());

            verify(tokenRepository).invalidateAllForUser(user.getId());
            verify(tokenRepository).save(any(PasswordSetupToken.class));
        }

        @Test
        @DisplayName("should invalidate existing tokens before generating new one")
        void shouldInvalidateExistingTokensBeforeGeneratingNewOne() {
            // Given
            User user = createPendingUser();
            when(tokenRepository.save(any(PasswordSetupToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            passwordSetupService.generateToken(user);

            // Then
            verify(tokenRepository).invalidateAllForUser(user.getId());
        }

        @Test
        @DisplayName("should reject null user")
        void shouldRejectNullUser() {
            assertThatThrownBy(() -> passwordSetupService.generateToken(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User is required");
        }

        @Test
        @DisplayName("should reject user that is already active")
        void shouldRejectUserThatIsAlreadyActive() {
            // Given
            User activeUser = UserTestDataBuilder.aMemberUser().build();

            // When/Then
            assertThatThrownBy(() -> passwordSetupService.generateToken(activeUser))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("User must be in PENDING_ACTIVATION status");
        }

    }

    @Nested
    @DisplayName("sendPasswordSetupEmail() tests")
    class SendPasswordSetupEmailTests {

        @Test
        @DisplayName("should send password setup email")
        void shouldSendPasswordSetupEmail() {
            // Given
            String firstName = "John";
            String email = "john.doe@example.com";
            String plainToken = UUID.randomUUID().toString();
            String expectedUrl = "https://localhost:8443/password-setup?token=" + plainToken;

            when(templateRenderer.renderHtml(any(), any())).thenReturn("<html>email body</html>");
            when(templateRenderer.renderText(any(), any())).thenReturn("text body");

            // When
            passwordSetupService.sendPasswordSetupEmail(firstName, email, plainToken);

            // Then
            ArgumentCaptor<EmailMessage> messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
            verify(emailService).send(messageCaptor.capture());

            EmailMessage capturedMessage = messageCaptor.getValue();
            assertThat(capturedMessage.to()).isEqualTo(email);
            assertThat(capturedMessage.subject()).contains("Set Your Password");
            assertThat(capturedMessage.htmlBody()).isEqualTo("<html>email body</html>");
            assertThat(capturedMessage.textBody()).isEqualTo("text body");

            // Verify template renderer was called with correct variables
            verify(templateRenderer).renderHtml(any(), argThat(vars -> {
                return firstName.equals(vars.get("firstName")) &&
                       expectedUrl.equals(vars.get("setupUrl")) &&
                       Integer.valueOf(4).equals(vars.get("expirationHours")) &&
                       "Klabis".equals(vars.get("clubName"));
            }));
        }

        @Test
        @DisplayName("should include setup link in email")
        void shouldIncludeSetupLinkInEmail() {
            // Given
            String firstName = "John";
            String email = "john.doe@example.com";
            String plainToken = "test-token-123";
            String expectedUrl = "https://localhost:8443/password-setup?token=" + plainToken;

            when(templateRenderer.renderHtml(any(), any())).thenReturn("<html>" + expectedUrl + "</html>");
            when(templateRenderer.renderText(any(), any())).thenReturn(expectedUrl);

            // When
            passwordSetupService.sendPasswordSetupEmail(firstName, email, plainToken);

            // Then
            ArgumentCaptor<EmailMessage> messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
            verify(emailService).send(messageCaptor.capture());

            EmailMessage capturedMessage = messageCaptor.getValue();
            assertThat(capturedMessage.htmlBody()).contains(expectedUrl);
            assertThat(capturedMessage.textBody()).contains(expectedUrl);

            // Verify template renderer was called with the setup URL
            verify(templateRenderer).renderHtml(any(), argThat(vars -> {
                return expectedUrl.equals(vars.get("setupUrl"));
            }));
        }
    }

    @Nested
    @DisplayName("validateToken() tests")
    class ValidateTokenTests {

        @Test
        @DisplayName("should validate valid token")
        void shouldValidateValidToken() {
            // Given
            String plainToken = "valid-token-123";
            TokenHash tokenHash = TokenHash.hash(plainToken);
            User user = createPendingUser();
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));

            when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));
            when(userRepository.findById(token.getUserId())).thenReturn(Optional.of(user));

            // When
            ValidateTokenResponse response = passwordSetupService.validateToken(plainToken);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.valid()).isTrue();
            assertThat(response.expiresAt()).isNotNull();
            assertThat(response.expiresAt()).isAfter(Instant.now());
        }

        @Test
        @DisplayName("should reject invalid token")
        void shouldRejectInvalidToken() {
            // Given
            String plainToken = "invalid-token";
            TokenHash tokenHash = TokenHash.hash(plainToken);

            when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> passwordSetupService.validateToken(plainToken))
                    .isInstanceOf(TokenValidationException.class)
                    .hasMessage("Invalid token");
        }

        @Test
        @DisplayName("should reject expired token")
        void shouldRejectExpiredToken() {
            // Given
            String plainToken = "expired-token";
            TokenHash tokenHash = TokenHash.hash(plainToken);
            User user = createPendingUser();
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofMillis(1)); // Very short expiry
            // Wait for token to expire
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

            // When/Then
            assertThatThrownBy(() -> passwordSetupService.validateToken(plainToken))
                    .isInstanceOf(TokenValidationException.class)
                    .hasMessage("Token has expired");
        }

        @Test
        @DisplayName("should reject used token")
        void shouldRejectUsedToken() {
            // Given
            String plainToken = "used-token";
            TokenHash tokenHash = TokenHash.hash(plainToken);
            User user = createPendingUser();
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));
            token.markAsUsed("192.168.1.1");

            when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

            // When/Then
            assertThatThrownBy(() -> passwordSetupService.validateToken(plainToken))
                    .isInstanceOf(TokenValidationException.class)
                    .hasMessage("Token has already been used");
        }

        @Test
        @DisplayName("should reject token when user not found")
        void shouldRejectTokenWhenUserNotFound() {
            // Given
            String plainToken = "valid-token";
            TokenHash tokenHash = TokenHash.hash(plainToken);
            User user = createPendingUser();
            PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), Duration.ofHours(4));

            when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));
            when(userRepository.findById(token.getUserId())).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> passwordSetupService.validateToken(plainToken))
                    .isInstanceOf(TokenValidationException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("should reject token when user is not pending activation")
        void shouldRejectTokenWhenUserIsNotPendingActivation() {
            // Given
            String plainToken = "valid-token";
            TokenHash tokenHash = TokenHash.hash(plainToken);
            User activeUser = UserTestDataBuilder.aMemberUser().build();
            PasswordSetupToken token = PasswordSetupToken.generateFor(activeUser.getId(), Duration.ofHours(4));

            when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));
            when(userRepository.findById(token.getUserId())).thenReturn(Optional.of(activeUser));

            // When/Then
            assertThatThrownBy(() -> passwordSetupService.validateToken(plainToken))
                    .isInstanceOf(TokenValidationException.class)
                    .hasMessage("Account is not in pending activation status");
        }
    }

    @Nested
    @DisplayName("completePasswordSetup() tests")
    class CompletePasswordSetupTests {

        @Test
        @DisplayName("should complete password setup successfully")
        void shouldCompletePasswordSetupSuccessfully() {
            // Given
            String plainToken = "valid-token";
            String password = "SecurePassword123!";
            String ipAddress = "192.168.1.1";
            TokenHash tokenHash = TokenHash.hash(plainToken);
            User pendingUser = createPendingUser();
            PasswordSetupToken token = PasswordSetupToken.generateFor(pendingUser.getId(), Duration.ofHours(4));
            String encodedPassword = "$2a$10$encodedPassword";

            when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));
            when(userRepository.findById(token.getUserId())).thenReturn(Optional.of(pendingUser));
            when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(tokenRepository.save(any(PasswordSetupToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PasswordSetupRequest request =
                    new PasswordSetupRequest(plainToken, password, password);

            // When
            PasswordSetupResponse response =
                    passwordSetupService.completePasswordSetup(request, ipAddress);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.message()).contains("Password set successfully");
            assertThat(response.registrationNumber()).isEqualTo(pendingUser.getUsername());

            verify(passwordValidator).validateBasic(password);
            verify(passwordEncoder).encode(password);
            verify(userRepository).save(any(User.class));
            verify(tokenRepository).save(any(PasswordSetupToken.class));
        }

        @Test
        @DisplayName("should reject password mismatch")
        void shouldRejectPasswordMismatch() {
            // Given
            String plainToken = "valid-token";
            TokenHash tokenHash = TokenHash.hash(plainToken);
            User pendingUser = createPendingUser();
            PasswordSetupToken token = PasswordSetupToken.generateFor(pendingUser.getId(), Duration.ofHours(4));

            when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));
            when(userRepository.findById(token.getUserId())).thenReturn(Optional.of(pendingUser));

            PasswordSetupRequest request =
                    new PasswordSetupRequest(plainToken, "Password123!", "DifferentPassword123!");

            // When/Then
            assertThatThrownBy(() -> passwordSetupService.completePasswordSetup(request, "192.168.1.1"))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessage("Passwords do not match");

            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("should reject invalid password complexity")
        void shouldRejectInvalidPasswordComplexity() {
            // Given
            String plainToken = "valid-token";
            String weakPassword = "weak";
            TokenHash tokenHash = TokenHash.hash(plainToken);
            User pendingUser = createPendingUser();
            PasswordSetupToken token = PasswordSetupToken.generateFor(pendingUser.getId(), Duration.ofHours(4));

            when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));
            when(userRepository.findById(token.getUserId())).thenReturn(Optional.of(pendingUser));

            // Set up the validator to throw exception for weak password
            doThrow(new PasswordValidationException("Password must be at least 12 characters long"))
                    .when(passwordValidator).validateBasic(weakPassword);

            PasswordSetupRequest request =
                    new PasswordSetupRequest(plainToken, weakPassword, weakPassword);

            // When/Then
            assertThatThrownBy(() -> passwordSetupService.completePasswordSetup(request, "192.168.1.1"))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessageContaining("Password must be at least 12 characters long");

            verify(passwordValidator).validateBasic(weakPassword);
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("should reject expired token")
        void shouldRejectExpiredToken() {
            // Given
            String plainToken = "expired-token";
            TokenHash tokenHash = TokenHash.hash(plainToken);
            User pendingUser = createPendingUser();
            PasswordSetupToken token = PasswordSetupToken.generateFor(pendingUser.getId(), Duration.ofMillis(1));
            // Wait for token to expire
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

            PasswordSetupRequest request =
                    new PasswordSetupRequest(plainToken, "SecurePassword123!", "SecurePassword123!");

            // When/Then
            assertThatThrownBy(() -> passwordSetupService.completePasswordSetup(request, "192.168.1.1"))
                    .isInstanceOf(TokenValidationException.class)
                    .hasMessage("Token has expired");

            verify(passwordValidator, never()).validateBasic(anyString());
        }

        @Test
        @DisplayName("should mark token as used after successful setup")
        void shouldMarkTokenAsUsedAfterSuccessfulSetup() {
            // Given
            String plainToken = "valid-token";
            String password = "SecurePassword123!";
            String ipAddress = "192.168.1.100";
            TokenHash tokenHash = TokenHash.hash(plainToken);
            User pendingUser = createPendingUser();
            PasswordSetupToken token = PasswordSetupToken.generateFor(pendingUser.getId(), Duration.ofHours(4));
            String encodedPassword = "$2a$10$encodedPassword";

            when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));
            when(userRepository.findById(token.getUserId())).thenReturn(Optional.of(pendingUser));
            when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(tokenRepository.save(any(PasswordSetupToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PasswordSetupRequest request =
                    new PasswordSetupRequest(plainToken, password, password);

            // When
            passwordSetupService.completePasswordSetup(request, ipAddress);

            // Then
            assertThat(token.isUsed()).isTrue();
            assertThat(token.getUsedByIp()).isEqualTo(ipAddress);
        }
    }

    @Nested
    @DisplayName("requestNewToken() tests")
    class RequestNewTokenTests {

        @Test
        @DisplayName("should generate and send new token for pending activation user")
        void shouldGenerateNewTokenForPendingActivationUser() {
            // Given
            String registrationNumber = "ZBM0101";
            String email = "test@example.com";
            User user = createPendingUser();

            when(userRepository.findByUsername(registrationNumber)).thenReturn(Optional.of(user));
            when(tokenRepository.save(any(PasswordSetupToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(templateRenderer.renderHtml(any(), any())).thenReturn("<html>email body</html>");
            when(templateRenderer.renderText(any(), any())).thenReturn("text body");

            // When
            passwordSetupService.requestNewToken(registrationNumber, email);

            // Then
            verify(tokenRepository).invalidateAllForUser(user.getId());
            verify(tokenRepository).save(any(PasswordSetupToken.class));
            verify(emailService).send(any(EmailMessage.class));
        }

        @Test
        @DisplayName("should throw TokenValidationException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            String registrationNumber = "ZBM0101";
            String email = "test@example.com";

            when(userRepository.findByUsername(registrationNumber)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> passwordSetupService.requestNewToken(registrationNumber, email))
                    .isInstanceOf(TokenValidationException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("should throw TokenValidationException when account is not pending activation")
        void shouldThrowExceptionWhenAccountNotPendingActivation() {
            // Given
            String registrationNumber = "ZBM0101";
            String email = "test@example.com";
            User activeUser = UserTestDataBuilder.aMemberUser().build();

            when(userRepository.findByUsername(registrationNumber)).thenReturn(Optional.of(activeUser));

            // When/Then
            assertThatThrownBy(() -> passwordSetupService.requestNewToken(registrationNumber, email))
                    .isInstanceOf(TokenValidationException.class)
                    .hasMessageContaining("not in pending activation status");
        }
    }

    // Helper methods

    private User createPendingUser() {
        return UserTestDataBuilder.aPendingUser().build();
    }
}
