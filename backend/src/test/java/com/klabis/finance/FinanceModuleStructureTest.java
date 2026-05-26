package com.klabis.finance;

import com.klabis.KlabisApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("Finance module structure verification")
class FinanceModuleStructureTest {

    private static final ApplicationModules modules = ApplicationModules.of(KlabisApplication.class);

    @Test
    @DisplayName("finance module is detected")
    void financeModuleIsDetected() {
        assertThat(modules.getModuleByName("finance")).isPresent();
    }

    @Test
    @DisplayName("finance module has application as a named interface")
    void financeModuleHasApplicationNamedInterface() {
        ApplicationModule financeModule = modules.getModuleByName("finance").orElseThrow();

        assertThat(financeModule.getNamedInterfaces().stream()
                .anyMatch(ni -> ni.getName().equals("application")))
                .as("finance module should have 'application' as a named interface")
                .isTrue();
    }

    @Test
    @DisplayName("module structure has no circular dependencies")
    void noCircularDependencies() {
        assertThatCode(modules::verify).doesNotThrowAnyException();
    }
}
