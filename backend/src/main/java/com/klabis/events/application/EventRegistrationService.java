package com.klabis.events.application;

import com.klabis.events.EventId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.SiCardNumber;
import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.Service;
import org.jspecify.annotations.NonNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EventRegistrationService implements EventRegistrationPort {

    private final EventRepository eventRepository;

    EventRegistrationService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public void registerMember(@NonNull EventId eventId, @NonNull MemberId memberId, Event.RegisterCommand command) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        event.registerMember(memberId, SiCardNumber.of(command.siCardNumber()), command.category());
        eventRepository.save(event);
    }

    @Override
    public void unregisterMember(@NonNull EventId eventId, @NonNull MemberId memberId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        event.unregisterMember(new Event.UnregisterMember(memberId));
        eventRepository.save(event);
    }

    @Override
    public void editRegistration(@NonNull EventId eventId, @NonNull MemberId memberId, Event.EditRegistrationCommand command) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        event.editRegistration(memberId, command);
        eventRepository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventRegistration> listRegistrations(@NonNull EventId eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        return event.getRegistrations();
    }

}
