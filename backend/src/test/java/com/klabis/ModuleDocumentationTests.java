package com.klabis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Spring Modulith module documentation generation test.
 *
 * <p><b>Purpose:</b> FRAMEWORK UTILITY - Generates Spring Modulith architecture documentation
 * including C4 component diagrams, module canvases, and API documentation.
 *
 * <p><b>Business Value:</b> Keeps architecture documentation up-to-date automatically. Every time
 * this test runs, it regenerates documentation to reflect the current module structure, ensuring
 * docs never become stale.
 *
 * <p><b>What It Generates:</b>
 * <ul>
 *   <li><b>C4 Component Diagrams</b> - PlantUML diagrams showing module structure and dependencies</li>
 *   <li><b>Per-Module Diagrams</b> - Individual PlantUML diagrams for each module</li>
 *   <li><b>Module Canvases</b> - Ad-hoc documentation canvases for each module</li>
 *   <li><b>Aggregating Document</b> - Single master document combining all modules</li>
 * </ul>
 *
 * <p><b>Output Location:</b> {@code target/spring-modulith-docs/}
 *
 * <p><b>CI/CD Integration:</b>
 * This test should run on every build to keep documentation up-to-date. Generated docs can be
 * committed to {@code docs/architecture/spring-modulith/} for version control and inclusion in
 * project wikis or documentation sites.
 *
 * <p><b>Module Structure:</b>
 * <ul>
 *   <li><b>members</b> - Member domain (aggregate root, registration, personal information)</li>
 *   <li><b>users</b> - User management (authentication, authorization, password setup)</li>
 *   <li><b>config</b> - Shared configuration (NOT a module, shared infrastructure)</li>
 *   <li><b>common</b> - Shared utilities (NOT a module, framework code)</li>
 * </ul>
 *
 * <p><b>Documented APIs:</b>
 * <ul>
 *   <li><b>members</b> - {@code com.klabis.members.domain.events} (MemberCreatedEvent, etc.)</li>
 *   <li><b>users</b> - No public events yet (internal module)</li>
 * </ul>
 *
 * <p><b>Documented Dependencies:</b>
 * <ul>
 *   <li><b>members → users</b> - members depends on users for User entity</li>
 *   <li><b>users → (none)</b> - users has no dependencies on other modules</li>
 * </ul>
 *
 * <p><b>References:</b>
 * <ul>
 *   <li><a href="https://docs.spring.io/spring-modulith/reference/#_documenting_modules">Spring Modulith Documentation</a></li>
 *   <li><a href="https://docs.spring.io/spring-modulith/reference/#_documenting_modules_module_canvas">Module Canvas</a></li>
 *   <li><a href="https://c4model.com/">C4 Model for Architecture Diagrams</a></li>
 * </ul>
 *
 * @see ApplicationModules
 * @see Documenter
 * @see KlabisApplication
 */
@DisplayName("Framework: Spring Modulith Module Documentation Generation")
class ModuleDocumentationTests {

    /**
     * Generates Spring Modulith module documentation.
     *
     * <p>This test creates comprehensive documentation including:
     * <ul>
     *   <li>C4 component diagrams (PlantUML format)</li>
     *   <li>Individual module diagrams (PlantUML format)</li>
     *   <li>Module canvases (Markdown format)</li>
     *   <li>Aggregating document (single master document)</li>
     * </ul>
     *
     * <p><b>Output Files:</b>
     * <ul>
     *   <li>{@code target/spring-modulith-docs/modules/} - Per-module diagrams and canvases</li>
     *   <li>{@code target/spring-modulith-docs/} - Aggregated diagrams and documentation</li>
     * </ul>
     *
     * <p><b>Usage in CI/CD:</b>
     * <pre>
     * # Run this test to generate documentation
     * mvn test -Dtest=ModuleDocumentationTests
     *
     * # Copy generated docs to docs folder
     * cp -r target/spring-modulith-docs/* docs/architecture/spring-modulith/
     *
     * # Commit updated documentation
     * git add docs/architecture/spring-modulith/
     * git commit -m "Update Spring Modulith documentation"
     * </pre>
     *
     * <p><b>Integration with Documentation Sites:</b>
     * The generated PlantUML diagrams can be rendered using:
     * <ul>
     *   <li>PlantUML Online Server (https://plantuml.com/plantuml/uml/)</li>
     *   <li>VS Code PlantUML extension</li>
     *   <li>Confluence PlantUML plugin</li>
     *   <li>GitHub markdown (with mermaid or plantuml plugin)</li>
     * </ul>
     *
     * <p><b>Current Status (Iteration 12.3):</b>
     * <ul>
     *   <li><span style="color:green">✓ DOCUMENTATION GENERATION WORKING</span> - All diagrams generate successfully</li>
     *   <li><span style="color:green">✓ MODULE STRUCTURE DOCUMENTED</span> - members and users modules included</li>
     *   <li><span style="color:green">✓ DEPENDENCIES DOCUMENTED</span> - members → users dependency shown</li>
     *   <li><span style="color:green">✓ APIS DOCUMENTED</span> - Public events exposed by modules</li>
     * </ul>
     */
    @Test
    @DisplayName("generates Spring Modulith module documentation")
    void generateModuleDocumentation() {
        // Given: ApplicationModules instance
        ApplicationModules modules = ApplicationModules.of(KlabisApplication.class);

        // When: Generate comprehensive documentation
        assertThatCode(() -> {
            Documenter documenter = new Documenter(modules);

            // Generate C4 component diagrams (PlantUML)
            documenter.writeModulesAsPlantUml()
                    .writeIndividualModulesAsPlantUml();

            // Generate module canvases (Markdown)
            documenter.writeModuleCanvases();

            // Generate aggregating document (master document)
            documenter.writeAggregatingDocument();

        }).doesNotThrowAnyException();

        // Then: Documentation files created in target/spring-modulith-docs/
        // Note: This test always passes if no exception is thrown
        // The actual documentation files are the output of interest
    }
}
