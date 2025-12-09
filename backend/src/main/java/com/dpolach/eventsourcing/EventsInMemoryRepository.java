package com.dpolach.eventsourcing;

import com.dpolach.inmemoryrepository.InMemoryRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
class EventsInMemoryRepository implements EventsRepository {

    private final InMemoryRepository<BaseEvent, Long> eventsRepository;
    private final ApplicationEventPublisher eventPublisher;

    public EventsInMemoryRepository(Repo repository, ApplicationEventPublisher eventPublisher) {
        this.eventsRepository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @Override
    public void appendPendingEventsFrom(EventsSource eventsSource) {
        List<BaseEvent> pendingEvens = eventsSource.getPendingEvents();
        eventsRepository.saveAll(pendingEvens);
        pendingEvens.forEach(eventPublisher::publishEvent);
        eventsSource.clearPendingEvents();
    }

    @Override
    public void appendEvent(BaseEvent event) {
        eventsRepository.save(event);
    }

    @Override
    public Stream<BaseEvent> streamAllEvents() {
        return eventsRepository.findAll().stream().sorted(Comparator.comparing(BaseEvent::getSequenceId));
    }

    // TODO: how to query single item (= findById(Account.class, id)
    // TODO: how to query multiple with order?

}
