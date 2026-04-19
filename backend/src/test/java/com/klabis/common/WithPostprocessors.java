package com.klabis.common;

import com.klabis.events.application.OrisEventImportPort;
import com.klabis.groups.familygroup.domain.FamilyGroupRepository;
import com.klabis.groups.traininggroup.domain.TrainingGroupRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides mocks for dependencies of every {@link com.klabis.common.mvc.MvcComponent}-annotated
 * {@link org.springframework.hateoas.server.RepresentationModelProcessor} so that {@code @WebMvcTest}
 * slices can load the MVC scan without {@code UnsatisfiedDependencyException}.
 * <p>
 * Add new entries here whenever a postprocessor gains a new dependency type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@MockitoBean(types = {
        FamilyGroupRepository.class,
        TrainingGroupRepository.class,
        OrisEventImportPort.class
})
public @interface WithPostprocessors {
}
