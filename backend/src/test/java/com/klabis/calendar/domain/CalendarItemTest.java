package com.klabis.calendar.domain;

import com.klabis.calendar.CalendarItemAssert;
import com.klabis.calendar.CalendarItemId;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.events.EventId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CalendarItem Aggregate")
class CalendarItemTest {

    @Nested
    @DisplayName("create() factory method")
    class CreateMethod {

        @Test
        @DisplayName("should create manual calendar item with all fields")
        void shouldCreateManualCalendarItemWithAllFields() {
            String name = "Club Training Session";
            String description = "Weekly orienteering training at City Park";
            LocalDate startDate = LocalDate.of(2026, 6, 15);
            LocalDate endDate = LocalDate.of(2026, 6, 15);

            CalendarItem calendarItem = CalendarItem.create(name, description, startDate, endDate);

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
            String name = "Summer Camp";
            String description = "5-day orienteering summer camp";
            LocalDate startDate = LocalDate.of(2026, 7, 1);
            LocalDate endDate = LocalDate.of(2026, 7, 5);

            CalendarItem calendarItem = CalendarItem.create(name, description, startDate, endDate);

            CalendarItemAssert.assertThat(calendarItem)
                    .hasStartDate(startDate)
                    .hasEndDate(endDate)
                    .isManual();
        }

        @Test
        @DisplayName("should create calendar item with same start and end date")
        void shouldCreateCalendarItemWithSameStartAndEndDate() {
            LocalDate sameDate = LocalDate.of(2026, 6, 15);

            CalendarItem calendarItem = CalendarItem.create(
                    "Single Day Event",
                    "One day activity",
                    sameDate,
                    sameDate
            );

            CalendarItemAssert.assertThat(calendarItem)
                    .hasStartDate(sameDate)
                    .hasEndDate(sameDate);
        }

