package com.klabis.events.application;

import com.klabis.common.users.Authority;
import com.klabis.events.EventId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRegistration;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberAccommodationDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import org.jmolecules.ddd.annotation.Service;
import org.jspecify.annotations.NonNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
class AccommodationListService implements AccommodationListPort {

    private final EventManagementPort eventManagement;
    private final Members members;

    AccommodationListService(EventManagementPort eventManagement, Members members) {
        this.eventManagement = eventManagement;
        this.members = members;
    }

    @Override
    public AccommodationList getAccommodationList(@NonNull EventId eventId, @NonNull CurrentUserData caller) {
        Event event = eventManagement.getEvent(eventId, false);
        authorize(event, caller);

        List<EventRegistration> registrations = event.getRegistrations().stream()
                .filter(EventRegistration::wantsSharedAccommodation)
                .toList();
        List<MemberId> memberIds = registrations.stream().map(EventRegistration::memberId).toList();
        Map<MemberId, MemberAccommodationDto> memberData = members.findAccommodationDataByIds(memberIds);

        List<AccommodationList.AccommodationListRow> rows = registrations.stream()
                .map(registration -> new AccommodationList.AccommodationListRow(
                        eventId.value(), registration, memberData.get(registration.memberId())))
                .toList();
        return new AccommodationList(eventId.value(), event.getName(), rows);
    }

    private static void authorize(Event event, CurrentUserData caller) {
        if (!caller.hasAuthority(Authority.EVENTS_REGISTRATIONS) && !caller.isMemberOf(event::isCoordinator)) {
            throw AccommodationListAccessDeniedException.callerNotPermitted();
        }
        if (!event.isSharedAccommodationEnabled()) {
            throw AccommodationListAccessDeniedException.sharedAccommodationNotOffered();
        }
    }
}
