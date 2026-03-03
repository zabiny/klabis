package com.klabis.events.application;

import com.klabis.events.domain.Event;
import com.klabis.events.EventId;
import com.klabis.events.infrastructure.restapi.OwnRegistrationDto;
import com.klabis.events.infrastructure.restapi.RegistrationDto;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.time.LocalDate;
import java.util.List;

@PrimaryPort
public interface EventRegistrationService {

    void registerMember(EventId eventId, MemberId memberId, Event.RegisterCommand command);

    void unregisterMember(EventId eventId, MemberId memberId, LocalDate currentDate);

    List<RegistrationDto> listRegistrations(EventId eventId);

    OwnRegistrationDto getOwnRegistration(EventId eventId, MemberId memberId);
}
