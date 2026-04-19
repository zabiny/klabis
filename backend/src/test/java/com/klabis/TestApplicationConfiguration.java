package com.klabis;

import com.klabis.config.TestSslConfiguration;
import com.klabis.members.infrastructure.authorizationserver.KlabisUserDetailsService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test application configuration.
 * <p>
 * This configuration is automatically discovered and applied when running tests.
 * It imports the test domain JDBC configuration to enable test repositories
 * for Spring Modulith event processing tests, and SSL configuration for HTTPS
 * testing with self-signed certificates.
 * </p>
 *
 * <p><b>Profiles:</b>
 * <ul>
 *   <li>test - SSL configuration is applied (trusts self-signed certificates)</li>
 *   <li>other profiles - SSL configuration is not applied</li>
 * </ul>
 * </p>
 */
@TestConfiguration
@Import({TestSslConfiguration.class, KlabisUserDetailsService.class})
@ActiveProfiles("test")
@CleanupTestData    // tests are sharing single H2 - need to find out why so we can remove this cleanup (it deletes also bootstrap data what can cause issues somewhere)
public class TestApplicationConfiguration {
}
