package com.klabis;

import com.klabis.config.TestSslConfiguration;
import com.klabis.authorizationserver.KlabisUserDetailsService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
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

    /**
     * Make async event handlers run synchronously during tests to avoid shutdown races
     * where background tasks try to access Hikari after the pool was closed.
     *
     * This is test-only and safe to add here because this class is a @TestConfiguration.
     */
    @Bean(name = "taskExecutor")
    public TaskExecutor taskExecutor() {
        return new SyncTaskExecutor();
    }
}
