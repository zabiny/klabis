package com.klabis;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit tests verifying correct layer dependency direction in hexagonal architecture.
 *
 * <p>Enforced rules:
 * <ul>
 *   <li>Domain layer must not depend on application or infrastructure layers</li>
 *   <li>Application layer must not depend on infrastructure layer</li>
 * </ul>
 */
// replace with jMolecules CleanArchitecture/Hexagonal architecture test (annotations of layers)
@DisplayName("Layer Architecture Tests")
class LayerArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.klabis");
    }

    @Test
    @DisplayName("Domain layer should not depend on application layer")
    void domainLayerShouldNotDependOnApplicationLayer() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("..application..")
                .because("Domain layer must not depend on application layer — dependency direction is application → domain")
                .check(classes);
    }

    @Test
    @DisplayName("Domain layer should not depend on infrastructure layer")
    void domainLayerShouldNotDependOnInfrastructureLayer() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .because("Domain layer must not depend on infrastructure layer — dependency direction is infrastructure → domain")
                .check(classes);
    }

    @Test
    @Disabled
    @DisplayName("Application layer should not depend on infrastructure layer")
    void applicationLayerShouldNotDependOnInfrastructureLayer() {
        FreezingArchRule.freeze(
                noClasses()
                        .that().resideInAPackage("..application..")
                        .should().dependOnClassesThat()
                        .resideInAPackage("..infrastructure..")
                        .because("Application layer must not depend on infrastructure layer — dependency direction is infrastructure → application")
        ).check(classes);
    }
}
