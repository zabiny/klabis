package club.klabis.adapters.inmemorystorage;

import club.klabis.domain.events.Event;
import club.klabis.domain.events.EventsRepository;

import java.util.Optional;

class EventsInMemoryRepository extends InMemoryRepositoryImpl<Event, Event.Id> implements EventsRepository {
    public EventsInMemoryRepository() {
        super(Event::getId);
    }

    @Override
    public Optional<Event> findByOrisId(int orisId) {
        return findAll().stream()
                .filter(this::hasOrisId)
                .filter(it -> it.getOrisId().orElseThrow() == orisId)
                .findAny();
    }

    private boolean hasOrisId(Event event) {
        return event.getOrisId().isPresent();
    }
}
