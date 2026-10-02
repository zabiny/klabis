package com.klabis.calendar.infrastructure.restapi;

import com.klabis.calendar.application.CalendarManagementPort;
import com.klabis.calendar.application.IcalFeedPort;
import com.klabis.calendar.application.IcalTokenPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mocks the primary ports required by the web beans of the calendar module (calendar, iCal feed and
 * iCal token controllers) so that other modules' {@code @WebMvcTest} slices can load them without
 * the calendar application layer. The real {@code ICalendarRenderer} (a stateless adapter-package bean) is
 * imported through its configuration rather than mocked.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(CalendarInfrastructureConfiguration.class)
@MockitoBean(types = {
        CalendarManagementPort.class,
        IcalTokenPort.class,
        IcalFeedPort.class
})
public @interface CalendarWebMvcMockitoBeans {
}
