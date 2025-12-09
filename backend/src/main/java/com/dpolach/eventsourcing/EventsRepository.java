package com.dpolach.eventsourcing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface EventsRepository {
    void appendPendingEventsFrom(EventsSource eventsSource);

    void appendEvent(BaseEvent event);

    Stream<BaseEvent> streamAllEvents();

    default Page<BaseEvent> getEvents(Pageable pageable) {
        List<BaseEvent> data = streamAllEvents()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList();

        return new PageImpl<>(data, pageable, size());
    }

    long size();

    default <T> Optional<T> project(Projector<T> projector) {
        streamAllEvents().forEach(projector::project);
        projector.completed();
        return projector.getResult();
    }
}
