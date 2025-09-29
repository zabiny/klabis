package club.klabis.events.application;

import club.klabis.events.domain.Competition;
import club.klabis.events.domain.Event;
import club.klabis.events.domain.forms.EventEditationForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

@Service
public class EventCreationUseCase {
    private final EventsRepository repository;

    EventCreationUseCase(EventsRepository repository) {
        this.repository = repository;
    }

    public Event createNewEvent(@Valid EventEditationForm form) {
        return repository.save(Competition.newEvent(form));
    }

}
