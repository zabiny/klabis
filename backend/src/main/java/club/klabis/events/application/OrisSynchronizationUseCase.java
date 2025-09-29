package club.klabis.events.application;

import club.klabis.events.domain.Competition;
import club.klabis.events.domain.Event;
import club.klabis.events.domain.OrisData;
import club.klabis.oris.domain.OrisId;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
public class OrisSynchronizationUseCase {

    private final EventsRepository eventsRepository;

    public OrisSynchronizationUseCase(EventsRepository eventsRepository) {
        this.eventsRepository = eventsRepository;
    }

    public Collection<OrisId> getOrisIds(Collection<Event.Id> eventIds) {
        return eventIds.stream()
                .map(eventsRepository::findById)
                .flatMap(Optional::stream)
                .filter(Event::hasOrisId)
                .map(Event::getOrisId)
                .flatMap(Optional::stream)
                .toList();
    }

    public void importEvent(@Valid OrisData orisData) {
        Event updatedEvent = eventsRepository.findByOrisId(orisData.orisId())
                .map(e -> {
                    e.synchronize(orisData);
                    return e;
                })
                .orElse(Competition.importFrom(orisData));

        eventsRepository.save(updatedEvent);
    }
}
