package com.klabis.events.infrastructure.restapi;

import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRegistration;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;

import java.util.List;
import java.util.Map;
import java.util.Set;

class RegistrationDtoMapper {

    static RegistrationSummaryDto toDto(EventRegistration registration, Map<MemberId, MemberDto> memberIndex, Members members, Event event) {
        MemberDto member = memberIndex.get(registration.memberId());
        if (member == null) {
            member = members.findById(registration.memberId())
                    .orElseThrow(() -> new IllegalStateException("Member not found for registration: " + registration.memberId()));
        }
        Set<MemberId> coordinators = event.getCoordinators();
        return new RegistrationSummaryDto(
                member.firstName(),
                member.lastName(),
                registration.category(),
                registration.registeredAt(),
                coordinators,
                registration.memberId()
        );
    }

    static List<RegistrationSummaryDto> toDtoList(List<EventRegistration> registrations, Members members, Event event) {
        List<MemberId> memberIds = registrations.stream().map(EventRegistration::memberId).toList();
        Map<MemberId, MemberDto> memberIndex = members.findByIds(memberIds);
        return registrations.stream()
                .map(r -> toDto(r, memberIndex, members, event))
                .toList();
    }
}
