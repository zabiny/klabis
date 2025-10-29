package com.dpolach.eventsourcing;

import java.util.stream.Stream;

public interface EventsRepository {
    void appendPendingEventsFrom(EventsSource eventsSource);

    Stream<BaseEvent> streamAllEvents();

    default <T extends CompositeEventsSource<?>> T rebuild(T compositeEventsSource) {
        streamAllEvents().forEach(compositeEventsSource::apply);
        compositeEventsSource.clearPendingEvents();
        return compositeEventsSource;
    }
}
