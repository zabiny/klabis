package com.dpolach.eventsourcing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class SingleEventsSource implements EventsSource {

    static final Logger LOG = LoggerFactory.getLogger(SingleEventsSource.class);

    private final Collection<BaseEvent> pendingEvents = new ArrayList<>();

    @AfterDomainEventPublication
    @Override
    public void clearPendingEvents() {
        pendingEvents.clear();
    }

    @Override
    public void andEvent(BaseEvent event) {
        this.pendingEvents.add(event);
    }

    @DomainEvents
    @Override
    public List<BaseEvent> getPendingEvents() {
        return new ArrayList<>(pendingEvents);
    }

    @Override
    public final void apply(BaseEvent event) {
        EventsSource.super.apply(event);
    }

}
