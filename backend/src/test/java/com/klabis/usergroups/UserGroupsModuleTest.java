package com.klabis.usergroups;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import com.klabis.KlabisApplication;

import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("UserGroups Spring Modulith module structure test")
class UserGroupsModuleTest {

    @Test
    @DisplayName("should have valid module structure with no architectural violations")
    void shouldHaveValidModuleStructure() {
        ApplicationModules modules = ApplicationModules.of(KlabisApplication.class);

        assertThatCode(modules::verify)
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should detect usergroups as a module")
    void shouldDetectUserGroupsAsModule() {
        ApplicationModules modules = ApplicationModules.of(KlabisApplication.class);

        assertThatCode(() -> modules.getModuleByName("usergroups").orElseThrow(
                () -> new AssertionError("Module 'usergroups' not detected by Spring Modulith")
        )).doesNotThrowAnyException();
    }
}
