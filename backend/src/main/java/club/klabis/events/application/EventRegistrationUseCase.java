package club.klabis.events.application;

import club.klabis.events.domain.Competition;
import club.klabis.events.domain.Event;
import club.klabis.events.domain.EventException;
import club.klabis.events.domain.Registration;
import club.klabis.events.domain.forms.EventRegistrationForm;
import club.klabis.events.domain.forms.EventRegistrationFormBuilder;
import club.klabis.members.MemberId;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventRegistrationUseCase {

    private final EventsRepository eventsRepository;
    private final MemberSiCardProvider memberSiCardProvider;

    public EventRegistrationUseCase(EventsRepository eventsRepository, MemberSiCardProvider memberSiCardProvider) {
        this.eventsRepository = eventsRepository;
        this.memberSiCardProvider = memberSiCardProvider;
    }

    public record EventRegistrationFormData(@JsonUnwrapped EventRegistrationForm eventRegistrationForm,
                                            @JsonProperty(access = JsonProperty.Access.READ_ONLY) String eventName) {
    }

    public EventRegistrationFormData getEventRegistrationForm(Event.Id eventId, MemberId memberId) {
        Event event = eventsRepository.findById(eventId)
                .orElseThrow(() -> EventException.createEventNotFoundException(eventId));

        return event.getRegistrationForMember(memberId)
                .map(registration -> toForm(event, toForm(registration)))
                .orElseGet(() -> toForm(event,
                        new EventRegistrationForm(memberSiCardProvider.getSiCardForMember(memberId).orElse(null),
                                null)));
    }

    public List<String> getEventCategories(Event.Id eventId) {
        Event event = eventsRepository.findById(eventId)
                .orElseThrow(() -> EventException.createEventNotFoundException(eventId));

        if (event instanceof Competition competition) {
            return competition.getCategories().stream().map(Competition.Category::name).toList();
        }
        return List.of();
    }

    private EventRegistrationFormData toForm(Event event, EventRegistrationForm registrationForm) {
        return new EventRegistrationFormData(registrationForm, event.getName());
    }

    private EventRegistrationForm toForm(Registration registration) {
        return EventRegistrationFormBuilder.builder()
                .category(registration.getCategory().name())
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
