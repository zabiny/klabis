package com.klabis;

import com.klabis.config.TestSslConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

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
@Import({TestSslConfiguration.class})
@ComponentScan("com.klabis.config")
public class TestApplicationConfiguration {
}
