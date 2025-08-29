package club.klabis;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.jmolecules.archunit.JMoleculesArchitectureRules;
import org.jmolecules.archunit.JMoleculesDddRules;

@AnalyzeClasses(packages = "club.klabis") // (1)
public class ArchitectureTests {
    @ArchTest
    ArchRule dddRules = JMoleculesDddRules.all();
    @ArchTest
    ArchRule simplifiedOnionArchitecture = JMoleculesArchitectureRules.ensureOnionSimple();

    // TODO: switch to hexagonal - https://ersantana.com/software-architecture/jmolecules/jmolecules-hexagonal-architecture-guide
    @ArchTest
    ArchRule hexagonalArchitecture = JMoleculesArchitectureRules.ensureHexagonal();

}

