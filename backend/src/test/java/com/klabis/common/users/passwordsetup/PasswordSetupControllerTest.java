package com.klabis.common.users.passwordsetup;

import tools.jackson.databind.ObjectMapper;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.users.UserId;
import com.klabis.common.users.application.PasswordSetupService;
import com.klabis.common.users.domain.PasswordSetupToken;
import com.klabis.common.users.domain.PasswordValidationException;
import com.klabis.common.users.domain.TokenAlreadyUsedException;
import com.klabis.common.users.domain.TokenExpiredException;
import com.klabis.common.users.domain.TokenValidationException;
import com.klabis.common.users.domain.User;
import com.klabis.common.users.infrastructure.restapi.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PasswordSetupController.class)
@Import(EncryptionConfiguration.class)
@DisplayName("PasswordSetupController API tests")
@WithPostprocessors
class PasswordSetupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PasswordSetupService passwordSetupServiceMock;

    @Nested
    @DisplayName("POST /api/auth/password-setup/complete tests")
    class CompletePasswordSetupTests {

        @Test
        @DisplayName("should complete password setup successfully")
        void shouldCompletePasswordSetupSuccessfully() throws Exception {
            // Given
            String plainToken = UUID.randomUUID().toString();
            String password = "SecurePassword123!";
            SetPasswordRequest request =
                    new SetPasswordRequest(plainToken, password, password);

            when(passwordSetupServiceMock.completePasswordSetup(any(PasswordSetupService.SetupPasswordCommand.class), anyString()))
                    .thenReturn(User.createdUser("ZBM0101", "$2a$10$hash"));

            // When/Then
            mockMvc.perform(post("/api/auth/password-setup/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password set successfully. You can now log in."))
                    .andExpect(jsonPath("$.registrationNumber").value("ZBM0101"));
        }

        @Test
        @DisplayName("should reject password mismatch")
        void shouldRejectPasswordMismatch() throws Exception {
            // Given
            String plainToken = UUID.randomUUID().toString();
            SetPasswordRequest request =
                    new SetPasswordRequest(plainToken, "Password123!", "DifferentPassword123!");

            // When/Then — controller validates password match before calling service
            mockMvc.perform(post("/api/auth/password-setup/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Passwords do not match"));
        }

        @Test
        @DisplayName("should reject weak password")
        void shouldRejectWeakPassword() throws Exception {
            // Given
            String plainToken = UUID.randomUUID().toString();
            String weakPassword = "weak";
            SetPasswordRequest request =
                    new SetPasswordRequest(plainToken, weakPassword, weakPassword);

            when(passwordSetupServiceMock.completePasswordSetup(any(), anyString()))
                    .thenThrow(new PasswordValidationException(
                            "Password must be at least 12 characters long"));

            // When/Then
            mockMvc.perform(post("/api/auth/password-setup/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Password must be at least 12 characters long"));
        }

        @Test
        @DisplayName("should reject invalid token")
        void shouldRejectInvalidToken() throws Exception {
            // Given
            SetPasswordRequest request =
                    new SetPasswordRequest("invalid-token",
                            "SecurePassword123!",
                            "SecurePassword123!");

            when(passwordSetupServiceMock.completePasswordSetup(any(), anyString()))
                    .thenThrow(new TokenValidationException("Invalid token"));

            // When/Then
            mockMvc.perform(post("/api/auth/password-setup/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Invalid token"));
        }

        @Test
        @DisplayName("should reject expired token with 410 status")
        void shouldRejectExpiredTokenWithGoneStatus() throws Exception {
            // Given
            String plainToken = UUID.randomUUID().toString();
            SetPasswordRequest request =
                    new SetPasswordRequest(plainToken,
                            "SecurePassword123!",
                            "SecurePassword123!");

            when(passwordSetupServiceMock.completePasswordSetup(any(), anyString()))
                    .thenThrow(new TokenExpiredException("Token has expired"));

            // When/Then
            mockMvc.perform(post("/api/auth/password-setup/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isGone())
                    .andExpect(jsonPath("$.detail").value("Token has expired. Please request a new one."));
        }

        @Test
        @DisplayName("should reject used token with 410 status")
        void shouldRejectUsedTokenWithGoneStatus() throws Exception {
            // Given
            String plainToken = UUID.randomUUID().toString();
            SetPasswordRequest request =
                    new SetPasswordRequest(plainToken,
                            "SecurePassword123!",
                            "SecurePassword123!");

            when(passwordSetupServiceMock.completePasswordSetup(any(), anyString()))
                    .thenThrow(new TokenAlreadyUsedException("Token has already been used"));

            // When/Then
            mockMvc.perform(post("/api/auth/password-setup/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isGone())
                    .andExpect(jsonPath("$.detail").value("Token has already been used. Please request a new one."));
        }

        @Test
        @DisplayName("should reject request with blank token")
        void shouldRejectRequestWithBlankToken() throws Exception {
            // Given
            SetPasswordRequest request =
                    new SetPasswordRequest("", "SecurePassword123!", "SecurePassword123!");

            // When/Then
            mockMvc.perform(post("/api/auth/password-setup/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should reject request with blank password")
        void shouldRejectRequestWithBlankPassword() throws Exception {
            // Given
            SetPasswordRequest request =
                    new SetPasswordRequest(UUID.randomUUID().toString(), "", "");

            // When/Then
            mockMvc.perform(post("/api/auth/password-setup/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/auth/password-setup/validate tests")
    class ValidateTokenTests {

        @Test
        @DisplayName("should validate valid token")
        void shouldValidateValidToken() throws Exception {
            // Given
            String plainToken = UUID.randomUUID().toString();
            UserId userId = new UserId(UUID.randomUUID());
            PasswordSetupToken setupToken = PasswordSetupToken.generateFor(userId, Duration.ofHours(4));

            when(passwordSetupServiceMock.validateToken(plainToken))
                    .thenReturn(setupToken);

            // When/Then
            mockMvc.perform(get("/api/auth/password-setup/validate")
                            .param("token", plainToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true))
                    .andExpect(jsonPath("$.expiresAt").exists());
        }

        @Test
        @DisplayName("should reject invalid token")
        void shouldRejectInvalidToken() throws Exception {
            // Given
            String invalidToken = "invalid-token";

            when(passwordSetupServiceMock.validateToken(invalidToken))
                    .thenThrow(new TokenValidationException("Invalid token"));

            // When/Then
            mockMvc.perform(get("/api/auth/password-setup/validate")
                            .param("token", invalidToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Invalid token"));
        }

        @Test
        @DisplayName("should reject expired token with 410 status")
        void shouldRejectExpiredTokenWithGoneStatus() throws Exception {
            // Given
            String expiredToken = UUID.randomUUID().toString();

            when(passwordSetupServiceMock.validateToken(expiredToken))
                    .thenThrow(new TokenExpiredException("Token has expired"));

            // When/Then
            mockMvc.perform(get("/api/auth/password-setup/validate")
                            .param("token", expiredToken))
                    .andExpect(status().isGone())
                    .andExpect(jsonPath("$.detail").value("Token has expired. Please request a new one."));
        }

        @Test
        @DisplayName("should reject used token with 410 status")
        void shouldRejectUsedTokenWithGoneStatus() throws Exception {
            // Given
            String usedToken = UUID.randomUUID().toString();

            when(passwordSetupServiceMock.validateToken(usedToken))
                    .thenThrow(new TokenAlreadyUsedException("Token has already been used"));

            // When/Then
            mockMvc.perform(get("/api/auth/password-setup/validate")
                            .param("token", usedToken))
                    .andExpect(status().isGone())
                    .andExpect(jsonPath("$.detail").value("Token has already been used. Please request a new one."));
        }

        @Test
        @DisplayName("should reject request with blank token")
        void shouldRejectRequestWithBlankToken() throws Exception {
            // When/Then
            // HandlerMethodValidationException wraps the validation error and returns 500
            mockMvc.perform(get("/api/auth/password-setup/validate")
                            .param("token", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.parameterErrors[0]").value("token: must not be blank"));
        }

        @Test
        @DisplayName("should reject request with missing token parameter")
        void shouldRejectRequestWithMissingTokenParameter() throws Exception {
            // When/Then
            mockMvc.perform(get("/api/auth/password-setup/validate"))
                    .andExpect(status().isBadRequest()); // Bean validation works with filters disabled
        }
    }

    @Nested
    @DisplayName("POST /api/auth/password-setup/request tests")
    class RequestNewTokenTests {

        @Test
        @DisplayName("should return error for unsupported operation")
        void shouldReturnErrorForUnsupportedOperation() throws Exception {
            // Given
            TokenRequestRequest request =
                    new TokenRequestRequest("ZBM0101", "test@example.com");

            // When/Then - UnsupportedOperationException is not caught, returns 500
            mockMvc.perform(post("/api/auth/password-setup/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("If your account is pending activation, you will receive an email with a new setup link."));

            verify(passwordSetupServiceMock, times(1))
                    .requestNewToken("ZBM0101", "test@example.com");
        }

        @Test
        @DisplayName("should reject request with blank registration number")
        void shouldRejectRequestWithBlankRegistrationNumber() throws Exception {
            // Given
            TokenRequestRequest request =
                    new TokenRequestRequest("", "test@example.com");

            // When/Then
            mockMvc.perform(post("/api/auth/password-setup/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
