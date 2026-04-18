package com.klabis.calendar.domain;

import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.CalendarItemKind;
import com.klabis.events.EventData;
import com.klabis.events.EventId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EventCalendarItem")
class EventCalendarItemTest {

    @Nested
    @DisplayName("createForEventDate()")
    class CreateForEventDateTests {

        @Test
        @DisplayName("should create event-linked item with EVENT_DATE kind and event data")
        void shouldCreateEventLinkedItemWithEventData() {
            EventId eventId = EventId.of(UUID.randomUUID());
            var command = new EventCalendarItem.CreateCalendarItemForEvent(
                    "City Championship", "Prague", "OOB", "https://example.com",
                    LocalDate.of(2026, 6, 15), eventId);

            EventCalendarItem item = EventCalendarItem.createForEventDate(command);

            assertThat(item).isInstanceOf(EventCalendarItem.class);
            assertThat(item.getId()).isNotNull();
            assertThat(item.getName()).isEqualTo("City Championship");
            assertThat(item.getDescription()).isEqualTo("Prague - OOB\nhttps://example.com");
            assertThat(item.getStartDate()).isEqualTo(LocalDate.of(2026, 6, 15));
            assertThat(item.getEndDate()).isEqualTo(LocalDate.of(2026, 6, 15));
            assertThat(item.getEventId()).isEqualTo(eventId);
            assertThat(item.getKind()).isEqualTo(CalendarItemKind.EVENT_DATE);
        }

