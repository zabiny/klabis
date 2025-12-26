package club.klabis.events.application;

import club.klabis.events.domain.Event;
import club.klabis.events.domain.commands.EventManagementCommand;
import club.klabis.shared.config.ddd.UseCase;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Transactional
public class EventCreationUseCase {
    private final EventsRepository repository;

    EventCreationUseCase(EventsRepository repository) {
        this.repository = repository;
    }

    public Event createNewEvent(@Valid EventManagementCommand form) {
        return repository.save(form.createNew());
    }

}
