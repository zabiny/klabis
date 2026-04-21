package com.klabis.common;

import com.klabis.common.users.UserService;
import com.klabis.groups.familygroup.domain.FamilyGroupRepository;
import com.klabis.groups.traininggroup.domain.TrainingGroupRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
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
 * <p>
 * Security-related infra beans mocked here:
 * <ul>
 *   <li>{@link UserService} — required by {@code AccountStatusValidationFilter}, which checks
 *       account status from the database on every request in the resource-server filter chain.</li>
 *   <li>{@link UserDetailsService} — required by Authorization Server components introduced
 *       via component scan (e.g. {@code KlabisUserDetailsService}).</li>
 * </ul>
 * <p>
 * Beans treated as feature flags via {@code Optional<T>} injection (e.g. {@code OrisEventImportPort})
 * must NOT be added here — the mere presence of the mock would activate the feature in every test.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@MockitoBean(types = {
        FamilyGroupRepository.class,
        TrainingGroupRepository.class,
        UserService.class,
        UserDetailsService.class
})
public @interface WithPostprocessors {
}
