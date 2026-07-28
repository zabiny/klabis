package com.klabis.calendar.infrastructure.restapi;

import com.klabis.calendar.CalendarItemId;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class CalendarOpenApiConfig {

    public CalendarOpenApiConfig() {
        SpringDocUtils.getConfig().replaceWithClass(CalendarItemId.class, UUID.class);
    }
}
