package club.klabis.events.application;

import club.klabis.events.domain.Competition;
import club.klabis.events.domain.Event;
import club.klabis.events.domain.EventException;
import club.klabis.events.domain.Registration;
import club.klabis.events.domain.forms.EventRegistrationForm;
import club.klabis.events.domain.forms.EventRegistrationFormBuilder;
import club.klabis.members.MemberId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventRegistrationUseCase {

    private final EventsRepository eventsRepository;

    public EventRegistrationUseCase(EventsRepository eventsRepository) {
        this.eventsRepository = eventsRepository;
    }

    public EventRegistrationForm getEventRegistrationForm(Event.Id eventId, MemberId memberId) {
        Event event = eventsRepository.findById(eventId)
                .orElseThrow(() -> EventException.createEventNotFoundException(eventId));

        return event.getRegistrationForMember(memberId)
                .map(registration -> toForm(event, registration))
                .orElseGet(() -> new EventRegistrationForm("predefinedSiForMember", null));
    }

    public List<String> getEventCategories(Event.Id eventId) {
        Event event = eventsRepository.findById(eventId)
                .orElseThrow(() -> EventException.createEventNotFoundException(eventId));

        if (event instanceof Competition competition) {
            return competition.getCategories().stream().map(Competition.Category::name).toList();
        }
        return List.of();
    }

    private EventRegistrationForm toForm(Event event, Registration registration) {
        return EventRegistrationFormBuilder.builder()
                .category(registration.getCategory())
                .siNumber(registration.getSiNumber())
                .build();
    }

    @Transactional
    public void registerForEvent(Event.Id eventId, MemberId memberId, EventRegistrationForm eventRegistrationForm) {
        Event event = eventsRepository.findById(eventId)
                .orElseThrow(() -> EventException.createEventNotFoundException(eventId));

        if (event.isMemberRegistered(memberId)) {
            event.changeRegistration(memberId, eventRegistrationForm);
        } else {
            event.registerMember(memberId, eventRegistrationForm);
        }

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
