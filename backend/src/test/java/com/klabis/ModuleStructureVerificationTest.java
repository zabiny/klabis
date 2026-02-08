package com.klabis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Spring Modulith module structure verification test.
 *
 * <p><b>Purpose:</b> FRAMEWORK TEST - Verifies Spring Modulith module detection works correctly
 * and enforces architectural modularity at build time.
 *
 * <p><b>Business Value:</b> Ensures architectural modularity is enforced at build time,
 * preventing violations of module boundaries and maintaining clean architecture.
 *
 * <p><b>What It Tests:</b>
 * <ul>
 *   <li>Spring Modulith detects modules correctly from package structure</li>
 *   <li>No circular dependencies between modules exist</li>
 *   <li>No architectural violations across module boundaries</li>
 *   <li>Module structure follows Spring Modulith conventions</li>
 * </ul>
 *
 * <p><b>Module Structure:</b>
 * <ul>
 *   <li><b>members</b> - Member domain (aggregate root, registration, personal information)</li>
 *   <li><b>users</b> - User management (authentication, authorization, password setup)</li>
 *   <li><b>events</b> - Event management and registration (club events, member registration)</li>
 *   <li><b>common</b> - Shared utilities and framework code</li>
 *   <li><b>config</b> - Shared configuration and infrastructure</li>
 * </ul>
 *
 * <p><b>References:</b>
 * <ul>
 *   <li><a href="https://docs.spring.io/spring-modulith/reference/#_architecture_verification">Spring Modulith Architecture Verification</a></li>
 *   <li><a href="https://docs.spring.io/spring-modulith/reference/#_module_dependencies">Module Dependencies</a></li>
 * </ul>
 *
 * @see ApplicationModules
 * @see KlabisApplication
 */
@DisplayName("Framework: Spring Modulith Module Structure Verification")
class ModuleStructureVerificationTest {

    /**
     * Verifies module structure for architectural violations.
     *
     * <p>This test instantiates {@link ApplicationModules} using Spring Modulith's
     * module detection and calls {@link ApplicationModules#verify()} to check for:
     * <ul>
     *   <li>Circular dependencies between modules</li>
     *   <li>Direct method calls across module boundaries (should use events)</li>
     *   <li>Package structure violations</li>
     *   <li>Dependency violations</li>
     * </ul>
     *
     * <p><b>Current Status (Iteration 12):</b>
     * <ul>
     *   <li><span style="color:green">✓ CIRCULAR DEPENDENCY RESOLVED</span> - The members → users → members cycle has been broken</li>
     *   <li><span style="color:orange">⚠ NON-EXPOSED TYPES</span> - Some modules use types from other modules that aren't in public `api` packages</li>
     * </ul>
     *
     * <p><b>Known Violations (Technical Debt):</b>
     * <ul>
     *   <li>members depends on users.domain (User, UserRepository, PasswordSetupToken, etc.)</li>
     *   <li>members depends on users.application (PerKeyRateLimiter)</li>
     *   <li>members depends on common.email (EmailService, EmailTemplate, ThymeleafTemplateRenderer)</li>
     *   <li>users depends on common.audit (Auditable, AuditEventType)</li>
     *   <li>events depends on users.domain (UserId - shared kernel value object)</li>
     *   <li>events depends on members.application (Members - public query API for member data)</li>
     * </ul>
     *
     * <p><b>Future Work:</b> To fully resolve these violations, either:
     * <ol>
     *   <li>Move shared types to `api` packages within each module, or</li>
     *   <li>Further refactor to use event-driven communication where possible</li>
     * </ol>
     *
     * <p>If violations are detected, the test will fail with a detailed error message
     * describing the architectural problems.
     *
     * <p><b>Note:</b> With the {@code detection-strategy: explicitly-annotated} configuration,
     * only packages annotated with {@code @ApplicationModule} are detected as modules.
     * The {@code common} package is NOT a module, so its types can be used freely across
     * all modules without "non-exposed type" violations.
     *
     * <p><b>Resolution:</b>
     * <ul>
     *   <li>Added {@code detection-strategy: explicitly-annotated} to application.yml</li>
     *   <li>Created package-info.java with {@code @ApplicationModule} for config, members, users</li>
     *   <li>Left common as a regular package (NOT a module) - no violations for its types</li>
     *   <li>Circular dependency already resolved in earlier refactoring</li>
     * </ul>
     */
    @Test
    @DisplayName("verifies module structure for architectural violations")
    void verifiesModuleStructure() {
        // When: Create ApplicationModules and verify structure
        ApplicationModules modules = ApplicationModules.of(KlabisApplication.class);

        // Then: No architectural violations should exist
        assertThatCode(modules::verify)
                .doesNotThrowAnyException();
    }

    /**
     * Prints detected module structure for debugging purposes.
     *
     * <p>This test outputs the detected module structure to help developers understand
     * how Spring Modulith has organized the application into modules.
     *
     * <p>Output includes:
     * <ul>
     *   <li>Detected module names</li>
     *   <li>Module dependencies</li>
     *   <li>Package structure</li>
     *   <li>Public module API (types exposed to other modules)</li>
     * </ul>
     *
     * <p>This test always passes and is purely informational.
     */
    @Test
    @DisplayName("prints detected module structure for debugging")
    void printsModuleStructure() {
        // Given: ApplicationModules instance
        ApplicationModules modules = ApplicationModules.of(KlabisApplication.class);

        // When: Print module structure to console
        System.out.println("=".repeat(80));
        System.out.println("SPRING MODULITH MODULE STRUCTURE");
        System.out.println("=".repeat(80));
        modules.forEach(System.out::println);
        System.out.println("=".repeat(80));

        // Then: Output should show expected modules (members, users, common, config)
        // This test always passes - it's purely informational
    }
}
