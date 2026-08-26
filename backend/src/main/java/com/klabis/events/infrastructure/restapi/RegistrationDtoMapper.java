package com.klabis.events.infrastructure.restapi;

import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRegistration;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

class RegistrationDtoMapper {

    static RegistrationSummaryDto toDto(EventRegistration registration, Map<MemberId, MemberDto> memberIndex, Members members, Event event) {
        MemberDto member = memberIndex.get(registration.memberId());
        if (member == null) {
            member = members.findById(registration.memberId())
                    .orElseThrow(() -> new IllegalStateException("Member not found for registration: " + registration.memberId()));
        }
        return new RegistrationSummaryDto(
                toCategoryDto(registration, event),
                toCoordinatorUuids(event.getCoordinators()),
                member.firstName(),
                member.lastName(),
                registration.memberId().uuid(),
                registration.registeredAt()
        );
    }

    private static List<UUID> toCoordinatorUuids(Set<MemberId> coordinators) {
        return coordinators.stream().map(MemberId::uuid).toList();
    }

    static EventCategoryDto toCategoryDto(EventRegistration registration, Event event) {
        return event.findCategory(registration.categoryId())
                .map(category -> new EventCategoryDto(null, category.id().value(), category.name()))
                .orElse(null);
    }

    static List<RegistrationSummaryDto> toDtoList(List<EventRegistration> registrations, Members members, Event event) {
        List<MemberId> memberIds = registrations.stream().map(EventRegistration::memberId).toList();
        Map<MemberId, MemberDto> memberIndex = members.findByIds(memberIds);
        return registrations.stream()
                .map(r -> toDto(r, memberIndex, members, event))
                .toList();
    }
}
