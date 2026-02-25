package com.klabis;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Service;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit tests verifying security architecture constraints.
 *
 * <p>These tests ensure that Spring Security infrastructure does not leak
 * into domain and application layers, maintaining hexagonal architecture principles.
 *
 * <p>Key rules enforced:
 * <ul>
 *   <li>Domain and application layers must not depend on Spring Security context infrastructure</li>
 *   <li>Infrastructure layer (controllers) handles security concerns and passes user context explicitly</li>
 * </ul>
 */
@DisplayName("Security Architecture Tests")
class SecurityArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.klabis");
    }

    @Test
    @DisplayName("Domain layer should not depend on Spring Security context classes")
    void domainLayerShouldNotDependOnSpringSecurityContext() {
        noClasses()
                .that().resideInAPackage("..domain..").or().areAnnotatedWith(AggregateRoot.class)
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework.security.core.context")
                .because("Domain layer must remain independent of Spring Security infrastructure")
                .check(classes);
    }

    @Test
    @DisplayName("Application layer should not depend on Spring Security context classes")
    void applicationLayerShouldNotDependOnSpringSecurityContext() {
        noClasses()
                .that().resideInAPackage("..application..").or().areAnnotatedWith(Service.class)
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework.security.core.context")
                .because("Application layer should receive user context explicitly from infrastructure")
                .check(classes);
    }

    @Test
    @DisplayName("Domain layer should not depend on Spring Security Authentication classes")
    void domainLayerShouldNotDependOnSpringSecurityAuthentication() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework.security.core..")
                .orShould().dependOnClassesThat()
                .resideInAPackage("org.springframework.security.web..")
                .because("Domain layer must remain independent of Spring Security")
                .check(classes);
    }

    @Test
    @DisplayName("Application layer should not depend on Spring Security Authentication classes")
    void applicationLayerShouldNotDependOnSpringSecurityAuthentication() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework.security.core..")
                .orShould().dependOnClassesThat()
                .resideInAPackage("org.springframework.security.web..")
                .because("Application layer should receive user context explicitly from infrastructure")
                .check(classes);
    }
}
