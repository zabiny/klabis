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
        return RegistrationSummaryDtoBuilder.builder()
                .firstName(member.firstName())
                .lastName(member.lastName())
                .category(toCategoryDto(registration, event))
                .registrationTime(registration.registeredAt())
                .wantsSharedTransport(wantsSharedTransport(registration, event))
                .wantsSharedAccommodation(wantsSharedAccommodation(registration, event))
                .coordinators(toCoordinatorUuids(event.getCoordinators()))
                .registeredMemberId(registration.memberId().uuid())
                .build();
    }

    private static List<UUID> toCoordinatorUuids(Set<MemberId> coordinators) {
        return coordinators.stream().map(MemberId::uuid).toList();
    }

    static Boolean wantsSharedTransport(EventRegistration registration, Event event) {
        return event.isSharedTransportEnabled() ? registration.wantsSharedTransport() : null;
    }

    static Boolean wantsSharedAccommodation(EventRegistration registration, Event event) {
        return event.isSharedAccommodationEnabled() ? registration.wantsSharedAccommodation() : null;
    }

    static EventCategoryDto toCategoryDto(EventRegistration registration, Event event) {
        return event.findCategory(registration.categoryId())
                .map(category -> EventCategoryDtoBuilder.builder()
                        .id(category.id().value())
                        .name(category.name())
                        .fee(null)
                        .build())
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
