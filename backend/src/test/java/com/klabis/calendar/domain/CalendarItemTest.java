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

/**
 * Unit tests for CalendarItem aggregate root.
 * <p>
 * Tests business rules and invariants:
 * - CalendarItem creation with required fields
 * - Validation of name, description, dates
 * - Date range validation (endDate >= startDate)
 * - Event-linked items are read-only (cannot update/delete)
 * - Manual items can be updated and deleted
 */
@DisplayName("CalendarItem Aggregate")
class CalendarItemTest {

    @Nested
    @DisplayName("create() factory method")
    class CreateMethod {

        @Test
        @DisplayName("should create manual calendar item with all fields")
        void shouldCreateManualCalendarItemWithAllFields() {
            // Arrange
            String name = "Club Training Session";
            String description = "Weekly orienteering training at City Park";
            LocalDate startDate = LocalDate.of(2026, 6, 15);
            LocalDate endDate = LocalDate.of(2026, 6, 15);

            // Act
            CalendarItem calendarItem = CalendarItem.create(name, description, startDate, endDate);

            // Assert
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
            // Arrange
            String name = "Summer Camp";
            String description = "5-day orienteering summer camp";
            LocalDate startDate = LocalDate.of(2026, 7, 1);
            LocalDate endDate = LocalDate.of(2026, 7, 5);

            // Act
            CalendarItem calendarItem = CalendarItem.create(name, description, startDate, endDate);

            // Assert
            CalendarItemAssert.assertThat(calendarItem)
                    .hasStartDate(startDate)
                    .hasEndDate(endDate)
                    .isManual();
        }

        @Test
        @DisplayName("should create calendar item with same start and end date")
        void shouldCreateCalendarItemWithSameStartAndEndDate() {
            // Arrange
            LocalDate sameDate = LocalDate.of(2026, 6, 15);

            // Act
            CalendarItem calendarItem = CalendarItem.create(
                    "Single Day Event",
                    "One day activity",
                    sameDate,
                    sameDate
            );

            // Assert
            CalendarItemAssert.assertThat(calendarItem)
                    .hasStartDate(sameDate)
                    .hasEndDate(sameDate);
        }

