package com.klabis.calendar.domain;

import com.klabis.calendar.CalendarItemAssert;
import com.klabis.calendar.CalendarItemId;
import com.klabis.events.EventId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import static com.klabis.calendar.domain.CalendarItemCreateCalendarItemBuilder.builder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CalendarItem Aggregate")
class CalendarItemTest {

    @Nested
    @DisplayName("ManualCalendarItem.create() factory method")
    class CreateMethod {

        @Test
        @DisplayName("should create manual calendar item with all fields")
        void shouldCreateManualCalendarItemWithAllFields() {
            String name = "Club Training Session";
            String description = "Weekly orienteering training at City Park";
            LocalDate startDate = LocalDate.of(2026, 6, 15);
            LocalDate endDate = LocalDate.of(2026, 6, 15);

            CalendarItem calendarItem = ManualCalendarItem.create(builder()
                    .name(name)
                    .description(description)
                    .startDate(startDate)
                    .endDate(endDate)
                    .build());

            CalendarItemAssert.assertThat(calendarItem)
                    .hasIdNotNull()
                    .hasName(name)
                    .hasDescription(description)
                    .hasStartDate(startDate)
                    .hasEndDate(endDate)
                    .hasEventId(null)
                    .isManual();
        }

        @Test
        @DisplayName("should create multi-day calendar item")
        void shouldCreateMultiDayCalendarItem() {
            LocalDate startDate = LocalDate.of(2026, 7, 1);
            LocalDate endDate = LocalDate.of(2026, 7, 5);

            CalendarItem calendarItem = ManualCalendarItem.create(builder()
                    .name("Summer Camp")
                    .description("5-day orienteering summer camp")
                    .startDate(startDate)
                    .endDate(endDate)
                    .build());

            CalendarItemAssert.assertThat(calendarItem)
                    .hasStartDate(startDate)
                    .hasEndDate(endDate)
                    .isManual();
        }

        @Test
        @DisplayName("should create calendar item with same start and end date")
        void shouldCreateCalendarItemWithSameStartAndEndDate() {
            LocalDate sameDate = LocalDate.of(2026, 6, 15);

            CalendarItem calendarItem = ManualCalendarItem.create(builder()
                    .name("Single Day Event")
                    .description("One day activity")
                    .startDate(sameDate)
                    .endDate(sameDate)
                    .build());

            CalendarItemAssert.assertThat(calendarItem)
                    .hasStartDate(sameDate)
                    .hasEndDate(sameDate);
        }

