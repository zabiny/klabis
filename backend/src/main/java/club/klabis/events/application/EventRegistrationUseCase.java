package club.klabis.events.application;

import club.klabis.events.domain.Event;
import club.klabis.events.domain.EventException;
import club.klabis.events.domain.Registration;
import club.klabis.events.domain.commands.EventRegistrationCommand;
import club.klabis.events.domain.commands.EventRegistrationCommandBuilder;
import club.klabis.members.MemberId;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventRegistrationUseCase {

    private final EventsRepository eventsRepository;
    private final MemberSiCardProvider memberSiCardProvider;

    public EventRegistrationUseCase(EventsRepository eventsRepository, MemberSiCardProvider memberSiCardProvider) {
        this.eventsRepository = eventsRepository;
        this.memberSiCardProvider = memberSiCardProvider;
    }

    public record EventRegistrationFormData(@JsonUnwrapped EventRegistrationCommand eventRegistrationCommand,
                                            @JsonProperty(access = JsonProperty.Access.READ_ONLY) String eventName) {
    }

    public EventRegistrationFormData getEventRegistrationForm(Event.Id eventId, MemberId memberId) {
        Event event = eventsRepository.findById(eventId)
                .orElseThrow(() -> EventException.createEventNotFoundException(eventId));

        return event.getRegistrationForMember(memberId)
                .map(registration -> toForm(event, toForm(registration)))
                .orElseGet(() -> toForm(event,
                        new EventRegistrationCommand(memberSiCardProvider.getSiCardForMember(memberId).orElse(null),
                                null)));
    }

    private EventRegistrationFormData toForm(Event event, EventRegistrationCommand registrationForm) {
        return new EventRegistrationFormData(registrationForm, event.getName());
    }

    private EventRegistrationCommand toForm(Registration registration) {
        return EventRegistrationCommandBuilder.builder()
                .category(registration.getCategory().name())
                .siNumber(registration.getSiNumber())
                .build();
    }

    @Transactional
    public void registerForEvent(Event.Id eventId, MemberId memberId, EventRegistrationCommand eventRegistrationCommand) {
        Event event = eventsRepository.findById(eventId)
                .orElseThrow(() -> EventException.createEventNotFoundException(eventId));

        if (event.isMemberRegistered(memberId)) {
            event.changeRegistration(memberId, eventRegistrationCommand);
        } else {
            event.registerMember(memberId, eventRegistrationCommand);
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
