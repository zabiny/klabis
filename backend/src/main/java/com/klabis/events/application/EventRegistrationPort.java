package com.klabis.events.application;

import com.klabis.events.EventId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRegistration;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.jspecify.annotations.NonNull;

import java.util.List;

@PrimaryPort
public interface EventRegistrationPort {

    void registerMember(@NonNull EventId eventId, @NonNull MemberId memberId, Event.RegisterCommand command);

    void unregisterMember(@NonNull EventId eventId, @NonNull MemberId memberId);

    List<EventRegistration> listRegistrations(@NonNull EventId eventId);

}
