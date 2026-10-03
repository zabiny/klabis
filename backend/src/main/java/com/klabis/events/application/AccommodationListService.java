package com.klabis.events.application;

import com.klabis.common.users.Authority;
import com.klabis.events.EventId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.EventStatus;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberAccommodationDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import org.jmolecules.ddd.annotation.Service;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
class AccommodationListService implements AccommodationListPort {

    private final EventRepository eventRepository;
    private final Members members;

    AccommodationListService(EventRepository eventRepository, Members members) {
        this.eventRepository = eventRepository;
        this.members = members;
    }

    @Override
    public AccommodationList getAccommodationList(@NonNull EventId eventId, @NonNull CurrentUserData caller) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        if (event.getStatus() == EventStatus.DRAFT) {
            throw new EventNotFoundException(eventId);
        }
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
            throw new AccessDeniedException("Access to accommodation list requires EVENTS:REGISTRATIONS authority or being the event coordinator");
        }
        if (!event.isSharedAccommodationEnabled()) {
            throw new AccessDeniedException("Accommodation list is available only when the event offers shared accommodation");
        }
    }
}
