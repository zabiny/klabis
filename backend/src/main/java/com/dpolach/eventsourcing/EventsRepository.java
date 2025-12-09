package com.dpolach.eventsourcing;

import java.util.Optional;
import java.util.stream.Stream;

public interface EventsRepository {
    void appendPendingEventsFrom(EventsSource eventsSource);

    void appendEvent(BaseEvent event);

    Stream<BaseEvent> streamAllEvents();

    default <T extends CompositeEventsSource<?>> T rebuild(T compositeEventsSource) {
        streamAllEvents().forEach(compositeEventsSource::apply);
        compositeEventsSource.clearPendingEvents();
        return compositeEventsSource;
    }

    default <T> Optional<T> project(Projector<T> projector) {
        streamAllEvents().forEach(projector::project);
        projector.completed();
        return projector.getResult();
    }
}
