package com.klabis.events.application;

import com.klabis.events.EventId;
import com.klabis.events.EventTestDataBuilder;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.SiCardNumber;
import com.klabis.members.MemberAccommodationDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccommodationListService Unit Tests")
class AccommodationListServiceTest {

    @Mock
    private EventManagementPort eventManagement;

    @Mock
    private Members members;

    private AccommodationListPort service;

    @BeforeEach
    void setUp() {
        service = new AccommodationListService(eventManagement, members);
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
        when(eventManagement.getEvent(event.getId(), false)).thenReturn(event);
        return event.getId();
    }

    private static EventTestDataBuilder accommodationEvent() {
        return EventTestDataBuilder.anEvent().withSharedAccommodationEnabled(true);
    }

    @Test
    @DisplayName("returns rows only for registrations wanting shared accommodation, with member data")
    void returnsFilteredRows() {
        MemberId wantsIt = new MemberId(UUID.randomUUID());
        MemberId doesNot = new MemberId(UUID.randomUUID());
        Event event = accommodationEvent()
                .withName("Camp")
                .addRegistrations(List.of(registration(wantsIt, true), registration(doesNot, false)))
                .buildPublished();
        EventId eventId = stubEvent(event);
        MemberAccommodationDto data = memberData("Wants");
        when(members.findAccommodationDataByIds(any())).thenReturn(Map.of(wantsIt, data));

        AccommodationList result = service.getAccommodationList(eventId);

        assertThat(result.eventId()).isEqualTo(eventId.value());
        assertThat(result.eventName()).isEqualTo("Camp");
        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().getFirst().eventId()).isEqualTo(eventId.value());
        assertThat(result.rows().getFirst().registration().memberId()).isEqualTo(wantsIt);
        assertThat(result.rows().getFirst().memberData()).isEqualTo(data);
    }

    @Test
    @DisplayName("registration without member data yields a row with null member data")
    void missingMemberDataYieldsNullMemberData() {
        MemberId unknown = new MemberId(UUID.randomUUID());
        Event event = accommodationEvent().addRegistrations(List.of(registration(unknown, true))).buildPublished();
        EventId eventId = stubEvent(event);
        when(members.findAccommodationDataByIds(any())).thenReturn(Map.of());

        AccommodationList result = service.getAccommodationList(eventId);

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().getFirst().memberData()).isNull();
    }

    @Test
    @DisplayName("propagates EventNotFoundException from the event management port")
    void propagatesEventNotFound() {
        EventId eventId = EventId.generate();
        when(eventManagement.getEvent(eventId, false)).thenThrow(new EventNotFoundException(eventId));

        assertThatThrownBy(() -> service.getAccommodationList(eventId)).isInstanceOf(EventNotFoundException.class);
        verifyNoInteractions(members);
    }
}
