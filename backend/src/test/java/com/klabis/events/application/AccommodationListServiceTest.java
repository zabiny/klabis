package com.klabis.events.application;

import com.klabis.common.users.Authority;
import com.klabis.events.EventId;
import com.klabis.events.EventTestDataBuilder;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.SiCardNumber;
import com.klabis.common.users.UserId;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberAccommodationDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccommodationListService Unit Tests")
class AccommodationListServiceTest {

    private static final MemberId COORDINATOR = new MemberId(UUID.randomUUID());
    private static final MemberId OTHER_MEMBER = new MemberId(UUID.randomUUID());

    @Mock
    private EventRepository eventRepository;

    @Mock
    private Members members;

    private AccommodationListPort service;

    @BeforeEach
    void setUp() {
        service = new AccommodationListService(eventRepository, members);
    }

    private static CurrentUserData caller(MemberId memberId, Authority... authorities) {
        return new CurrentUserData("user", UserId.newId(), memberId, Set.of(authorities));
    }

    private static EventRegistration registration(MemberId memberId, boolean wantsAccommodation) {
        return EventRegistration.reconstruct(UUID.randomUUID(), memberId, SiCardNumber.of("1234"),
                null, Instant.now(), false, wantsAccommodation);
    }

    private static MemberAccommodationDto memberData(String firstName) {
        return new MemberAccommodationDto(firstName, "Doe", "AB123456", LocalDate.of(2028, 1, 1),
                LocalDate.of(1985, 5, 15), "Main St 1", "Prague", "11000", "CZ");
    }

    private EventId stubEvent(Event event) {
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        return event.getId();
    }

    private static EventTestDataBuilder accommodationEvent() {
        return EventTestDataBuilder.anEvent()
                .withSharedAccommodationEnabled(true)
                .withCoordinator(COORDINATOR);
    }

    @Test
    @DisplayName("coordinator gets rows only for registrations wanting shared accommodation, with member data")
    void coordinatorGetsFilteredRows() {
        MemberId wantsIt = new MemberId(UUID.randomUUID());
        MemberId doesNot = new MemberId(UUID.randomUUID());
        Event event = accommodationEvent()
                .withName("Camp")
                .addRegistrations(List.of(registration(wantsIt, true), registration(doesNot, false)))
                .buildPublished();
        EventId eventId = stubEvent(event);
        MemberAccommodationDto data = memberData("Wants");
        when(members.findAccommodationDataByIds(any())).thenReturn(Map.of(wantsIt, data));
        CurrentUserData caller = caller(COORDINATOR);

        AccommodationList result = service.getAccommodationList(eventId, caller);

        assertThat(result.eventId()).isEqualTo(eventId.value());
        assertThat(result.eventName()).isEqualTo("Camp");
        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().getFirst().eventId()).isEqualTo(eventId.value());
        assertThat(result.rows().getFirst().registration().memberId()).isEqualTo(wantsIt);
        assertThat(result.rows().getFirst().memberData()).isEqualTo(data);
    }

    @Test
    @DisplayName("caller with EVENTS:REGISTRATIONS who is not the coordinator is allowed")
    void registrationsAuthorityIsAllowed() {
        Event event = accommodationEvent().buildPublished();
        EventId eventId = stubEvent(event);
        when(members.findAccommodationDataByIds(any())).thenReturn(Map.of());
        CurrentUserData caller = caller(OTHER_MEMBER, Authority.EVENTS_REGISTRATIONS);

        assertThat(service.getAccommodationList(eventId, caller).rows()).isEmpty();
    }

    @Test
    @DisplayName("caller who is neither coordinator nor holds EVENTS:REGISTRATIONS is denied")
    void otherCallerIsDenied() {
        Event event = accommodationEvent().buildPublished();
        EventId eventId = stubEvent(event);
        CurrentUserData caller = caller(OTHER_MEMBER);

        assertThatThrownBy(() -> service.getAccommodationList(eventId, caller)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(members);
    }

    @Test
    @DisplayName("authorized caller is denied when shared accommodation is not offered")
    void deniedWhenSharedAccommodationDisabled() {
        Event event = accommodationEvent().withSharedAccommodationEnabled(false).buildPublished();
        EventId eventId = stubEvent(event);
        CurrentUserData caller = caller(COORDINATOR);

        assertThatThrownBy(() -> service.getAccommodationList(eventId, caller)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("registration without member data yields a row with null member data")
    void missingMemberDataYieldsNullMemberData() {
        MemberId unknown = new MemberId(UUID.randomUUID());
        Event event = accommodationEvent().addRegistrations(List.of(registration(unknown, true))).buildPublished();
        EventId eventId = stubEvent(event);
        when(members.findAccommodationDataByIds(any())).thenReturn(Map.of());
        CurrentUserData caller = caller(COORDINATOR);

        AccommodationList result = service.getAccommodationList(eventId, caller);

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().getFirst().memberData()).isNull();
    }

    @Test
    @DisplayName("unknown event is reported as not found")
    void unknownEventIsNotFound() {
        EventId eventId = EventId.generate();
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());
        CurrentUserData caller = caller(COORDINATOR);

        assertThatThrownBy(() -> service.getAccommodationList(eventId, caller)).isInstanceOf(EventNotFoundException.class);
    }

    @Test
    @DisplayName("draft event is reported as not found")
    void draftEventIsNotFound() {
        Event event = accommodationEvent().build();
        EventId eventId = stubEvent(event);
        CurrentUserData caller = caller(COORDINATOR);

        assertThatThrownBy(() -> service.getAccommodationList(eventId, caller)).isInstanceOf(EventNotFoundException.class);
    }
}
