package com.klabis.common.users.passwordsetup;

import tools.jackson.databind.ObjectMapper;
import com.klabis.common.CommonWebMvcTest;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CommonWebMvcTest
@DisplayName("PasswordSetupController API tests")
class PasswordSetupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
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
            SetPasswordRequest request = SetPasswordRequestBuilder.builder()
                    .token(plainToken)
                    .password(password)
                    .passwordConfirmation(password)
                    .build();

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
            SetPasswordRequest request = SetPasswordRequestBuilder.builder()
                    .token(plainToken)
                    .password("Password123!")
                    .passwordConfirmation("DifferentPassword123!")
                    .build();

            // When/Then — controller validates password match before calling service
            mockMvc.perform(post("/api/auth/password-setup/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/errors/passwords-do-not-match"))
                    .andExpect(jsonPath("$.field").value("passwordConfirmation"))
                    .andExpect(jsonPath("$.detail").value("Passwords do not match"));
        }

        @Test
        @DisplayName("should reject weak password")
        void shouldRejectWeakPassword() throws Exception {
            // Given
            String plainToken = UUID.randomUUID().toString();
            String weakPassword = "weak";
            SetPasswordRequest request = SetPasswordRequestBuilder.builder()
                    .token(plainToken)
                    .password(weakPassword)
                    .passwordConfirmation(weakPassword)
                    .build();

            when(passwordSetupServiceMock.completePasswordSetup(any(), anyString()))
                    .thenThrow(new PasswordValidationException(PasswordValidationException.Violation.TOO_SHORT));

            // When/Then
            mockMvc.perform(post("/api/auth/password-setup/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/errors/password-requirements-not-met"))
                    .andExpect(jsonPath("$.violations[0]").value("TOO_SHORT"))
                    .andExpect(jsonPath("$.minLength").value(12))
                    .andExpect(jsonPath("$.detail").value("Password must be at least 12 characters long."));
        }

        @Test
        @DisplayName("should reject invalid token")
        void shouldRejectInvalidToken() throws Exception {
            // Given
            SetPasswordRequest request = SetPasswordRequestBuilder.builder()
                    .token("invalid-token")
                    .password("SecurePassword123!")
                    .passwordConfirmation("SecurePassword123!")
                    .build();

            when(passwordSetupServiceMock.completePasswordSetup(any(), anyString()))
                    .thenThrow(new TokenValidationException("Invalid token"));

            // When/Then
            mockMvc.perform(post("/api/auth/password-setup/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/errors/invalid-token"))
                    .andExpect(jsonPath("$.detail").value("Invalid token"));
        }

        @Test
        @DisplayName("should reject expired token with 410 status")
        void shouldRejectExpiredTokenWithGoneStatus() throws Exception {
            // Given
            String plainToken = UUID.randomUUID().toString();
            SetPasswordRequest request = SetPasswordRequestBuilder.builder()
                    .token(plainToken)
                    .password("SecurePassword123!")
                    .passwordConfirmation("SecurePassword123!")
                    .build();

            when(passwordSetupServiceMock.completePasswordSetup(any(), anyString()))
                    .thenThrow(new TokenExpiredException("Token has expired"));

            // When/Then
            mockMvc.perform(post("/api/auth/password-setup/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isGone())
                    .andExpect(jsonPath("$.type").value("/errors/token-expired"))
                    .andExpect(jsonPath("$.detail").value("Token has expired"));
        }

        @Test
        @DisplayName("should reject used token with 410 status")
        void shouldRejectUsedTokenWithGoneStatus() throws Exception {
            // Given
            String plainToken = UUID.randomUUID().toString();
            SetPasswordRequest request = SetPasswordRequestBuilder.builder()
                    .token(plainToken)
                    .password("SecurePassword123!")
                    .passwordConfirmation("SecurePassword123!")
                    .build();

            when(passwordSetupServiceMock.completePasswordSetup(any(), anyString()))
                    .thenThrow(new TokenAlreadyUsedException("Token has already been used"));

            // When/Then
            mockMvc.perform(post("/api/auth/password-setup/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isGone())
                    .andExpect(jsonPath("$.type").value("/errors/token-already-used"))
                    .andExpect(jsonPath("$.detail").value("Token has already been used"));
        }

        @Test
        @DisplayName("should reject request with blank token")
        void shouldRejectRequestWithBlankToken() throws Exception {
            // Given
            SetPasswordRequest request = SetPasswordRequestBuilder.builder()
                    .token("")
                    .password("SecurePassword123!")
                    .passwordConfirmation("SecurePassword123!")
                    .build();

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
            SetPasswordRequest request = SetPasswordRequestBuilder.builder()
                    .token(UUID.randomUUID().toString())
                    .password("")
                    .passwordConfirmation("")
                    .build();

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
                    .andExpect(jsonPath("$.type").value("/errors/invalid-token"))
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
                    .andExpect(jsonPath("$.type").value("/errors/token-expired"))
                    .andExpect(jsonPath("$.detail").value("Token has expired"));
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
                    .andExpect(jsonPath("$.type").value("/errors/token-already-used"))
                    .andExpect(jsonPath("$.detail").value("Token has already been used"));
        }

        @Test
        @DisplayName("should reject request with blank token")
        void shouldRejectRequestWithBlankToken() throws Exception {
            // When/Then
            mockMvc.perform(get("/api/auth/password-setup/validate")
                            .param("token", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.parameterErrors[0]").value("musí odpovídat \"^(?!\\s*$).+\""));
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
            TokenRequestRequest request = TokenRequestRequestBuilder.builder()
                    .registrationNumber("ZBM0101")
                    .email("test@example.com")
                    .build();

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
        @DisplayName("should return the same neutral response when the service rejects the request")
        void shouldReturnNeutralResponseWhenServiceRejectsRequest() throws Exception {
            TokenRequestRequest request = TokenRequestRequestBuilder.builder()
                    .registrationNumber("ZBM0101")
                    .email("test@example.com")
                    .build();
            doThrow(new TokenValidationException("Account is not in pending activation status"))
                    .when(passwordSetupServiceMock).requestNewToken("ZBM0101", "test@example.com");

            mockMvc.perform(post("/api/auth/password-setup/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("If your account is pending activation, you will receive an email with a new setup link."));
        }

        @Test
        @DisplayName("should reject request with blank registration number")
        void shouldRejectRequestWithBlankRegistrationNumber() throws Exception {
            // Given
            TokenRequestRequest request = TokenRequestRequestBuilder.builder()
                    .registrationNumber("")
                    .email("test@example.com")
                    .build();

            // When/Then
            mockMvc.perform(post("/api/auth/password-setup/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("problem+json negotiation as sent by the SPA")
    class ProblemJsonNegotiationTests {

        private static final String SPA_ACCEPT = "application/json, application/problem+json";

        @Test
        @DisplayName("validate with an expired token answers problem+json")
        void expiredTokenOnValidate() throws Exception {
            String token = UUID.randomUUID().toString();
            when(passwordSetupServiceMock.validateToken(token))
                    .thenThrow(new TokenExpiredException("Token has expired"));

            mockMvc.perform(get("/api/auth/password-setup/validate")
                            .header("Accept", SPA_ACCEPT)
                            .param("token", token))
                    .andExpect(status().isGone())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.type").value("/errors/token-expired"));
        }

        @Test
        @DisplayName("complete with a used token answers problem+json")
        void usedTokenOnComplete() throws Exception {
            SetPasswordRequest request = SetPasswordRequestBuilder.builder()
                    .token(UUID.randomUUID().toString())
                    .password("SecurePassword123!")
                    .passwordConfirmation("SecurePassword123!")
                    .build();
            when(passwordSetupServiceMock.completePasswordSetup(any(), anyString()))
                    .thenThrow(new TokenAlreadyUsedException("Token has already been used"));

            mockMvc.perform(post("/api/auth/password-setup/complete")
                            .header("Accept", SPA_ACCEPT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isGone())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.type").value("/errors/token-already-used"));
        }

        @Test
        @DisplayName("complete with mismatching passwords answers problem+json bound to the confirmation field")
        void mismatchOnComplete() throws Exception {
            SetPasswordRequest request = SetPasswordRequestBuilder.builder()
                    .token(UUID.randomUUID().toString())
                    .password("Password123!")
                    .passwordConfirmation("DifferentPassword123!")
                    .build();

            mockMvc.perform(post("/api/auth/password-setup/complete")
                            .header("Accept", SPA_ACCEPT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.type").value("/errors/passwords-do-not-match"))
                    .andExpect(jsonPath("$.field").value("passwordConfirmation"));
        }
    }
}
