package com.klabis.calendar.infrastructure.restapi;

import com.klabis.calendar.infrastructure.ical.ICalendarRenderer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CalendarInfrastructureConfiguration {

    @Bean
    ICalendarRenderer icalendarRenderer() {
        return new ICalendarRenderer();
    }
}
