package com.dpolach.eventsourcing;

import com.dpolach.inmemoryrepository.InMemoryRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
class EventsRepositoryAdapterFromInMemoryRepository implements EventsRepository {

    private final InMemoryRepository<BaseEvent, Long> eventsRepository;
    private final ApplicationEventPublisher eventPublisher;

    public EventsRepositoryAdapterFromInMemoryRepository(Repo repository, ApplicationEventPublisher eventPublisher) {
        this.eventsRepository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @Override
    public synchronized void appendPendingEventsFrom(EventsSource eventsSource) {
        List<BaseEvent> pendingEvens = eventsSource.getPendingEvents();
        eventsRepository.saveAll(pendingEvens);
        pendingEvens.forEach(eventPublisher::publishEvent);
        eventsSource.clearPendingEvents();
    }

    @Override
    public synchronized void appendEvent(BaseEvent event) {
        eventsRepository.save(event);
        eventPublisher.publishEvent(event);
    }

    @Override
    public Stream<BaseEvent> streamAllEvents() {
        return eventsRepository.findAll().stream().sorted(Comparator.comparing(BaseEvent::getSequenceId));
    }

    @Override
    public long size() {
        return eventsRepository.count();
    }

    // TODO: how to query multiple with order? - create projection with needed data and which can be easily ordered - basically Read model is simple CRUD.

}