        @Test
        @DisplayName("should fail when name is null")
        void shouldFailWhenNameIsNull() {
            // Arrange
            LocalDate startDate = LocalDate.of(2026, 6, 15);
            LocalDate endDate = LocalDate.of(2026, 6, 15);

            // Act & Assert
            assertThatThrownBy(() -> CalendarItem.create(
                    null,
                    "Description",
                    startDate,
                    endDate
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail when name is blank")
        void shouldFailWhenNameIsBlank() {
            // Arrange
            LocalDate startDate = LocalDate.of(2026, 6, 15);
            LocalDate endDate = LocalDate.of(2026, 6, 15);

            // Act & Assert
            assertThatThrownBy(() -> CalendarItem.create(
                    "   ",
                    "Description",
                    startDate,
                    endDate
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail when description is null")
        void shouldFailWhenDescriptionIsNull() {
            // Arrange
            LocalDate startDate = LocalDate.of(2026, 6, 15);
            LocalDate endDate = LocalDate.of(2026, 6, 15);

            // Act & Assert
            assertThatThrownBy(() -> CalendarItem.create(
                    "Name",
                    null,
                    startDate,
                    endDate
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description");
        }

        @Test
        @DisplayName("should fail when description is blank")
        void shouldFailWhenDescriptionIsBlank() {
            // Arrange
            LocalDate startDate = LocalDate.of(2026, 6, 15);
            LocalDate endDate = LocalDate.of(2026, 6, 15);

            // Act & Assert
            assertThatThrownBy(() -> CalendarItem.create(
                    "Name",
                    "   ",
                    startDate,
                    endDate
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description");
        }

        @Test
        @DisplayName("should fail when startDate is null")
        void shouldFailWhenStartDateIsNull() {
            // Arrange
            LocalDate endDate = LocalDate.of(2026, 6, 15);

            // Act & Assert
            assertThatThrownBy(() -> CalendarItem.create(
                    "Name",
                    "Description",
                    null,
                    endDate
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Start date");
        }

        @Test
        @DisplayName("should fail when endDate is null")
        void shouldFailWhenEndDateIsNull() {
            // Arrange
            LocalDate startDate = LocalDate.of(2026, 6, 15);

            // Act & Assert
            assertThatThrownBy(() -> CalendarItem.create(
                    "Name",
                    "Description",
                    startDate,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("End date");
        }

        @Test
        @DisplayName("should fail when endDate is before startDate")
        void shouldFailWhenEndDateIsBeforeStartDate() {
            // Arrange
            LocalDate startDate = LocalDate.of(2026, 6, 15);
            LocalDate endDate = LocalDate.of(2026, 6, 10);

            // Act & Assert
            assertThatThrownBy(() -> CalendarItem.create(
                    "Name",
                    "Description",
                    startDate,
                    endDate
            ))
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
            // Arrange
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

            // Act
            calendarItem.update(newName, newDescription, newStartDate, newEndDate);

            // Assert
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
            // Arrange
            CalendarItem calendarItem = CalendarItem.create(
                    "Multi-day Event",
                    "Description",
                    LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 5)
            );

            LocalDate singleDate = LocalDate.of(2026, 6, 15);

            // Act
            calendarItem.update("Single Day", "Updated", singleDate, singleDate);

            // Assert
            CalendarItemAssert.assertThat(calendarItem)
                    .hasStartDate(singleDate)
                    .hasEndDate(singleDate);
        }

        @Test
        @DisplayName("should fail to update event-linked calendar item")
        void shouldFailToUpdateEventLinkedCalendarItem() {
            // Arrange
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
            CalendarItemAssert.assertThat(calendarItem).isEventLinked();

            // Act & Assert
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
            // Arrange
            CalendarItem calendarItem = CalendarItem.create(
                    "Original Name",
                    "Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );

            // Act & Assert
            assertThatThrownBy(() -> calendarItem.update(
                    null,
                    "Description",
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 7, 1)
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail to update with blank name")
        void shouldFailToUpdateWithBlankName() {
            // Arrange
            CalendarItem calendarItem = CalendarItem.create(
                    "Original Name",
                    "Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );

            // Act & Assert
            assertThatThrownBy(() -> calendarItem.update(
                    "   ",
                    "Description",
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 7, 1)
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail to update with null description")
        void shouldFailToUpdateWithNullDescription() {
            // Arrange
            CalendarItem calendarItem = CalendarItem.create(
                    "Name",
                    "Original Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );

            // Act & Assert
            assertThatThrownBy(() -> calendarItem.update(
                    "Name",
                    null,
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 7, 1)
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description");
        }

        @Test
        @DisplayName("should fail to update with blank description")
        void shouldFailToUpdateWithBlankDescription() {
            // Arrange
            CalendarItem calendarItem = CalendarItem.create(
                    "Name",
                    "Original Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );

            // Act & Assert
            assertThatThrownBy(() -> calendarItem.update(
                    "Name",
                    "   ",
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 7, 1)
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description");
        }

        @Test
        @DisplayName("should fail to update with null startDate")
        void shouldFailToUpdateWithNullStartDate() {
            // Arrange
            CalendarItem calendarItem = CalendarItem.create(
                    "Name",
                    "Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );

            // Act & Assert
            assertThatThrownBy(() -> calendarItem.update(
                    "Name",
                    "Description",
                    null,
                    LocalDate.of(2026, 7, 1)
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Start date");
        }

        @Test
        @DisplayName("should fail to update with null endDate")
        void shouldFailToUpdateWithNullEndDate() {
            // Arrange
            CalendarItem calendarItem = CalendarItem.create(
                    "Name",
                    "Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );

            // Act & Assert
            assertThatThrownBy(() -> calendarItem.update(
                    "Name",
                    "Description",
                    LocalDate.of(2026, 7, 1),
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("End date");
        }

        @Test
        @DisplayName("should fail to update with endDate before startDate")
        void shouldFailToUpdateWithEndDateBeforeStartDate() {
            // Arrange
            CalendarItem calendarItem = CalendarItem.create(
                    "Name",
                    "Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );

            // Act & Assert
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
    @DisplayName("delete() method")
    class DeleteMethod {

        @Test
        @DisplayName("should delete manual calendar item")
        void shouldDeleteManualCalendarItem() {
            // Arrange
            CalendarItem calendarItem = CalendarItem.create(
                    "Manual Item",
                    "Can be deleted",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );
            CalendarItemAssert.assertThat(calendarItem).isManual();

            // Act - should not throw exception
            calendarItem.delete();

            // Assert - no exception thrown means success
            CalendarItemAssert.assertThat(calendarItem).isManual();
        }

        @Test
        @DisplayName("should fail to delete event-linked calendar item")
        void shouldFailToDeleteEventLinkedCalendarItem() {
            // Arrange
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
            CalendarItemAssert.assertThat(calendarItem).isEventLinked();

            // Act & Assert
            assertThatThrownBy(() -> calendarItem.delete())
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Cannot manually delete event-linked calendar item");
        }
    }

    @Nested
    @DisplayName("Event-linked read-only enforcement")
    class EventLinkedReadOnlyEnforcement {

        @Test
        @DisplayName("should identify event-linked calendar item")
        void shouldIdentifyEventLinkedCalendarItem() {
            // Arrange
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

            // Assert
            CalendarItemAssert.assertThat(calendarItem)
                    .isEventLinked()
                    .hasEventId(eventId);
        }

        @Test
        @DisplayName("should identify manual calendar item")
        void shouldIdentifyManualCalendarItem() {
            // Arrange
            CalendarItem calendarItem = CalendarItem.create(
                    "Manual Item",
                    "Created manually",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );

            // Assert
            CalendarItemAssert.assertThat(calendarItem)
                    .isManual()
                    .hasEventId(null);
        }

        @Test
        @DisplayName("should prevent update of event-linked item")
        void shouldPreventUpdateOfEventLinkedItem() {
            // Arrange
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

            // Act & Assert
            assertThatThrownBy(() -> calendarItem.update(
                    "Attempted Update",
                    "Should fail",
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 7, 1)
            ))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("event-linked");
        }

        @Test
        @DisplayName("should prevent delete of event-linked item")
        void shouldPreventDeleteOfEventLinkedItem() {
            // Arrange
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

            // Act & Assert
            assertThatThrownBy(() -> calendarItem.delete())
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("event-linked");
        }

        @Test
        @DisplayName("should allow update of manual item")
        void shouldAllowUpdateOfManualItem() {
            // Arrange
            CalendarItem calendarItem = CalendarItem.create(
                    "Manual Item",
                    "Can be updated",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );

            // Act - should not throw exception
            calendarItem.update(
                    "Updated Name",
                    "Updated Description",
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 7, 5)
            );

            // Assert
            CalendarItemAssert.assertThat(calendarItem)
                    .hasName("Updated Name")
                    .hasDescription("Updated Description")
                    .hasStartDate(LocalDate.of(2026, 7, 1))
                    .hasEndDate(LocalDate.of(2026, 7, 5))
                    .isManual();
        }

        @Test
        @DisplayName("should allow delete of manual item")
        void shouldAllowDeleteOfManualItem() {
            // Arrange
            CalendarItem calendarItem = CalendarItem.create(
                    "Manual Item",
                    "Can be deleted",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );

            // Act - should not throw exception
            calendarItem.delete();

            // Assert - no exception means success
            CalendarItemAssert.assertThat(calendarItem).isManual();
        }
    }
}
