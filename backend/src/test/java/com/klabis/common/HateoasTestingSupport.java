package com.klabis.common;

import com.klabis.KlabisApplication;
import com.tngtech.archunit.core.domain.JavaClass;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.core.ControllerEntityLinks;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilderFactory;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.stereotype.Controller;

import java.util.List;

public final class HateoasTestingSupport {
    private HateoasTestingSupport() {
    }

    /**
     * Returns EntityLinks instance containing links created from all controllers in same module as given testedController class
     */
    public static EntityLinks createModuleEntityLinks(Class<?> testedController) {
        ApplicationModule testedControllerModule = ApplicationModules.of(KlabisApplication.class)
                .stream()
                .filter(p -> !p.getSpringBeans(testedController).isEmpty())
                .findFirst().orElseThrow();

        List<? extends Class<?>> moduleControllers = testedControllerModule.getBasePackage()
                .stream()
                .filter(c -> c.isMetaAnnotatedWith(Controller.class))
                .filter(c -> c.isMetaAnnotatedWith(ExposesResourceFor.class))
                .map(JavaClass::reflect).toList();;

        return new ControllerEntityLinks(moduleControllers, new WebMvcLinkBuilderFactory());
    }

}