        @Test
        @DisplayName("should fail when name is null")
        void shouldFailWhenNameIsNull() {
            LocalDate date = LocalDate.of(2026, 6, 15);

            assertThatThrownBy(() -> ManualCalendarItem.create(builder()
                    .name(null)
                    .description("Description")
                    .startDate(date)
                    .endDate(date)
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail when name is blank")
        void shouldFailWhenNameIsBlank() {
            LocalDate date = LocalDate.of(2026, 6, 15);

            assertThatThrownBy(() -> ManualCalendarItem.create(builder()
                    .name("   ")
                    .description("Description")
                    .startDate(date)
                    .endDate(date)
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should create calendar item with null description")
        void shouldCreateCalendarItemWithNullDescription() {
            LocalDate date = LocalDate.of(2026, 6, 15);

            CalendarItem calendarItem = ManualCalendarItem.create(builder()
                    .name("Klubová schůze")
                    .description(null)
                    .startDate(date)
                    .endDate(date)
                    .build());

            CalendarItemAssert.assertThat(calendarItem)
                    .hasIdNotNull()
                    .hasName("Klubová schůze")
                    .hasDescription(null)
                    .isManual();
        }

        @Test
        @DisplayName("should still reject missing name")
        void shouldStillRejectMissingName() {
            LocalDate date = LocalDate.of(2026, 6, 15);

            assertThatThrownBy(() -> ManualCalendarItem.create(builder()
                    .name(null)
                    .description(null)
                    .startDate(date)
                    .endDate(date)
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should still reject missing startDate")
        void shouldStillRejectMissingStartDate() {
            assertThatThrownBy(() -> ManualCalendarItem.create(builder()
                    .name("Name")
                    .description(null)
                    .startDate(null)
                    .endDate(LocalDate.of(2026, 6, 15))
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Start date");
        }

        @Test
        @DisplayName("should still reject missing endDate")
        void shouldStillRejectMissingEndDate() {
            assertThatThrownBy(() -> ManualCalendarItem.create(builder()
                    .name("Name")
                    .description(null)
                    .startDate(LocalDate.of(2026, 6, 15))
                    .endDate(null)
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("End date");
        }

        @Test
        @DisplayName("should still reject endDate before startDate")
        void shouldStillRejectEndDateBeforeStartDate() {
            assertThatThrownBy(() -> ManualCalendarItem.create(builder()
                    .name("Name")
                    .description(null)
                    .startDate(LocalDate.of(2026, 6, 15))
                    .endDate(LocalDate.of(2026, 6, 10))
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("End date must be on or after start date");
        }

        @Test
        @DisplayName("should fail when startDate is null")
        void shouldFailWhenStartDateIsNull() {
            assertThatThrownBy(() -> ManualCalendarItem.create(builder()
                    .name("Name")
                    .description("Description")
                    .startDate(null)
                    .endDate(LocalDate.of(2026, 6, 15))
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Start date");
        }

        @Test
        @DisplayName("should fail when endDate is null")
        void shouldFailWhenEndDateIsNull() {
            assertThatThrownBy(() -> ManualCalendarItem.create(builder()
                    .name("Name")
                    .description("Description")
                    .startDate(LocalDate.of(2026, 6, 15))
                    .endDate(null)
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("End date");
        }

        @Test
        @DisplayName("should fail when endDate is before startDate")
        void shouldFailWhenEndDateIsBeforeStartDate() {
            assertThatThrownBy(() -> ManualCalendarItem.create(builder()
                    .name("Name")
                    .description("Description")
                    .startDate(LocalDate.of(2026, 6, 15))
                    .endDate(LocalDate.of(2026, 6, 10))
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("End date must be on or after start date");
        }
    }

    @Nested
    @DisplayName("ManualCalendarItem.update() method")
    class UpdateMethod {

        @Test
        @DisplayName("should update manual calendar item with valid data")
        void shouldUpdateManualCalendarItemWithValidData() {
            ManualCalendarItem calendarItem = ManualCalendarItem.create(builder()
                    .name("Original Name")
                    .description("Original Description")
                    .startDate(LocalDate.of(2026, 6, 15))
                    .endDate(LocalDate.of(2026, 6, 15))
                    .build());

            String newName = "Updated Name";
            String newDescription = "Updated Description";
            LocalDate newStartDate = LocalDate.of(2026, 7, 1);
            LocalDate newEndDate = LocalDate.of(2026, 7, 5);

            calendarItem.update(CalendarItemUpdateCalendarItemBuilder.builder()
                    .name(newName)
                    .description(newDescription)
                    .startDate(newStartDate)
                    .endDate(newEndDate)
                    .build());

            CalendarItemAssert.assertThat(calendarItem)
                    .hasName(newName)
                    .hasDescription(newDescription)
                    .hasStartDate(newStartDate)
                    .hasEndDate(newEndDate)
                    .isManual();
        }

        @Test
        @DisplayName("should update manual calendar item to single day")
        void shouldUpdateManualCalendarItemToSingleDay() {
            ManualCalendarItem calendarItem = ManualCalendarItem.create(builder()
                    .name("Multi-day Event")
                    .description("Description")
                    .startDate(LocalDate.of(2026, 6, 1))
                    .endDate(LocalDate.of(2026, 6, 5))
                    .build());

            LocalDate singleDate = LocalDate.of(2026, 6, 15);

            calendarItem.update(CalendarItemUpdateCalendarItemBuilder.builder()
                    .name("Single Day")
                    .description("Updated")
                    .startDate(singleDate)
                    .endDate(singleDate)
                    .build());

            CalendarItemAssert.assertThat(calendarItem)
                    .hasStartDate(singleDate)
                    .hasEndDate(singleDate);
        }

        @Test
        @DisplayName("should fail to update event-linked calendar item")
        void shouldFailToUpdateEventCalendarItem() {
            EventId eventId = EventId.of(UUID.randomUUID());
            EventCalendarItem calendarItem = EventCalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "Event-linked Item",
                    "Synchronized from event",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15),
                    eventId,
                    null
            );

            // EventCalendarItem has no update() method — verified at compile time.
            // Calling assertCanBeDeleted() tests the read-only guard.
            assertThatThrownBy(calendarItem::assertCanBeDeleted)
                    .isInstanceOf(CalendarItemReadOnlyException.class)
                    .hasMessageContaining("Cannot manually modify event-linked calendar item");
        }

        @Test
        @DisplayName("should fail to update with null name")
        void shouldFailToUpdateWithNullName() {
            ManualCalendarItem calendarItem = ManualCalendarItem.create(builder()
                    .name("Original Name")
                    .description("Description")
                    .startDate(LocalDate.of(2026, 6, 15))
                    .endDate(LocalDate.of(2026, 6, 15))
                    .build());

            assertThatThrownBy(() -> calendarItem.update(CalendarItemUpdateCalendarItemBuilder.builder()
                    .name(null)
                    .description("Description")
                    .startDate(LocalDate.of(2026, 7, 1))
                    .endDate(LocalDate.of(2026, 7, 1))
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail to update with blank name")
        void shouldFailToUpdateWithBlankName() {
            ManualCalendarItem calendarItem = ManualCalendarItem.create(builder()
                    .name("Original Name")
                    .description("Description")
                    .startDate(LocalDate.of(2026, 6, 15))
                    .endDate(LocalDate.of(2026, 6, 15))
                    .build());

            assertThatThrownBy(() -> calendarItem.update(CalendarItemUpdateCalendarItemBuilder.builder()
                    .name("   ")
                    .description("Description")
                    .startDate(LocalDate.of(2026, 7, 1))
                    .endDate(LocalDate.of(2026, 7, 1))
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should update manual calendar item with null description")
        void shouldUpdateManualCalendarItemWithNullDescription() {
            ManualCalendarItem calendarItem = ManualCalendarItem.create(builder()
                    .name("Name")
                    .description("Original Description")
                    .startDate(LocalDate.of(2026, 6, 15))
                    .endDate(LocalDate.of(2026, 6, 15))
                    .build());

            calendarItem.update(CalendarItemUpdateCalendarItemBuilder.builder()
                    .name("Name")
                    .description(null)
                    .startDate(LocalDate.of(2026, 7, 1))
                    .endDate(LocalDate.of(2026, 7, 1))
                    .build());

            CalendarItemAssert.assertThat(calendarItem)
                    .hasDescription(null);
        }

        @Test
        @DisplayName("should clear existing description when update command has null description")
        void shouldClearExistingDescriptionWhenUpdateCommandHasNullDescription() {
            ManualCalendarItem calendarItem = ManualCalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "Name",
                    "Existing description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15),
                    null
            );

            calendarItem.update(CalendarItemUpdateCalendarItemBuilder.builder()
                    .name("Name")
                    .description(null)
                    .startDate(LocalDate.of(2026, 6, 15))
                    .endDate(LocalDate.of(2026, 6, 15))
                    .build());

            CalendarItemAssert.assertThat(calendarItem)
                    .hasDescription(null);
        }

        @Test
        @DisplayName("should still throw CalendarItemReadOnlyException for event-linked items")
        void shouldStillThrowForEventLinkedItems() {
            com.klabis.events.EventId eventId = com.klabis.events.EventId.of(UUID.randomUUID());
            EventCalendarItem calendarItem = EventCalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "Event-linked Item",
                    null,
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15),
                    eventId,
                    null
            );

            assertThatThrownBy(calendarItem::assertCanBeDeleted)
                    .isInstanceOf(CalendarItemReadOnlyException.class);
        }

        @Test
        @DisplayName("should fail to update with null startDate")
        void shouldFailToUpdateWithNullStartDate() {
            ManualCalendarItem calendarItem = ManualCalendarItem.create(builder()
                    .name("Name")
                    .description("Description")
                    .startDate(LocalDate.of(2026, 6, 15))
                    .endDate(LocalDate.of(2026, 6, 15))
                    .build());

            assertThatThrownBy(() -> calendarItem.update(CalendarItemUpdateCalendarItemBuilder.builder()
                    .name("Name")
                    .description("Description")
                    .startDate(null)
                    .endDate(LocalDate.of(2026, 7, 1))
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Start date");
        }

        @Test
        @DisplayName("should fail to update with null endDate")
        void shouldFailToUpdateWithNullEndDate() {
            ManualCalendarItem calendarItem = ManualCalendarItem.create(builder()
                    .name("Name")
                    .description("Description")
                    .startDate(LocalDate.of(2026, 6, 15))
                    .endDate(LocalDate.of(2026, 6, 15))
                    .build());

            assertThatThrownBy(() -> calendarItem.update(CalendarItemUpdateCalendarItemBuilder.builder()
                    .name("Name")
                    .description("Description")
                    .startDate(LocalDate.of(2026, 7, 1))
                    .endDate(null)
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("End date");
        }

        @Test
        @DisplayName("should fail to update with endDate before startDate")
        void shouldFailToUpdateWithEndDateBeforeStartDate() {
            ManualCalendarItem calendarItem = ManualCalendarItem.create(builder()
                    .name("Name")
                    .description("Description")
                    .startDate(LocalDate.of(2026, 6, 15))
                    .endDate(LocalDate.of(2026, 6, 15))
                    .build());

            assertThatThrownBy(() -> calendarItem.update(CalendarItemUpdateCalendarItemBuilder.builder()
                    .name("Name")
                    .description("Description")
                    .startDate(LocalDate.of(2026, 7, 10))
                    .endDate(LocalDate.of(2026, 7, 5))
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("End date must be on or after start date");
        }
    }

    @Nested
    @DisplayName("EventCalendarItem.synchronizeFromEvent() method")
    class SynchronizeFromEventMethod {

        @Test
        @DisplayName("should synchronize event-linked calendar item with updated event data")
        void shouldSynchronizeEventCalendarItem() {
            EventId eventId = EventId.of(UUID.randomUUID());
            EventCalendarItem calendarItem = EventCalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "Old Name",
                    "Old Description",
                    LocalDate.of(2026, 5, 10),
                    LocalDate.of(2026, 5, 10),
                    eventId,
                    null
            );

            LocalDate newDate = LocalDate.of(2026, 7, 20);

            calendarItem.synchronizeFromEvent(EventCalendarItemSynchronizeFromEventBuilder.builder()
                    .name("New Name")
                    .location("City Park")
                    .organizer("OOB")
                    .websiteUrl(null)
                    .eventDate(newDate)
                    .build());

            CalendarItemAssert.assertThat(calendarItem)
                    .hasName("New Name")
                    .hasDescription("City Park - OOB")
                    .hasStartDate(newDate)
                    .hasEndDate(newDate)
                    .isEventLinked();
        }

        @Test
        @DisplayName("should synchronize with website URL appended to description")
        void shouldSynchronizeWithWebsiteUrl() {
            EventId eventId = EventId.of(UUID.randomUUID());
            EventCalendarItem calendarItem = EventCalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "Old Name",
                    "Old Description",
                    LocalDate.of(2026, 5, 10),
                    LocalDate.of(2026, 5, 10),
                    eventId,
                    null
            );

            calendarItem.synchronizeFromEvent(EventCalendarItemSynchronizeFromEventBuilder.builder()
                    .name("New Name")
                    .location("City Park")
                    .organizer("OOB")
                    .websiteUrl("https://example.com")
                    .eventDate(LocalDate.of(2026, 7, 20))
                    .build());

            CalendarItemAssert.assertThat(calendarItem)
                    .hasDescription("City Park - OOB\nhttps://example.com");
        }

        @Test
        @DisplayName("should fail synchronization when name is blank")
        void shouldFailSynchronizationWhenNameIsBlank() {
            EventId eventId = EventId.of(UUID.randomUUID());
            EventCalendarItem calendarItem = EventCalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "Old Name",
                    "Old Description",
                    LocalDate.of(2026, 5, 10),
                    LocalDate.of(2026, 5, 10),
                    eventId,
                    null
            );

            assertThatThrownBy(() -> calendarItem.synchronizeFromEvent(
                    EventCalendarItemSynchronizeFromEventBuilder.builder()
                            .name("   ")
                            .location("Location")
                            .organizer("OOB")
                            .websiteUrl(null)
                            .eventDate(LocalDate.of(2026, 7, 20))
                            .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should synchronize with null location — description contains organizer only")
        void shouldSynchronizeWithNullLocation() {
            EventId eventId = EventId.of(UUID.randomUUID());
            EventCalendarItem calendarItem = EventCalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "Old Name",
                    "Old Description",
                    LocalDate.of(2026, 5, 10),
                    LocalDate.of(2026, 5, 10),
                    eventId,
                    null
            );

            calendarItem.synchronizeFromEvent(EventCalendarItemSynchronizeFromEventBuilder.builder()
                    .name("New Name")
                    .location(null)
                    .organizer("OOB")
                    .websiteUrl(null)
                    .eventDate(LocalDate.of(2026, 7, 20))
                    .build());

            CalendarItemAssert.assertThat(calendarItem)
                    .hasDescription("OOB");
        }

        @Test
        @DisplayName("should synchronize with blank location — treated as null, description contains organizer only")
        void shouldSynchronizeWithBlankLocation() {
            EventId eventId = EventId.of(UUID.randomUUID());
            EventCalendarItem calendarItem = EventCalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "Old Name",
                    "Old Description",
                    LocalDate.of(2026, 5, 10),
                    LocalDate.of(2026, 5, 10),
                    eventId,
                    null
            );

            calendarItem.synchronizeFromEvent(EventCalendarItemSynchronizeFromEventBuilder.builder()
                    .name("New Name")
                    .location("   ")
                    .organizer("OOB")
                    .websiteUrl(null)
                    .eventDate(LocalDate.of(2026, 7, 20))
                    .build());

            CalendarItemAssert.assertThat(calendarItem)
                    .hasDescription("OOB");
        }

        @Test
        @DisplayName("should fail synchronization when eventDate is null")
        void shouldFailSynchronizationWhenEventDateIsNull() {
            EventId eventId = EventId.of(UUID.randomUUID());
            EventCalendarItem calendarItem = EventCalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "Old Name",
                    "Old Description",
                    LocalDate.of(2026, 5, 10),
                    LocalDate.of(2026, 5, 10),
                    eventId,
                    null
            );

            assertThatThrownBy(() -> calendarItem.synchronizeFromEvent(
                    EventCalendarItemSynchronizeFromEventBuilder.builder()
                            .name("Name")
                            .location("Location")
                            .organizer("OOB")
                            .websiteUrl(null)
                            .eventDate(null)
                            .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Start date");
        }
    }

    @Nested
    @DisplayName("assertCanBeDeleted()")
    class AssertCanBeDeletedMethod {

        @Test
        @DisplayName("should not throw for manual calendar item")
        void shouldNotThrowForManualCalendarItem() {
            ManualCalendarItem calendarItem = ManualCalendarItem.create(builder()
                    .name("Manual Item")
                    .description("Created manually")
                    .startDate(LocalDate.of(2026, 6, 15))
                    .endDate(LocalDate.of(2026, 6, 15))
                    .build());

            calendarItem.assertCanBeDeleted();
        }

        @Test
        @DisplayName("should throw CalendarItemReadOnlyException for event-linked calendar item")
        void shouldThrowForEventCalendarItem() {
            EventId eventId = EventId.of(UUID.randomUUID());
            EventCalendarItem calendarItem = EventCalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "Event-linked Item",
                    "Synchronized from event",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15),
                    eventId,
                    null
            );

            assertThatThrownBy(calendarItem::assertCanBeDeleted)
                    .isInstanceOf(CalendarItemReadOnlyException.class);
        }
    }

    @Nested
    @DisplayName("buildEventDescription() — null-safe join")
    class BuildEventDescription {

        private String descriptionFor(String location, String organizer, String websiteUrl) {
            return new EventCalendarItem.CreateCalendarItemForEvent(
                    "Event Name", location, organizer, websiteUrl,
                    LocalDate.of(2026, 6, 15), EventId.of(UUID.randomUUID())
            ).description();
        }

        @Test
        @DisplayName("null location + organizer only → organizer alone")
        void nullLocationOrganizerOnly() {
            assertThat(descriptionFor(null, "PBM", null)).isEqualTo("PBM");
        }

        @Test
        @DisplayName("location + organizer → joined with \" - \"")
        void locationAndOrganizerJoined() {
            assertThat(descriptionFor("Senomaty", "PBM", null)).isEqualTo("Senomaty - PBM");
        }

        @Test
        @DisplayName("location + organizer + websiteUrl → joined with newline")
        void locationOrganizerAndWebsiteUrl() {
            assertThat(descriptionFor("Senomaty", "PBM", "https://example")).isEqualTo("Senomaty - PBM\nhttps://example");
        }

        @Test
        @DisplayName("null location + null organizer + websiteUrl → just the URL")
        void nullLocationNullOrganizerWithUrl() {
            assertThat(descriptionFor(null, null, "https://example")).isEqualTo("https://example");
        }

        @Test
        @DisplayName("all null → null (not empty string)")
        void allNullReturnsNull() {
            assertThat(descriptionFor(null, null, null)).isNull();
        }

        @Test
        @DisplayName("blank location + blank organizer → null")
        void blankInputsReturnNull() {
            assertThat(descriptionFor("", "   ", null)).isNull();
        }

        @ParameterizedTest(name = "{3}")
        @MethodSource("cases")
        @DisplayName("parametrized cases")
        void parametrizedCases(String location, String organizer, String websiteUrl, String expectedDescription) {
            assertThat(descriptionFor(location, organizer, websiteUrl)).isEqualTo(expectedDescription);
        }

        static Stream<Arguments> cases() {
            return Stream.of(
                    Arguments.of(null, "PBM", null, "PBM"),
                    Arguments.of("Senomaty", "PBM", null, "Senomaty - PBM"),
                    Arguments.of("Senomaty", "PBM", "https://example", "Senomaty - PBM\nhttps://example"),
                    Arguments.of(null, null, "https://example", "https://example"),
                    Arguments.of(null, null, null, null),
                    Arguments.of("", "   ", null, null)
            );
        }
    }

    @Nested
    @DisplayName("instanceof checks (replacing isEventLinked())")
    class IsEventLinked {

        @Test
        @DisplayName("should identify event-linked calendar item via instanceof")
        void shouldIdentifyEventCalendarItem() {
            EventId eventId = EventId.of(UUID.randomUUID());
            EventCalendarItem calendarItem = EventCalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "Event Item",
                    "From event",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15),
                    eventId,
                    null
            );

            CalendarItemAssert.assertThat(calendarItem)
                    .isEventLinked()
                    .hasEventId(eventId);
        }

        @Test
        @DisplayName("should identify manual calendar item via instanceof")
        void shouldIdentifyManualCalendarItem() {
            ManualCalendarItem calendarItem = ManualCalendarItem.create(builder()
                    .name("Manual Item")
                    .description("Created manually")
                    .startDate(LocalDate.of(2026, 6, 15))
                    .endDate(LocalDate.of(2026, 6, 15))
                    .build());

            CalendarItemAssert.assertThat(calendarItem)
                    .isManual()
                    .hasEventId(null);
        }
    }
}
