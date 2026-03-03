package com.klabis.calendar.application;

import com.klabis.calendar.CalendarItemTestDataBuilder;
import com.klabis.calendar.domain.CalendarItem;
import com.klabis.calendar.domain.CalendarItemId;
import com.klabis.calendar.domain.CalendarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalendarManagementService Unit Tests")
class CalendarManagementServiceTest {

    @Mock
    private CalendarRepository calendarRepository;

    private CalendarManagementService testedSubject;

    @BeforeEach
    void setUp() {
        testedSubject = new CalendarManagementService(calendarRepository);
    }

    @Nested
    @DisplayName("listCalendarItems")
    class ListCalendarItemsTests {

        @Test
        @DisplayName("should return list of calendar items for given date range")
        void shouldReturnListOfCalendarItemsForDateRange() {
            LocalDate startDate = LocalDate.of(2026, 3, 1);
            LocalDate endDate = LocalDate.of(2026, 3, 31);

            CalendarItem item1 = CalendarItemTestDataBuilder.aCalendarItem()
                    .withName("March Training")
                    .withStartDate(LocalDate.of(2026, 3, 10))
                    .withEndDate(LocalDate.of(2026, 3, 10))
                    .buildManual();

            CalendarItem item2 = CalendarItemTestDataBuilder.aCalendarItem()
                    .withName("March Event")
                    .withStartDate(LocalDate.of(2026, 3, 20))
                    .withEndDate(LocalDate.of(2026, 3, 20))
                    .buildManual();

            when(calendarRepository.findByDateRange(startDate, endDate))
                    .thenReturn(List.of(item1, item2));

            List<CalendarItemDto> result = testedSubject.listCalendarItems(startDate, endDate, Sort.unsorted());

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("March Training");
            assertThat(result.get(1).name()).isEqualTo("March Event");
        }