        @Test
        @DisplayName("should fail when name is null")
        void shouldFailWhenNameIsNull() {
            EventId eventId = EventId.of(UUID.randomUUID());
            var command = new EventCalendarItem.CreateCalendarItemForEvent(
                    null, null, null, null,
                    LocalDate.of(2026, 6, 15), eventId);

            assertThatThrownBy(() -> EventCalendarItem.createForEventDate(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail when eventId is null")
        void shouldFailWhenEventIdIsNull() {
            var command = new EventCalendarItem.CreateCalendarItemForEvent(
                    "Name", null, null, null,
                    LocalDate.of(2026, 6, 15), null);

            assertThatThrownBy(() -> EventCalendarItem.createForEventDate(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Event ID");
        }

        @Test
        @DisplayName("should fail when eventDate is null")
        void shouldFailWhenEventDateIsNull() {
            EventId eventId = EventId.of(UUID.randomUUID());
            var command = new EventCalendarItem.CreateCalendarItemForEvent(
                    "Name", null, null, null, null, eventId);

            assertThatThrownBy(() -> EventCalendarItem.createForEventDate(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Start date");
        }
    }

    @Nested
    @DisplayName("createForRegistrationDeadline()")
    class CreateForRegistrationDeadlineTests {

        @Test
        @DisplayName("should create item with EVENT_REGISTRATION_DATE kind, prefixed name, null description, deadline as startDate and endDate")
        void shouldCreateRegistrationDeadlineItem() {
            EventId eventId = EventId.of(UUID.randomUUID());
            LocalDate deadline = LocalDate.of(2026, 5, 31);

            EventCalendarItem item = EventCalendarItem.createForRegistrationDeadline(
                    "City Championship", eventId, deadline);

            assertThat(item.getId()).isNotNull();
            assertThat(item.getKind()).isEqualTo(CalendarItemKind.EVENT_REGISTRATION_DATE);
            assertThat(item.getName()).isEqualTo("Přihlášky - City Championship");
            assertThat(item.getDescription()).isNull();
            assertThat(item.getStartDate()).isEqualTo(deadline);
            assertThat(item.getEndDate()).isEqualTo(deadline);
            assertThat(item.getEventId()).isEqualTo(eventId);
        }

        @Test
        @DisplayName("should fail when eventName is null")
        void shouldFailWhenEventNameIsNull() {
            EventId eventId = EventId.of(UUID.randomUUID());
            LocalDate deadline = LocalDate.of(2026, 5, 31);

            assertThatThrownBy(() -> EventCalendarItem.createForRegistrationDeadline(null, eventId, deadline))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should fail when eventId is null")
        void shouldFailWhenEventIdIsNull() {
            LocalDate deadline = LocalDate.of(2026, 5, 31);

            assertThatThrownBy(() -> EventCalendarItem.createForRegistrationDeadline("City Championship", null, deadline))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should fail when deadline is null")
        void shouldFailWhenDeadlineIsNull() {
            EventId eventId = EventId.of(UUID.randomUUID());

            assertThatThrownBy(() -> EventCalendarItem.createForRegistrationDeadline("City Championship", eventId, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("synchronizeFromEvent(EventData)")
    class SynchronizeFromEventTests {

        @Test
        @DisplayName("should update EVENT_DATE item from event fields")
        void shouldUpdateEventDateItemFromEventFields() {
            EventId eventId = EventId.of(UUID.randomUUID());
            EventCalendarItem item = EventCalendarItem.createForEventDate(
                    new EventCalendarItem.CreateCalendarItemForEvent(
                            "Old", "OldLoc", "OldOrg", null,
                            LocalDate.of(2026, 5, 10), eventId));

            EventData event = new EventData(
                    "New Name", LocalDate.of(2026, 7, 20),
                    "New Loc", "New Org", "https://new.com",
                    LocalDate.of(2026, 7, 1));

            item.synchronizeFromEvent(event);

            assertThat(item.getName()).isEqualTo("New Name");
            assertThat(item.getDescription()).isEqualTo("New Loc - New Org\nhttps://new.com");
            assertThat(item.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 20));
            assertThat(item.getEndDate()).isEqualTo(LocalDate.of(2026, 7, 20));
            assertThat(item.getEventId()).isEqualTo(eventId);
        }

        @Test
        @DisplayName("should update EVENT_REGISTRATION_DATE item: prefixed name, null description, deadline date")
        void shouldUpdateRegistrationDeadlineItemFromEventFields() {
            EventId eventId = EventId.of(UUID.randomUUID());
            EventCalendarItem item = EventCalendarItem.createForRegistrationDeadline(
                    "Old Event", eventId, LocalDate.of(2026, 5, 10));

            EventData event = new EventData(
                    "New Name", LocalDate.of(2026, 7, 20),
                    "New Loc", "New Org", "https://new.com",
                    LocalDate.of(2026, 6, 30));

            item.synchronizeFromEvent(event);

            assertThat(item.getName()).isEqualTo("Přihlášky - New Name");
            assertThat(item.getDescription()).isNull();
            assertThat(item.getStartDate()).isEqualTo(LocalDate.of(2026, 6, 30));
            assertThat(item.getEndDate()).isEqualTo(LocalDate.of(2026, 6, 30));
            assertThat(item.getEventId()).isEqualTo(eventId);
        }

        @Test
        @DisplayName("should fail when EVENT_DATE item receives blank name during sync")
        void shouldFailWhenNameIsBlankDuringSync() {
            EventId eventId = EventId.of(UUID.randomUUID());
            EventCalendarItem item = EventCalendarItem.createForEventDate(
                    new EventCalendarItem.CreateCalendarItemForEvent(
                            "Old", null, null, null,
                            LocalDate.of(2026, 5, 10), eventId));

            EventData event = new EventData(
                    "   ", LocalDate.of(2026, 7, 20),
                    null, null, null, null);

            assertThatThrownBy(() -> item.synchronizeFromEvent(event))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail when EVENT_REGISTRATION_DATE item receives null registrationDeadline during sync")
        void shouldFailWhenRegistrationDeadlineIsNullDuringSync() {
            EventId eventId = EventId.of(UUID.randomUUID());
            EventCalendarItem item = EventCalendarItem.createForRegistrationDeadline(
                    "Old Event", eventId, LocalDate.of(2026, 5, 10));

            EventData event = new EventData(
                    "New Name", LocalDate.of(2026, 7, 20),
                    null, null, null, null);

            assertThatThrownBy(() -> item.synchronizeFromEvent(event))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("registrationDeadline");
        }
    }

    @Nested
    @DisplayName("assertCanBeDeleted()")
    class AssertCanBeDeletedTests {

        @Test
        @DisplayName("should throw CalendarItemReadOnlyException — event-linked items are read-only")
        void shouldThrowCalendarItemReadOnlyException() {
            EventId eventId = EventId.of(UUID.randomUUID());
            EventCalendarItem item = EventCalendarItem.createForEventDate(
                    new EventCalendarItem.CreateCalendarItemForEvent(
                            "Name", null, null, null,
                            LocalDate.of(2026, 6, 15), eventId));

            assertThatThrownBy(item::assertCanBeDeleted)
                    .isInstanceOf(CalendarItemReadOnlyException.class);
        }
    }

    @Nested
    @DisplayName("reconstruct()")
    class ReconstructTests {

        @Test
        @DisplayName("should reconstruct EVENT_DATE item from persistence without validation")
        void shouldReconstructEventDateItemFromPersistence() {
            CalendarItemId id = CalendarItemId.generate();
            EventId eventId = EventId.of(UUID.randomUUID());

            EventCalendarItem item = EventCalendarItem.reconstruct(
                    id, "Name", "desc",
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1),
                    eventId, CalendarItemKind.EVENT_DATE, null);

            assertThat(item.getId()).isEqualTo(id);
            assertThat(item.getEventId()).isEqualTo(eventId);
            assertThat(item.getKind()).isEqualTo(CalendarItemKind.EVENT_DATE);
        }

        @Test
        @DisplayName("should reconstruct EVENT_REGISTRATION_DATE item from persistence")
        void shouldReconstructRegistrationDeadlineItemFromPersistence() {
            CalendarItemId id = CalendarItemId.generate();
            EventId eventId = EventId.of(UUID.randomUUID());

            EventCalendarItem item = EventCalendarItem.reconstruct(
                    id, "Přihlášky - Race", null,
                    LocalDate.of(2026, 5, 31), LocalDate.of(2026, 5, 31),
                    eventId, CalendarItemKind.EVENT_REGISTRATION_DATE, null);

            assertThat(item.getId()).isEqualTo(id);
            assertThat(item.getEventId()).isEqualTo(eventId);
            assertThat(item.getKind()).isEqualTo(CalendarItemKind.EVENT_REGISTRATION_DATE);
            assertThat(item.getDescription()).isNull();
        }
    }
}
