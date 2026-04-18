package com.klabis.calendar.domain;

import com.klabis.calendar.CalendarItemId;
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
    @DisplayName("createForEvent()")
    class CreateForEventTests {

        @Test
        @DisplayName("should create event-linked item with event data")
        void shouldCreateEventLinkedItemWithEventData() {
            EventId eventId = EventId.of(UUID.randomUUID());
            var command = new EventCalendarItem.CreateCalendarItemForEvent(
                    "City Championship", "Prague", "OOB", "https://example.com",
                    LocalDate.of(2026, 6, 15), eventId);

            EventCalendarItem item = EventCalendarItem.createForEvent(command);

            assertThat(item).isInstanceOf(EventCalendarItem.class);
            assertThat(item.getId()).isNotNull();
            assertThat(item.getName()).isEqualTo("City Championship");
            assertThat(item.getDescription()).isEqualTo("Prague - OOB\nhttps://example.com");
            assertThat(item.getStartDate()).isEqualTo(LocalDate.of(2026, 6, 15));
            assertThat(item.getEndDate()).isEqualTo(LocalDate.of(2026, 6, 15));
            assertThat(item.getEventId()).isEqualTo(eventId);
        }

        @Test
        @DisplayName("should fail when name is null")
        void shouldFailWhenNameIsNull() {
            EventId eventId = EventId.of(UUID.randomUUID());
            var command = new EventCalendarItem.CreateCalendarItemForEvent(
                    null, null, null, null,
                    LocalDate.of(2026, 6, 15), eventId);

            assertThatThrownBy(() -> EventCalendarItem.createForEvent(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail when eventId is null")
        void shouldFailWhenEventIdIsNull() {
            var command = new EventCalendarItem.CreateCalendarItemForEvent(
                    "Name", null, null, null,
                    LocalDate.of(2026, 6, 15), null);

            assertThatThrownBy(() -> EventCalendarItem.createForEvent(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Event ID");
        }

        @Test
        @DisplayName("should fail when eventDate is null")
        void shouldFailWhenEventDateIsNull() {
            EventId eventId = EventId.of(UUID.randomUUID());
            var command = new EventCalendarItem.CreateCalendarItemForEvent(
                    "Name", null, null, null, null, eventId);

            assertThatThrownBy(() -> EventCalendarItem.createForEvent(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Start date");
        }
    }

    @Nested
    @DisplayName("synchronizeFromEvent()")
    class SynchronizeFromEventTests {

        @Test
        @DisplayName("should mutate fields on synchronization")
        void shouldMutateFieldsOnSynchronization() {
            EventId eventId = EventId.of(UUID.randomUUID());
            EventCalendarItem item = EventCalendarItem.createForEvent(
                    new EventCalendarItem.CreateCalendarItemForEvent(
                            "Old", "OldLoc", "OldOrg", null,
                            LocalDate.of(2026, 5, 10), eventId));

            item.synchronizeFromEvent(new EventCalendarItem.SynchronizeFromEvent(
                    "New Name", "New Loc", "New Org", "https://new.com",
                    LocalDate.of(2026, 7, 20)));

            assertThat(item.getName()).isEqualTo("New Name");
            assertThat(item.getDescription()).isEqualTo("New Loc - New Org\nhttps://new.com");
            assertThat(item.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 20));
            assertThat(item.getEndDate()).isEqualTo(LocalDate.of(2026, 7, 20));
            assertThat(item.getEventId()).isEqualTo(eventId);
        }

        @Test
        @DisplayName("should fail when name is blank during sync")
        void shouldFailWhenNameIsBlankDuringSync() {
            EventId eventId = EventId.of(UUID.randomUUID());
            EventCalendarItem item = EventCalendarItem.createForEvent(
                    new EventCalendarItem.CreateCalendarItemForEvent(
                            "Old", null, null, null,
                            LocalDate.of(2026, 5, 10), eventId));

            assertThatThrownBy(() -> item.synchronizeFromEvent(
                    new EventCalendarItem.SynchronizeFromEvent(
                            "   ", null, null, null,
                            LocalDate.of(2026, 7, 20))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }
    }

    @Nested
    @DisplayName("assertCanBeDeleted()")
    class AssertCanBeDeletedTests {

        @Test
        @DisplayName("should throw CalendarItemReadOnlyException — event-linked items are read-only")
        void shouldThrowCalendarItemReadOnlyException() {
            EventId eventId = EventId.of(UUID.randomUUID());
            EventCalendarItem item = EventCalendarItem.createForEvent(
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
        @DisplayName("should reconstruct from persistence without validation")
        void shouldReconstructFromPersistence() {
            CalendarItemId id = CalendarItemId.generate();
            EventId eventId = EventId.of(UUID.randomUUID());

            EventCalendarItem item = EventCalendarItem.reconstruct(
                    id, "Name", "desc",
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1),
                    eventId, null);

            assertThat(item.getId()).isEqualTo(id);
            assertThat(item.getEventId()).isEqualTo(eventId);
        }
    }
}
