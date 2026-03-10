package com.klabis;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
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
    @DisplayName("Application layer should not depend on infrastructure layer")
    void applicationLayerShouldNotDependOnInfrastructureLayer() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .because("Application layer must not depend on infrastructure layer — dependency direction is infrastructure → application")
                .check(classes);
    }

    @Test
    @DisplayName("Domain layer should not depend on Jackson/JSON classes")
    void domainLayerShouldNotDependOnJackson() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.fasterxml.jackson..", "org.json..", "jakarta.json..")
                .because("Domain layer must not depend on serialization frameworks — domain objects are pure business logic")
                .check(classes);
    }

    @Test
    @DisplayName("Application layer should not depend on Jackson/JSON classes")
    void applicationLayerShouldNotDependOnJackson() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.fasterxml.jackson..", "org.json..", "jakarta.json..")
                .because("Application layer must not depend on serialization frameworks — serialization belongs to infrastructure layer")
                .check(classes);
    }
}