        @Test
        @DisplayName("should return empty list when no items found in date range")
        void shouldReturnEmptyListWhenNoItemsFound() {
            LocalDate startDate = LocalDate.of(2026, 3, 1);
            LocalDate endDate = LocalDate.of(2026, 3, 31);

            when(calendarRepository.findByDateRange(startDate, endDate)).thenReturn(List.of());

            List<CalendarItemDto> result = testedSubject.listCalendarItems(startDate, endDate, Sort.unsorted());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should accept date range of exactly 366 days")
        void shouldAcceptDateRangeOfExactly366Days() {
            LocalDate startDate = LocalDate.of(2024, 1, 1); // Leap year
            LocalDate endDate = LocalDate.of(2024, 12, 31);

            when(calendarRepository.findByDateRange(startDate, endDate)).thenReturn(List.of());

            List<CalendarItemDto> result = testedSubject.listCalendarItems(startDate, endDate, Sort.unsorted());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw exception when date range exceeds 366 days")
        void shouldThrowExceptionWhenDateRangeExceeds366Days() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 2); // 367 days in leap year

            assertThatThrownBy(() -> testedSubject.listCalendarItems(startDate, endDate, Sort.unsorted()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Date range must not exceed 366 days")
                    .hasMessageContaining("368");
        }

        @Test
        @DisplayName("should throw exception when date range exceeds 366 days in non-leap year")
        void shouldThrowExceptionWhenDateRangeExceeds366DaysInNonLeapYear() {
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2026, 1, 2); // 367 days

            assertThatThrownBy(() -> testedSubject.listCalendarItems(startDate, endDate, Sort.unsorted()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Date range must not exceed 366 days");
        }
    }

    @Nested
    @DisplayName("getCalendarItem")
    class GetCalendarItemTests {

        @Test
        @DisplayName("should return calendar item dto when item exists")
        void shouldReturnCalendarItemDtoWhenExists() {
            UUID calendarItemId = UUID.randomUUID();
            CalendarItem calendarItem = CalendarItemTestDataBuilder.aCalendarItemWithId(new CalendarItemId(calendarItemId))
                    .withName("Test Event")
                    .withDescription("Test description")
                    .buildManual();

            when(calendarRepository.findById(any())).thenReturn(Optional.of(calendarItem));

            CalendarItemDto result = testedSubject.getCalendarItem(calendarItemId);

            assertThat(result.id()).isEqualTo(calendarItemId);
            assertThat(result.name()).isEqualTo("Test Event");
            assertThat(result.description()).isEqualTo("Test description");
        }

        @Test
        @DisplayName("should throw CalendarNotFoundException when item not found")
        void shouldThrowExceptionWhenNotFound() {
            UUID calendarItemId = UUID.randomUUID();

            when(calendarRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testedSubject.getCalendarItem(calendarItemId))
                    .isInstanceOf(CalendarNotFoundException.class)
                    .hasMessageContaining(calendarItemId.toString());
        }
    }

    @Nested
    @DisplayName("createCalendarItem")
    class CreateCalendarItemTests {

        @Test
        @DisplayName("should create calendar item and return its ID")
        void shouldCreateCalendarItemAndReturnId() {
            CreateCalendarItemCommand command = new CreateCalendarItemCommand(
                    "New Event",
                    "New event description",
                    LocalDate.of(2026, 3, 15),
                    LocalDate.of(2026, 3, 15)
            );

            CalendarItem savedItem = CalendarItemTestDataBuilder.aCalendarItem()
                    .withName("New Event")
                    .buildManual();

            when(calendarRepository.save(any(CalendarItem.class))).thenReturn(savedItem);

            UUID result = testedSubject.createCalendarItem(command);

            assertThat(result).isEqualTo(savedItem.getId().value());
            verify(calendarRepository).save(any(CalendarItem.class));
        }
    }

    @Nested
    @DisplayName("updateCalendarItem")
    class UpdateCalendarItemTests {

        @Test
        @DisplayName("should update manual calendar item")
        void shouldUpdateManualCalendarItem() {
            UUID calendarItemId = UUID.randomUUID();
            CalendarItem existingItem = CalendarItemTestDataBuilder.aCalendarItem()
                    .withName("Old Name")
                    .buildManual();

            UpdateCalendarItemCommand command = new UpdateCalendarItemCommand(
                    "Updated Name",
                    "Updated description",
                    LocalDate.of(2026, 3, 20),
                    LocalDate.of(2026, 3, 20)
            );

            when(calendarRepository.findById(any())).thenReturn(Optional.of(existingItem));
            when(calendarRepository.save(any(CalendarItem.class))).thenReturn(existingItem);

            testedSubject.updateCalendarItem(calendarItemId, command);

            assertThat(existingItem.getName()).isEqualTo("Updated Name");
            assertThat(existingItem.getDescription()).isEqualTo("Updated description");
            verify(calendarRepository).save(existingItem);
        }

        @Test
        @DisplayName("should throw CalendarItemReadOnlyException when updating event-linked item")
        void shouldThrowExceptionWhenUpdatingEventLinkedItem() {
            UUID calendarItemId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            CalendarItem eventLinkedItem = CalendarItemTestDataBuilder.aCalendarItem()
                    .withEventId(eventId)
                    .build();

            UpdateCalendarItemCommand command = new UpdateCalendarItemCommand(
                    "Updated Name",
                    "Updated description",
                    LocalDate.of(2026, 3, 20),
                    LocalDate.of(2026, 3, 20)
            );

            when(calendarRepository.findById(any())).thenReturn(Optional.of(eventLinkedItem));

            assertThatThrownBy(() -> testedSubject.updateCalendarItem(calendarItemId, command))
                    .isInstanceOf(CalendarItemReadOnlyException.class);
        }

        @Test
        @DisplayName("should throw CalendarNotFoundException when item not found")
        void shouldThrowExceptionWhenNotFound() {
            UUID calendarItemId = UUID.randomUUID();
            UpdateCalendarItemCommand command = new UpdateCalendarItemCommand(
                    "Updated Name",
                    "Updated description",
                    LocalDate.of(2026, 3, 20),
                    LocalDate.of(2026, 3, 20)
            );

            when(calendarRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testedSubject.updateCalendarItem(calendarItemId, command))
                    .isInstanceOf(CalendarNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteCalendarItem")
    class DeleteCalendarItemTests {

        @Test
        @DisplayName("should delete manual calendar item")
        void shouldDeleteManualCalendarItem() {
            UUID calendarItemId = UUID.randomUUID();
            CalendarItem manualItem = CalendarItemTestDataBuilder.aCalendarItem()
                    .withName("To Delete")
                    .buildManual();

            when(calendarRepository.findById(any())).thenReturn(Optional.of(manualItem));

            testedSubject.deleteCalendarItem(calendarItemId);

            verify(calendarRepository).delete(manualItem);
        }

        @Test
        @DisplayName("should throw CalendarItemReadOnlyException when deleting event-linked item")
        void shouldThrowExceptionWhenDeletingEventLinkedItem() {
            UUID calendarItemId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            CalendarItem eventLinkedItem = CalendarItemTestDataBuilder.aCalendarItem()
                    .withEventId(eventId)
                    .build();

            when(calendarRepository.findById(any())).thenReturn(Optional.of(eventLinkedItem));

            assertThatThrownBy(() -> testedSubject.deleteCalendarItem(calendarItemId))
                    .isInstanceOf(CalendarItemReadOnlyException.class);
        }

        @Test
        @DisplayName("should throw CalendarNotFoundException when item not found")
        void shouldThrowExceptionWhenNotFound() {
            UUID calendarItemId = UUID.randomUUID();

            when(calendarRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testedSubject.deleteCalendarItem(calendarItemId))
                    .isInstanceOf(CalendarNotFoundException.class);
        }
    }
}
