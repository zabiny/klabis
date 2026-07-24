package com.klabis.calendar.application;

import com.klabis.events.EventId;
import com.klabis.events.application.EventScheduleQuery;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventStatus;
import com.klabis.events.domain.Events;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("IcalFeedService")
@ExtendWith(MockitoExtension.class)
class IcalFeedServiceTest {

    @Mock
    private EventScheduleQuery eventScheduleQuery;

    @Mock
    private Events events;

    private IcalWindowProperties windowProperties;
    private IcalFeedService service;

    private static final LocalDate NOW = LocalDate.of(2026, 5, 21);
    private static final MemberId MEMBER_ID = new MemberId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        windowProperties = new IcalWindowProperties();
        windowProperties.setPast(Period.ofDays(30));
        windowProperties.setFuture(Period.ofMonths(12));
        service = new IcalFeedService(eventScheduleQuery, events, windowProperties);
    }

    private Event eventWithCoordinator(EventId id, MemberId coordinatorId) {
        LinkedHashSet<MemberId> coordinators = new LinkedHashSet<>();
        if (coordinatorId != null) coordinators.add(coordinatorId);
        return Event.reconstruct(
                id,
                "Test Event",
                NOW.plusDays(10),
                "Praha",
                "ORG",
                null,
                coordinators,
                null,
                null,
                EventStatus.ACTIVE,
                null,
                null,
                List.of(),
                null,
                null,
                List.of(),
                null
        );
    }

    @Nested
    @DisplayName("getMySchedule()")
    class GetMyScheduleTests {

        @Test
        @DisplayName("should return empty list when member has no registrations or coordinator roles")
        void shouldReturnEmptyListWhenMemberHasNoInvolvement() {
            when(eventScheduleQuery.findEventIdsForMemberSchedule(eq(MEMBER_ID), any(), any()))
                    .thenReturn(Set.of());

            List<IcalFeedPort.EventScheduleEntry> result = service.getMySchedule(MEMBER_ID, NOW);

            assertThat(result).isEmpty();
            verifyNoInteractions(events);
        }

        @Test
        @DisplayName("should return entries as participant when member is registered but not coordinator")
        void shouldReturnParticipantEntriesWhenRegistered() {
            EventId eventId = EventId.generate();
            Event event = eventWithCoordinator(eventId, new MemberId(UUID.randomUUID()));

            when(eventScheduleQuery.findEventIdsForMemberSchedule(eq(MEMBER_ID), any(), any()))
                    .thenReturn(Set.of(eventId));
            when(events.findById(eventId)).thenReturn(Optional.of(event));

            List<IcalFeedPort.EventScheduleEntry> result = service.getMySchedule(MEMBER_ID, NOW);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).event()).isSameAs(event);
            assertThat(result.get(0).isCoordinator()).isFalse();
        }

        @Test
        @DisplayName("should return entry as coordinator when member is the event coordinator")
        void shouldReturnCoordinatorEntryWhenMemberIsCoordinator() {
            EventId eventId = EventId.generate();
            Event event = eventWithCoordinator(eventId, MEMBER_ID);

            when(eventScheduleQuery.findEventIdsForMemberSchedule(eq(MEMBER_ID), any(), any()))
                    .thenReturn(Set.of(eventId));
            when(events.findById(eventId)).thenReturn(Optional.of(event));

            List<IcalFeedPort.EventScheduleEntry> result = service.getMySchedule(MEMBER_ID, NOW);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).event()).isSameAs(event);
            assertThat(result.get(0).isCoordinator()).isTrue();
        }

        @Test
        @DisplayName("should return both registration and coordinator entries without duplication")
        void shouldHandleBothRolesWithoutDuplication() {
            EventId participantEventId = EventId.generate();
            EventId coordinatorEventId = EventId.generate();

            Event participantEvent = eventWithCoordinator(participantEventId, new MemberId(UUID.randomUUID()));
            Event coordinatorEvent = eventWithCoordinator(coordinatorEventId, MEMBER_ID);

            when(eventScheduleQuery.findEventIdsForMemberSchedule(eq(MEMBER_ID), any(), any()))
                    .thenReturn(Set.of(participantEventId, coordinatorEventId));
            when(events.findById(participantEventId)).thenReturn(Optional.of(participantEvent));
            when(events.findById(coordinatorEventId)).thenReturn(Optional.of(coordinatorEvent));

            List<IcalFeedPort.EventScheduleEntry> result = service.getMySchedule(MEMBER_ID, NOW);

            assertThat(result).hasSize(2);
            assertThat(result).anySatisfy(e -> {
                assertThat(e.event()).isSameAs(participantEvent);
                assertThat(e.isCoordinator()).isFalse();
            });
            assertThat(result).anySatisfy(e -> {
                assertThat(e.event()).isSameAs(coordinatorEvent);
                assertThat(e.isCoordinator()).isTrue();
            });
        }

        @Test
        @DisplayName("should skip events not found in Events repository")
        void shouldSkipEventsNotFoundInRepository() {
            EventId existingId = EventId.generate();
            EventId missingId = EventId.generate();

            Event existingEvent = eventWithCoordinator(existingId, new MemberId(UUID.randomUUID()));

            when(eventScheduleQuery.findEventIdsForMemberSchedule(eq(MEMBER_ID), any(), any()))
                    .thenReturn(Set.of(existingId, missingId));
            when(events.findById(existingId)).thenReturn(Optional.of(existingEvent));
            when(events.findById(missingId)).thenReturn(Optional.empty());

            List<IcalFeedPort.EventScheduleEntry> result = service.getMySchedule(MEMBER_ID, NOW);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).event()).isSameAs(existingEvent);
        }

        @Test
        @DisplayName("should use configured window past/future offsets from now")
        void shouldUseConfiguredWindowOffsets() {
            when(eventScheduleQuery.findEventIdsForMemberSchedule(
                    eq(MEMBER_ID),
                    eq(NOW.minus(Period.ofDays(30))),
                    eq(NOW.plus(Period.ofMonths(12)))
            )).thenReturn(Set.of());

            service.getMySchedule(MEMBER_ID, NOW);

            // Assertion is implicit: Mockito verifies the exact date arguments via the stubbing above
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when memberId is null")
        void shouldThrowWhenMemberIdIsNull() {
            assertThatThrownBy(() -> service.getMySchedule(null, NOW))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when now is null")
        void shouldThrowWhenNowIsNull() {
            assertThatThrownBy(() -> service.getMySchedule(MEMBER_ID, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
