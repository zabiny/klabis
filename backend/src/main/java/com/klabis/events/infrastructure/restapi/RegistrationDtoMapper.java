package com.klabis.events.infrastructure.restapi;

import com.klabis.events.domain.EventRegistration;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;

import java.util.List;
import java.util.Map;

class RegistrationDtoMapper {

    static RegistrationDto toDto(EventRegistration registration, Map<MemberId, MemberDto> memberIndex, Members members) {
        MemberDto member = memberIndex.get(registration.memberId());
        if (member == null) {
            member = members.findById(registration.memberId())
                    .orElseThrow(() -> new IllegalStateException("Member not found for registration: " + registration.memberId()));
        }
        return new RegistrationDto(member.firstName(), member.lastName(), registration.registeredAt());
    }

    static List<RegistrationDto> toDtoList(List<EventRegistration> registrations, Members members) {
        List<MemberId> memberIds = registrations.stream().map(EventRegistration::memberId).toList();
        Map<MemberId, MemberDto> memberIndex = members.findByIds(memberIds);
        return registrations.stream()
                .map(r -> toDto(r, memberIndex, members))
                .toList();
    }
}
