package club.klabis;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

public class Modulith {

    ApplicationModules modules = ApplicationModules.of(KlabisApplication.class);

    // Generates documentation of modules into build/spring-modulith-docs
    @Test
    void writeDocumentationSnippets() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
