package club.klabis.domain.events;

import club.klabis.domain.events.forms.EventEditationForm;
import org.jmolecules.ddd.annotation.Service;

import java.util.Optional;

@Service
@org.springframework.stereotype.Service
class EventsServiceImpl implements EventsService {
    private final EventsRepository repository;

    EventsServiceImpl(EventsRepository repository) {
        this.repository = repository;
    }

    @Override
    public Event createNewEvent(EventEditationForm form) {
        return repository.save(Event.newEvent(form));
    }

    @Override
    public Optional<Event> findByOrisId(int orisId) {
        return repository.findByOrisId(orisId);
    }
}
