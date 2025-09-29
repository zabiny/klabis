package club.klabis.events.application;

import club.klabis.events.domain.Event;
import club.klabis.events.domain.EventException;
import club.klabis.events.domain.forms.EventRegistrationForm;
import club.klabis.members.MemberId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventRegistrationUseCase {

    private final EventsRepository eventsRepository;

    public EventRegistrationUseCase(EventsRepository eventsRepository) {
        this.eventsRepository = eventsRepository;
    }

    public EventRegistrationForm createEventRegistrationForm(Event.Id eventId, MemberId memberId) {
        // read event and prepare registration form (categories, services, ... )
        return new EventRegistrationForm("predefinedSiForMember", null);
    }

    @Transactional
    public void registerForEvent(Event.Id eventId, MemberId memberId, EventRegistrationForm eventRegistrationForm) {
        Event event = eventsRepository.findById(eventId)
                .orElseThrow(() -> EventException.createEventNotFoundException(eventId));
        event.registerMember(memberId, eventRegistrationForm);
        eventsRepository.save(event);
    }

    @Transactional
    public void cancelMemberRegistration(Event.Id eventId, MemberId memberId) {
        Event event = eventsRepository.findById(eventId)
                .orElseThrow(() -> EventException.createEventNotFoundException(eventId));
        event.cancelMemberRegistration(memberId);
        eventsRepository.save(event);
    }

}
