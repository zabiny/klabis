package com.klabis.events.infrastructure.restapi;

import com.klabis.events.CategoryPresetId;
import com.klabis.events.EventCategoryId;
import com.klabis.events.EventId;
import com.klabis.events.EventTypeId;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class EventsOpenApiConfig {

    public EventsOpenApiConfig() {
        SpringDocUtils.getConfig()
                .replaceWithClass(EventId.class, UUID.class)
                .replaceWithClass(EventTypeId.class, UUID.class)
                .replaceWithClass(EventCategoryId.class, UUID.class)
                .replaceWithClass(CategoryPresetId.class, UUID.class);
    }
}
