package com.klabis.events.application;

import com.klabis.events.domain.Event;
import com.klabis.events.EventId;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.SiCardNumber;
import com.klabis.events.domain.EventRepository;
import com.klabis.members.MemberId;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class EventRegistrationServiceImpl implements EventRegistrationService {

    private final EventRepository eventRepository;

    EventRegistrationServiceImpl(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public void registerMember(@NonNull EventId eventId, @NonNull MemberId memberId, Event.RegisterCommand command) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        event.registerMember(memberId, SiCardNumber.of(command.siCardNumber()));
        eventRepository.save(event);
    }

    @Override
    public void unregisterMember(@NonNull EventId eventId, @NonNull MemberId memberId, @NonNull LocalDate currentDate) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        if (event.findRegistration(memberId).isEmpty()) {
            throw new RegistrationNotFoundException(memberId, eventId);
        }

        event.unregisterMember(memberId, currentDate);
        eventRepository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventRegistration> listRegistrations(@NonNull EventId eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        return event.getRegistrations();
    }

    @Override
    @Transactional(readOnly = true)
    public EventRegistration getOwnRegistration(@NonNull EventId eventId, @NonNull MemberId memberId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        return event.findRegistration(memberId)
                .orElseThrow(() -> new RegistrationNotFoundException(memberId, eventId));
    }
}
