package club.klabis.events.application;

import club.klabis.events.domain.Event;
import club.klabis.events.domain.forms.EventEditationForm;
import org.springframework.stereotype.Service;

@Service
public class EventCreationUseCase {
    private final EventsRepository repository;

    EventCreationUseCase(EventsRepository repository) {
        this.repository = repository;
    }

    public Event createNewEvent(EventEditationForm form) {
        return repository.save(Event.newEvent(form));
    }

}
