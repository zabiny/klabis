package club.klabis;

import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

public class Modulith {

    ApplicationModules modules = ApplicationModules.of(KlabisApplication.class,
            JavaClass.Predicates.resideInAPackage("club.klabis.shared.."));

    // Generates documentation of modules into build/spring-modulith-docs
    @Test
    void writeDocumentationSnippets() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
