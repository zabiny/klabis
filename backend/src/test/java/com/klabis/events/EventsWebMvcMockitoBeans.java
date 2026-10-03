package com.klabis.events;

import com.klabis.events.application.CategoryPresetManagementPort;
import com.klabis.events.application.DisciplineManagementPort;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.EventRegistrationPort;
import com.klabis.events.application.EventTypeManagementPort;
import com.klabis.events.application.MemberRegistrationSanctionPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mocks the ports required by the web beans of the events module (event, registration, event type, discipline
 * and category preset controllers, postprocessors) so that other modules' {@code @WebMvcTest} slices
 * can load them without the events application layer.
 * <p>
 * {@code OrisEventImportPort} is deliberately absent: it is injected as {@code Optional} and acts as a
 * feature flag. ORIS-gated controllers are not part of the test profile.
 * <p>
 * Also requires {@code Members} (see members annotation), {@code FinanceAccountLinkSupport} (real bean,
 * loaded by adding {@code finance} to {@code extraIncludes}) and {@code SynchronizationPort} (see sync annotation).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@MockitoBean(types = {
        EventManagementPort.class,
        EventRegistrationPort.class,
        EventTypeManagementPort.class,
        DisciplineManagementPort.class,
        CategoryPresetManagementPort.class,
        MemberRegistrationSanctionPort.class
})
public @interface EventsWebMvcMockitoBeans {
}
