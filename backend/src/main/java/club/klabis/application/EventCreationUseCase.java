package club.klabis.application;

import club.klabis.domain.events.Event;
import club.klabis.domain.events.forms.EventEditationForm;
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
