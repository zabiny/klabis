package com.klabis.common.users.passwordsetup;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test utility class to help configure services using reflection in tests.
 * <p>
 * This helper eliminates duplicate reflection-based setup code across test classes.
 * It uses Spring's {@link ReflectionTestUtils} to set private fields that are
 * normally configured via {@code @Value} annotations in production.
 * </p>
 */
public class TestConfigurationHelper {

    /**
     * Default token expiration time in hours (matches production default).
     */
    public static final int DEFAULT_TOKEN_EXPIRATION_HOURS = 4;

    /**
     * Default base URL for the application (matches production default).
     */
    public static final String DEFAULT_BASE_URL = "https://localhost:8443";

    /**
     * Default club name for the application (matches production default).
     */
    public static final String DEFAULT_CLUB_NAME = "Klabis";

    private TestConfigurationHelper() {
        // Utility class - prevent instantiation
    }

    /**
     * Configures a PasswordSetupService instance with default test values.
     * <p>
     * Sets the following fields:
     * <ul>
     *   <li>tokenExpirationHours = 4</li>
     *   <li>baseUrl = "https://localhost:8443"</li>
     *   <li>clubName = "Klabis"</li>
     * </ul>
     * </p>
     *
     * @param service the PasswordSetupService instance to configure
     * @throws IllegalArgumentException if service is null
     */
    public static void configurePasswordSetupService(Object service) {
        configurePasswordSetupService(
                service,
                DEFAULT_TOKEN_EXPIRATION_HOURS,
                DEFAULT_BASE_URL,
                DEFAULT_CLUB_NAME
        );
    }

    /**
     * Configures a PasswordSetupService instance with custom test values.
     * <p>
     * This method allows tests to override the default configuration values
     * for testing specific scenarios (e.g., different token expiration times).
     * </p>
     *
     * @param service         the PasswordSetupService instance to configure
     * @param expirationHours token expiration time in hours
     * @param baseUrl         base URL for the application
     * @param clubName        name of the club
     * @throws IllegalArgumentException if service is null
     */
    public static void configurePasswordSetupService(
            Object service,
            int expirationHours,
            String baseUrl,
            String clubName
    ) {
        if (service == null) {
            throw new IllegalArgumentException("Service cannot be null");
        }

        ReflectionTestUtils.setField(service, "tokenExpirationHours", expirationHours);
        ReflectionTestUtils.setField(service, "baseUrl", baseUrl);
        ReflectionTestUtils.setField(service, "clubName", clubName);
    }
}
