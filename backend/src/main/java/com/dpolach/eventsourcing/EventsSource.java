package com.dpolach.eventsourcing;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;

import java.util.List;

public interface EventsSource {
    Logger LOG = LoggerFactory.getLogger(EventsSource.class);

    @AfterDomainEventPublication
    void clearPendingEvents();

    void andEvent(BaseEvent event);

    @DomainEvents
    List<BaseEvent> getPendingEvents();

    default void apply(BaseEvent event) {
        handleEvent(event);
    }

    default void handleEvent(BaseEvent event) {
        String sourceModule = BaseEvent.getModuleName(getClass());
        if (sourceModule.equals(event.getModuleName())) {
            throw new NotImplementedException("Events from same module must be applied in EventsSource %s".formatted(
                    getClass().getCanonicalName()));
        } else {
            LOG.trace("No apply for event {} in events source {}", event, getClass().getCanonicalName());
        }
    }

}
