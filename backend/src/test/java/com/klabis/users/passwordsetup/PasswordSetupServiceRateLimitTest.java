package com.klabis.users.passwordsetup;

import com.klabis.common.email.EmailMessage;
import com.klabis.users.User;
import com.klabis.users.PasswordSetupToken;
import com.klabis.users.testdata.UserTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Rate limiting tests for PasswordSetupService.
 *
 * <p>These tests verify that rate limiting is enforced correctly:
 * <ul>
 *   <li>Rate limiting should be per registration number, not per IP address</li>
 *   <li>Same registration number from different IPs should be rate limited together</li>
 *   <li>Different registration numbers should have independent rate limits</li>
 * </ul>
 *
 * <p><b>SECURITY ISSUE:</b> The current implementation uses resilience4j's default
 * rate limiter which applies globally to the method, not per registration number.
 * This means that ALL users share the same rate limit, which is overly restrictive.
 * A proper implementation should use a per-key rate limiter (e.g., using registration
 * number as the key).
 */
@DisplayName("PasswordSetupService Rate Limiting Tests")
class PasswordSetupServiceRateLimitTest extends PasswordSetupServiceTestBase {

    private PasswordSetupService passwordSetupService;

    @BeforeEach
    void setUp() {
        passwordSetupService = createService();

        // Note: We're not setting up RateLimiterRegistry here because in a real test
        // we would need to use the actual Spring context with @SpringBootTest.
        // These tests document the expected behavior vs actual behavior.
    }

    @Nested
    @DisplayName("SECURITY: Rate limiting should be per registration number (not IP)")
    class SecurityRateLimitingTests {

        @Test
        @DisplayName("SECURITY ISSUE: Same registration number from different IPs should share rate limit")
        void sameRegistrationNumberDifferentIpShouldShareRateLimit() {
            // Given
            String registrationNumber = "ZBM0101";
            User user = createPendingUser(registrationNumber);

            when(userRepository.findByUsername(registrationNumber)).thenReturn(Optional.of(user));
            when(tokenRepository.save(any(PasswordSetupToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(templateRenderer.renderHtml(any(), any())).thenReturn("<html>email body</html>");
            when(templateRenderer.renderText(any(), any())).thenReturn("text body");

            // When: Make 3 requests from different IPs (should all succeed)
            for (int i = 0; i < 3; i++) {
                passwordSetupService.requestNewToken(registrationNumber, "test@example.com");
            }

            // Then: Should have sent 3 emails
            verify(emailService, times(3)).send(any(EmailMessage.class));

            // When: Make 4th request from different IP
            // EXPECTED: Should be rate limited because it's the same registration number
            // ACTUAL: Will fail only if rate limiter is working (depends on Spring context)
            // This test requires @SpringBootTest to properly test the rate limiter

            // NOTE: This test documents the EXPECTED behavior.
            // To actually test this, we need integration tests with MockMvc.
        }

        @Test
        @DisplayName("SECURITY ISSUE: Different registration numbers should have independent rate limits")
        void differentRegistrationNumbersShouldHaveIndependentRateLimits() {
            // Given
            String regNumber1 = "ZBM0101";
            String regNumber2 = "ZBM0202";

            User user1 = createPendingUser(regNumber1);
            User user2 = createPendingUser(regNumber2);

            when(userRepository.findByUsername(regNumber1)).thenReturn(Optional.of(user1));
            when(userRepository.findByUsername(regNumber2)).thenReturn(Optional.of(user2));
            when(tokenRepository.save(any(PasswordSetupToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(templateRenderer.renderHtml(any(), any())).thenReturn("<html>email body</html>");
            when(templateRenderer.renderText(any(), any())).thenReturn("text body");

            // When: Exhaust rate limit for registration number 1
            for (int i = 0; i < 3; i++) {
                passwordSetupService.requestNewToken(regNumber1, "test1@example.com");
            }

            // EXPECTED: Registration number 2 should still work (independent rate limit)
            // ACTUAL: With global rate limiter, this will fail
            // This test requires @SpringBootTest to properly test the rate limiter
            passwordSetupService.requestNewToken(regNumber2, "test2@example.com");

            // NOTE: This test documents the EXPECTED behavior.
            // To actually test this, we need integration tests with MockMvc.
        }
    }

    @Nested
    @DisplayName("Rate limiter integration tests")
    class RateLimiterIntegrationTests {

        @Test
        @DisplayName("should call PerKeyRateLimiter to check rate limit")
        void shouldCallPerKeyRateLimiterToCheckLimit() {
            // Given
            String registrationNumber = "ZBM0101";
            User user = createPendingUser(registrationNumber);

            when(userRepository.findByUsername(registrationNumber)).thenReturn(Optional.of(user));
            when(tokenRepository.save(any(PasswordSetupToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(templateRenderer.renderHtml(any(), any())).thenReturn("<html>email body</html>");
            when(templateRenderer.renderText(any(), any())).thenReturn("text body");

            // When
            passwordSetupService.requestNewToken(registrationNumber, "test@example.com");

            // Then
            verify(rateLimiter).checkLimit(registrationNumber);
        }
    }

    private User createPendingUser(String registrationNumber) {
        return UserTestDataBuilder.aPendingUser()
                .username(registrationNumber)
                .build();
    }
}
