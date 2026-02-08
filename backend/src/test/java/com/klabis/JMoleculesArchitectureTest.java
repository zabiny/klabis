package com.klabis;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.jmolecules.archunit.JMoleculesArchitectureRules;
import org.jmolecules.archunit.JMoleculesDddRules;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * ArchUnit tests verifying jMolecules architecture rules.
 *
 * <p>These tests use standard jMolecules ArchUnit integration to verify:
 * <ul>
 *   <li>DDD building blocks are properly used (aggregates, entities, value objects, repositories)</li>
 *   <li>Hexagonal architecture boundaries are respected (ports and adapters)</li>
 * </ul>
 *
 * <p>See design.md for rationale behind these architectural rules.
 */
class JMoleculesArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        // Import all classes from the com.klabis package, excluding test classes
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.klabis");
    }

    /**
     * Verifies that DDD building blocks are used correctly.
     */
    @Test
    void dddBuildingBlocksShouldBeValid() {
        JMoleculesDddRules.all().check(classes);
    }

    /**
     * Verifies hexagonal architecture constraints.
     * <p>
     * Checks from JMoleculesArchitectureRules:
     * - Primary adapters may only access primary ports
     * - Secondary adapters may only access secondary ports
     * - Primary adapters must not depend on secondary adapters
     * - Ports define the application core interface
     */
    @Test
    void hexagonalArchitectureShouldBeRespected() {
        JMoleculesArchitectureRules.ensureHexagonal().check(classes);
    }
}
