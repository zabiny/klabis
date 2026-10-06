package com.klabis.events.application;

import com.klabis.events.domain.EventRegistration;
import com.klabis.members.MemberAccommodationDto;

import java.util.List;
import java.util.UUID;

public record AccommodationList(UUID eventId, String eventName, List<AccommodationListRow> rows) {

    public record AccommodationListRow(UUID eventId, EventRegistration registration, MemberAccommodationDto memberData) {
    }
}