        @Test
        @DisplayName("should fail when name is null")
        void shouldFailWhenNameIsNull() {
            LocalDate startDate = LocalDate.of(2026, 6, 15);
            LocalDate endDate = LocalDate.of(2026, 6, 15);

            assertThatThrownBy(() -> CalendarItem.create(null, "Description", startDate, endDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail when name is blank")
        void shouldFailWhenNameIsBlank() {
            LocalDate startDate = LocalDate.of(2026, 6, 15);
            LocalDate endDate = LocalDate.of(2026, 6, 15);

            assertThatThrownBy(() -> CalendarItem.create("   ", "Description", startDate, endDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail when description is null")
        void shouldFailWhenDescriptionIsNull() {
            LocalDate startDate = LocalDate.of(2026, 6, 15);
            LocalDate endDate = LocalDate.of(2026, 6, 15);

            assertThatThrownBy(() -> CalendarItem.create("Name", null, startDate, endDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description");
        }

        @Test
        @DisplayName("should fail when description is blank")
        void shouldFailWhenDescriptionIsBlank() {
            LocalDate startDate = LocalDate.of(2026, 6, 15);
            LocalDate endDate = LocalDate.of(2026, 6, 15);

            assertThatThrownBy(() -> CalendarItem.create("Name", "   ", startDate, endDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description");
        }

        @Test
        @DisplayName("should fail when startDate is null")
        void shouldFailWhenStartDateIsNull() {
            LocalDate endDate = LocalDate.of(2026, 6, 15);

            assertThatThrownBy(() -> CalendarItem.create("Name", "Description", null, endDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Start date");
        }

        @Test
        @DisplayName("should fail when endDate is null")
        void shouldFailWhenEndDateIsNull() {
            LocalDate startDate = LocalDate.of(2026, 6, 15);

            assertThatThrownBy(() -> CalendarItem.create("Name", "Description", startDate, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("End date");
        }

        @Test
        @DisplayName("should fail when endDate is before startDate")
        void shouldFailWhenEndDateIsBeforeStartDate() {
            LocalDate startDate = LocalDate.of(2026, 6, 15);
            LocalDate endDate = LocalDate.of(2026, 6, 10);

            assertThatThrownBy(() -> CalendarItem.create("Name", "Description", startDate, endDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("End date must be on or after start date");
        }
    }

    @Nested
    @DisplayName("update() method")
    class UpdateMethod {

        @Test
        @DisplayName("should update manual calendar item with valid data")
        void shouldUpdateManualCalendarItemWithValidData() {
            CalendarItem calendarItem = CalendarItem.create(
                    "Original Name",
                    "Original Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );

            String newName = "Updated Name";
            String newDescription = "Updated Description";
            LocalDate newStartDate = LocalDate.of(2026, 7, 1);
            LocalDate newEndDate = LocalDate.of(2026, 7, 5);

            calendarItem.update(newName, newDescription, newStartDate, newEndDate);

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
            CalendarItem calendarItem = CalendarItem.create(
                    "Multi-day Event",
                    "Description",
                    LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 5)
            );

            LocalDate singleDate = LocalDate.of(2026, 6, 15);

            calendarItem.update("Single Day", "Updated", singleDate, singleDate);

            CalendarItemAssert.assertThat(calendarItem)
                    .hasStartDate(singleDate)
                    .hasEndDate(singleDate);
        }

        @Test
        @DisplayName("should fail to update event-linked calendar item")
        void shouldFailToUpdateEventLinkedCalendarItem() {
            EventId eventId = EventId.of(UUID.randomUUID());
            CalendarItem calendarItem = CalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "Event-linked Item",
                    "Synchronized from event",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15),
                    eventId,
                    null
            );

            assertThatThrownBy(() -> calendarItem.update(
                    "New Name",
                    "New Description",
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 7, 1)
            ))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Cannot manually update event-linked calendar item");
        }

        @Test
        @DisplayName("should fail to update with null name")
        void shouldFailToUpdateWithNullName() {
            CalendarItem calendarItem = CalendarItem.create(
                    "Original Name",
                    "Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );

            assertThatThrownBy(() -> calendarItem.update(null, "Description", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail to update with blank name")
        void shouldFailToUpdateWithBlankName() {
            CalendarItem calendarItem = CalendarItem.create(
                    "Original Name",
                    "Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );

            assertThatThrownBy(() -> calendarItem.update("   ", "Description", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail to update with null description")
        void shouldFailToUpdateWithNullDescription() {
            CalendarItem calendarItem = CalendarItem.create(
                    "Name",
                    "Original Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );

            assertThatThrownBy(() -> calendarItem.update("Name", null, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description");
        }

        @Test
        @DisplayName("should fail to update with blank description")
        void shouldFailToUpdateWithBlankDescription() {
            CalendarItem calendarItem = CalendarItem.create(
                    "Name",
                    "Original Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );

            assertThatThrownBy(() -> calendarItem.update("Name", "   ", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description");
        }

        @Test
        @DisplayName("should fail to update with null startDate")
        void shouldFailToUpdateWithNullStartDate() {
            CalendarItem calendarItem = CalendarItem.create(
                    "Name",
                    "Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );

            assertThatThrownBy(() -> calendarItem.update("Name", "Description", null, LocalDate.of(2026, 7, 1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Start date");
        }

        @Test
        @DisplayName("should fail to update with null endDate")
        void shouldFailToUpdateWithNullEndDate() {
            CalendarItem calendarItem = CalendarItem.create(
                    "Name",
                    "Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );

            assertThatThrownBy(() -> calendarItem.update("Name", "Description", LocalDate.of(2026, 7, 1), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("End date");
        }

        @Test
        @DisplayName("should fail to update with endDate before startDate")
        void shouldFailToUpdateWithEndDateBeforeStartDate() {
            CalendarItem calendarItem = CalendarItem.create(
                    "Name",
                    "Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );

            assertThatThrownBy(() -> calendarItem.update(
                    "Name",
                    "Description",
                    LocalDate.of(2026, 7, 10),
                    LocalDate.of(2026, 7, 5)
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("End date must be on or after start date");
        }
    }

    @Nested
    @DisplayName("isEventLinked()")
    class IsEventLinked {

        @Test
        @DisplayName("should identify event-linked calendar item")
        void shouldIdentifyEventLinkedCalendarItem() {
            EventId eventId = EventId.of(UUID.randomUUID());
            CalendarItem calendarItem = CalendarItem.reconstruct(
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
        @DisplayName("should identify manual calendar item")
        void shouldIdentifyManualCalendarItem() {
            CalendarItem calendarItem = CalendarItem.create(
                    "Manual Item",
                    "Created manually",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );

            CalendarItemAssert.assertThat(calendarItem)
                    .isManual()
                    .hasEventId(null);
        }
    }
}
