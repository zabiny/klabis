package com.dpolach.eventsourcing;

import com.dpolach.inmemoryrepository.InMemoryRepository;
import org.springframework.stereotype.Repository;

import java.util.stream.Stream;

@Repository
public interface EventsInMemoryRepository extends InMemoryRepository<BaseEvent, Long>, EventsRepository {

    @Override
    default void appendPendingEventsFrom(EventsSource eventsSource) {
        saveAll(eventsSource.getPendingEvents());
    }

    @Override
    default Stream<BaseEvent> streamAllEvents() {
        return findAll().stream();
    }

    @Override
    default <T extends CompositeEventsSource<?>> T rebuild(T compositeEventsSource) {
        findAll().forEach(compositeEventsSource::apply);
        return compositeEventsSource;
    }

    // TODO: how to query single item (= findById(Account.class, id)
    // TODO: how to query multiple with order?

}
