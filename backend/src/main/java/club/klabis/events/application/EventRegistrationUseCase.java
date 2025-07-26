package club.klabis.events.application;

import club.klabis.events.domain.Event;
import club.klabis.events.domain.EventException;
import club.klabis.events.domain.forms.EventRegistrationForm;
import club.klabis.members.domain.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventRegistrationUseCase {

    private final EventsRepository eventsRepository;

    public EventRegistrationUseCase(EventsRepository eventsRepository) {
        this.eventsRepository = eventsRepository;
    }

    public EventRegistrationForm newRegistrationForMember(Event.Id eventId, Member.Id memberId) {
        // read event and prepare registration form (categories, services, ... )
        return new EventRegistrationForm(memberId, null);
    }

    @Transactional
    public void registerForEvent(Event.Id eventId, EventRegistrationForm eventRegistrationForm) {
        Event event = eventsRepository.findById(eventId)
                .orElseThrow(() -> EventException.createEventNotFoundException(eventId));
        event.addEventRegistration(eventRegistrationForm);
        eventsRepository.save(event);
    }

    @Transactional
    public void deregisterFromEvent(Event.Id eventId, Member.Id memberId) {
        Event event = eventsRepository.findById(eventId)
                .orElseThrow(() -> EventException.createEventNotFoundException(eventId));
        event.removeEventRegistration(memberId);
        eventsRepository.save(event);
    }

}
