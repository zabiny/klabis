package club.klabis.domain.events;

import club.klabis.domain.events.forms.EventEditationForm;

import java.util.Optional;

public interface EventsService {

    Event createNewEvent(EventEditationForm form);

    Optional<Event> findByOrisId(int orisId);
}
