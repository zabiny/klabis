package com.klabis.events.infrastructure.bootstrap;

import com.klabis.common.bootstrap.BootstrapDataInitializer;
import com.klabis.events.application.EventTypeManagementPort;
import com.klabis.events.domain.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class EventTypeDataBootstrap implements BootstrapDataInitializer {

    public static final String RACE_TYPE_NAME = "Závod";
    public static final String TRAINING_TYPE_NAME = "Trénink";

    private static final Logger LOG = LoggerFactory.getLogger(EventTypeDataBootstrap.class);

    private final EventTypeManagementPort eventTypeManagement;

    EventTypeDataBootstrap(EventTypeManagementPort eventTypeManagement) {
        this.eventTypeManagement = eventTypeManagement;
    }

    @Override
    public boolean requiresBootstrap() {
        return eventTypeManagement.listAllSorted().isEmpty();
    }

    @Override
    public void bootstrapData() {
        eventTypeManagement.createEventType(new EventType.CreateEventType(RACE_TYPE_NAME, "#1d4ed8", 0, null));
        eventTypeManagement.createEventType(new EventType.CreateEventType(TRAINING_TYPE_NAME, "#16a34a", 1, null));

        LOG.info("Created 2 bootstrap event types ({}, {})", RACE_TYPE_NAME, TRAINING_TYPE_NAME);
    }
}
