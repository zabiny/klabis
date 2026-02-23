package com.klabis.common.users.passwordsetup;

import com.klabis.common.email.EmailService;
import com.klabis.common.ratelimit.PerKeyRateLimiter;
import com.klabis.common.templating.ThymeleafTemplateRenderer;
import com.klabis.common.users.persistence.PasswordSetupTokenRepository;
import com.klabis.common.users.persistence.UserRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Base test class for {@link PasswordSetupService} tests.
 * <p>
 * This class eliminates duplicate mock setup code across PasswordSetupService test classes
 * by providing common mock fields and a factory method for creating service instances.
 * </p>
 * <p>
 * <b>Usage:</b>
 * </p>
 * <pre>
 * &#64;ExtendWith(MockitoExtension.class)
 * class PasswordSetupServiceTest extends PasswordSetupServiceTestBase {
 *
 *     private PasswordSetupService passwordSetupService;
 *
 *     &#64;BeforeEach
 *     void setUp() {
 *         passwordSetupService = createService();
 *         // Additional test-specific setup
 *     }
 *
 *     // Test methods...
 * }
 * </pre>
 * <p>
 * <b>Benefits:</b>
 * </p>
 * <ul>
 *   <li>Single source of truth for mock configuration</li>
 *   <li>Reduces duplicate code by ~20 lines per test class</li>
 *   <li>Ensures consistent setup across all PasswordSetupService tests</li>
 *   <li>Simplifies adding new dependencies to PasswordSetupService</li>
 * </ul>
 *
 * @see PasswordSetupService
 * @see TestConfigurationHelper
 */
@ExtendWith(MockitoExtension.class)
public abstract class PasswordSetupServiceTestBase {

    /**
     * Mock repository for password setup tokens.
     */
    @Mock
    protected PasswordSetupTokenRepository tokenRepository;

    /**
     * Mock repository for user domain operations.
     */
    @Mock
    protected UserRepository userRepository;

    /**
     * Mock email service for sending notifications.
     */
    @Mock
    protected EmailService emailService;

    /**
     * Mock template renderer for email templates.
     */
    @Mock
    protected ThymeleafTemplateRenderer templateRenderer;

    /**
     * Mock password encoder for hashing passwords.
     */
    @Mock
    protected PasswordEncoder passwordEncoder;

    /**
     * Mock password complexity validator.
     */
    @Mock
    protected PasswordComplexityValidator passwordValidator;

    /**
     * Mock rate limiter for password setup requests.
     */
    @Mock
    protected PerKeyRateLimiter rateLimiter;

    /**
     * Creates a new {@link PasswordSetupService} instance with all mock dependencies.
     * <p>
     * The returned service is configured with default test values via
     * {@link TestConfigurationHelper#configurePasswordSetupService(Object)}.
     * </p>
     * <p>
     * This method should be called from {@code @BeforeEach} setup methods in test classes:
     * </p>
     * <pre>
     * &#64;BeforeEach
     * void setUp() {
     *     passwordSetupService = createService();
     * }
     * </pre>
     *
     * @return a new PasswordSetupService instance configured with mocks
     */
    protected PasswordSetupService createService() {
        PasswordSetupService service = new PasswordSetupService(
                tokenRepository,
                userRepository,
                emailService,
                templateRenderer,
                passwordEncoder,
                passwordValidator,
                rateLimiter
        );
        TestConfigurationHelper.configurePasswordSetupService(service);
        return service;
    }
}
