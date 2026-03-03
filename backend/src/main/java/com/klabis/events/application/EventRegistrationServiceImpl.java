package com.klabis.events.application;

import com.klabis.events.domain.Event;
import com.klabis.events.EventId;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.SiCardNumber;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.infrastructure.restapi.DuplicateRegistrationException;
import com.klabis.events.infrastructure.restapi.EventNotFoundException;
import com.klabis.events.infrastructure.restapi.OwnRegistrationDto;
import com.klabis.events.infrastructure.restapi.RegistrationDto;
import com.klabis.events.infrastructure.restapi.RegistrationNotFoundException;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventRegistrationServiceImpl implements EventRegistrationService {

    private final EventRepository eventRepository;
    private final Members members;

    EventRegistrationServiceImpl(EventRepository eventRepository, Members members) {
        this.eventRepository = eventRepository;
        this.members = members;
    }

    @Override
    public void registerMember(EventId eventId, MemberId memberId, Event.RegisterCommand command) {
        Assert.notNull(eventId, "Event id must not be null");
        Assert.notNull(memberId, "Member id must not be null");

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        if (event.findRegistration(memberId).isPresent()) {
            throw new DuplicateRegistrationException(memberId, eventId);
        }

        event.registerMember(memberId, SiCardNumber.of(command.siCardNumber()));
        eventRepository.save(event);
    }

    @Override
    public void unregisterMember(EventId eventId, MemberId memberId, LocalDate currentDate) {
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
    public List<RegistrationDto> listRegistrations(EventId eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        return event.getRegistrations().stream()
                .map(this::toRegistrationDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OwnRegistrationDto getOwnRegistration(EventId eventId, MemberId memberId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        EventRegistration registration = event.findRegistration(memberId)
                .orElseThrow(() -> new RegistrationNotFoundException(memberId, eventId));

        return toOwnRegistrationDto(registration);
    }

    private RegistrationDto toRegistrationDto(EventRegistration registration) {
        MemberDto member = members.findById(registration.memberId())
                .orElseThrow(() -> new IllegalStateException(
                        "Member not found for registration: " + registration.memberId()));

        return new RegistrationDto(
                member.firstName(),
                member.lastName(),
                registration.registeredAt()
        );
    }

    private OwnRegistrationDto toOwnRegistrationDto(EventRegistration registration) {
        MemberDto member = members.findById(registration.memberId())
                .orElseThrow(() -> new IllegalStateException(
                        "Member not found for registration: " + registration.memberId()));

        return new OwnRegistrationDto(
                member.firstName(),
                member.lastName(),
                registration.siCardNumber().value(),
                registration.registeredAt()
        );
    }
}
