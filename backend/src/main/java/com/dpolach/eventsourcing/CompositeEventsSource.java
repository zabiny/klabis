package com.dpolach.eventsourcing;

import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public abstract class CompositeEventsSource<T extends EventsSource> implements EventsSource {
    private final List<BaseEvent> compositeEvents = new ArrayList<>();
    private final Collection<T> eventsSources;

    public CompositeEventsSource(Collection<T> eventsSources) {
        this.eventsSources = new ArrayList<>(eventsSources);
    }

    protected void addItem(T item) {
        this.eventsSources.add(item);
    }

    protected Stream<T> streamItems() {
        return eventsSources.stream();
    }

    @Override
    public void clearPendingEvents() {
        compositeEvents.clear();
        eventsSources.forEach(EventsSource::clearPendingEvents);

    }

    @Override
    public void andEvent(BaseEvent event) {
        compositeEvents.add(event);
    }

    @Override
    public List<BaseEvent> getPendingEvents() {
        return Stream.concat(compositeEvents.stream(),
                        eventsSources.stream().map(EventsSource::getPendingEvents).flatMap(Collection::stream))
                .sorted().toList();
    }

    @Override
    public final void apply(BaseEvent event) {
        try {
            handleEvent(event);
        } catch (NotImplementedException ex) {
            eventsSources.forEach(e -> e.apply(event));
        }
    }

}
