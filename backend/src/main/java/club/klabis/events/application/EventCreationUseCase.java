package club.klabis.events.application;

import club.klabis.events.domain.Event;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

@Service
public class EventCreationUseCase {
    private final EventsRepository repository;

    EventCreationUseCase(EventsRepository repository) {
        this.repository = repository;
    }

    public Event createNewEvent(@Valid EventManagementForm form) {
        return repository.save(form.createNew());
    }

}
