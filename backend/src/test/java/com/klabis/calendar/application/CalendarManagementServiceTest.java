package com.klabis.calendar.application;

import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.CalendarItemTestDataBuilder;
import com.klabis.calendar.domain.*;
import com.klabis.events.EventId;
import com.klabis.events.EventScheduleQuery;
import com.klabis.members.MemberId;
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
import java.util.Set;
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

    @Mock
    private EventScheduleQuery eventScheduleQuery;

    private CalendarManagementService testedSubject;

    @BeforeEach
    void setUp() {
        testedSubject = new CalendarManagementService(calendarRepository, eventScheduleQuery);
    }

    @Nested
    @DisplayName("listCalendarItems")
    class ListCalendarItemsTests {

        @Test
        @DisplayName("should return list of calendar items for given date range")
        void shouldReturnListOfCalendarItemsForDateRange() {
            LocalDate startDate = LocalDate.of(2026, 3, 1);
            LocalDate endDate = LocalDate.of(2026, 3, 31);

            ManualCalendarItem item1 = CalendarItemTestDataBuilder.aCalendarItem()
                    .withName("March Training")
                    .withStartDate(LocalDate.of(2026, 3, 10))
                    .withEndDate(LocalDate.of(2026, 3, 10))
                    .buildManual();

            ManualCalendarItem item2 = CalendarItemTestDataBuilder.aCalendarItem()
                    .withName("March Event")
                    .withStartDate(LocalDate.of(2026, 3, 20))
                    .withEndDate(LocalDate.of(2026, 3, 20))
                    .buildManual();

            when(calendarRepository.findByDateRange(startDate, endDate))
                    .thenReturn(List.of(item1, item2));

            List<CalendarItem> result = testedSubject.listCalendarItems(startDate, endDate, Sort.unsorted(), null);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("March Training");
            assertThat(result.get(1).getName()).isEqualTo("March Event");
        }

        @Test
        @DisplayName("should return empty list when no items found in date range")
        void shouldReturnEmptyListWhenNoItemsFound() {
            LocalDate startDate = LocalDate.of(2026, 3, 1);
            LocalDate endDate = LocalDate.of(2026, 3, 31);

            when(calendarRepository.findByDateRange(startDate, endDate)).thenReturn(List.of());

            List<CalendarItem> result = testedSubject.listCalendarItems(startDate, endDate, Sort.unsorted(), null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should accept date range of exactly 366 days")
        void shouldAcceptDateRangeOfExactly366Days() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 12, 31);

            when(calendarRepository.findByDateRange(startDate, endDate)).thenReturn(List.of());

            List<CalendarItem> result = testedSubject.listCalendarItems(startDate, endDate, Sort.unsorted(), null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw exception when date range exceeds 366 days")
        void shouldThrowExceptionWhenDateRangeExceeds366Days() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 2);

            assertThatThrownBy(() -> testedSubject.listCalendarItems(startDate, endDate, Sort.unsorted(), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Date range must not exceed 366 days")
                    .hasMessageContaining("368");
        }

        @Test
        @DisplayName("should throw exception when date range exceeds 366 days in non-leap year")
        void shouldThrowExceptionWhenDateRangeExceeds366DaysInNonLeapYear() {
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2026, 1, 2);

            assertThatThrownBy(() -> testedSubject.listCalendarItems(startDate, endDate, Sort.unsorted(), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Date range must not exceed 366 days");
        }

        @Test
        @DisplayName("should return only EVENT_DATE items for events where member is registered when mySchedule is set")
        void shouldReturnOnlyEventDateItemsForRegisteredMemberWhenMyScheduleSet() {
            LocalDate startDate = LocalDate.of(2026, 6, 1);
            LocalDate endDate = LocalDate.of(2026, 6, 30);
            MemberId memberId = new MemberId(UUID.randomUUID());
            EventId registeredEventId = new EventId(UUID.randomUUID());
            EventId otherEventId = new EventId(UUID.randomUUID());

            EventCalendarItem myEventItem = CalendarItemTestDataBuilder.aCalendarItem()
                    .withName("My Event")
                    .withStartDate(LocalDate.of(2026, 6, 15))
                    .withEndDate(LocalDate.of(2026, 6, 15))
                    .buildEventLinked(registeredEventId.value());

            EventCalendarItem otherEventItem = CalendarItemTestDataBuilder.aCalendarItem()
                    .withName("Other Event")
                    .withStartDate(LocalDate.of(2026, 6, 20))
                    .withEndDate(LocalDate.of(2026, 6, 20))
                    .buildEventLinked(otherEventId.value());

            when(eventScheduleQuery.findEventIdsByRegistration(memberId, startDate, endDate))
                    .thenReturn(Set.of(registeredEventId));
            when(eventScheduleQuery.findEventIdsByCoordinator(memberId, startDate, endDate))
                    .thenReturn(Set.of());
            when(calendarRepository.findByDateRange(startDate, endDate))
                    .thenReturn(List.of(myEventItem, otherEventItem));

            List<CalendarItem> result = testedSubject.listCalendarItems(startDate, endDate, Sort.unsorted(), memberId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("My Event");
        }

        @Test
        @DisplayName("should return only EVENT_DATE items for events where member is coordinator when mySchedule is set")
        void shouldReturnOnlyEventDateItemsForCoordinatorWhenMyScheduleSet() {
            LocalDate startDate = LocalDate.of(2026, 6, 1);
            LocalDate endDate = LocalDate.of(2026, 6, 30);
            MemberId memberId = new MemberId(UUID.randomUUID());
            EventId coordinatedEventId = new EventId(UUID.randomUUID());

            EventCalendarItem coordinatedItem = CalendarItemTestDataBuilder.aCalendarItem()
                    .withName("Coordinated Event")
                    .withStartDate(LocalDate.of(2026, 6, 10))
                    .withEndDate(LocalDate.of(2026, 6, 10))
                    .buildEventLinked(coordinatedEventId.value());

            when(eventScheduleQuery.findEventIdsByRegistration(memberId, startDate, endDate))
                    .thenReturn(Set.of());
            when(eventScheduleQuery.findEventIdsByCoordinator(memberId, startDate, endDate))
                    .thenReturn(Set.of(coordinatedEventId));
            when(calendarRepository.findByDateRange(startDate, endDate))
                    .thenReturn(List.of(coordinatedItem));

            List<CalendarItem> result = testedSubject.listCalendarItems(startDate, endDate, Sort.unsorted(), memberId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Coordinated Event");
        }

        @Test
        @DisplayName("should union registration and coordinator event IDs when mySchedule is set")
        void shouldUnionRegistrationAndCoordinatorEventIds() {
            LocalDate startDate = LocalDate.of(2026, 6, 1);
            LocalDate endDate = LocalDate.of(2026, 6, 30);
            MemberId memberId = new MemberId(UUID.randomUUID());
            EventId registeredEventId = new EventId(UUID.randomUUID());
            EventId coordinatedEventId = new EventId(UUID.randomUUID());
            EventId bothEventId = new EventId(UUID.randomUUID());

            EventCalendarItem registeredItem = CalendarItemTestDataBuilder.aCalendarItem()
                    .withName("Registered Event")
                    .withStartDate(LocalDate.of(2026, 6, 5))
                    .withEndDate(LocalDate.of(2026, 6, 5))
                    .buildEventLinked(registeredEventId.value());

            EventCalendarItem coordinatedItem = CalendarItemTestDataBuilder.aCalendarItem()
                    .withName("Coordinated Event")
                    .withStartDate(LocalDate.of(2026, 6, 10))
                    .withEndDate(LocalDate.of(2026, 6, 10))
                    .buildEventLinked(coordinatedEventId.value());

            EventCalendarItem bothItem = CalendarItemTestDataBuilder.aCalendarItem()
                    .withName("Both Event")
                    .withStartDate(LocalDate.of(2026, 6, 15))
                    .withEndDate(LocalDate.of(2026, 6, 15))
                    .buildEventLinked(bothEventId.value());

            when(eventScheduleQuery.findEventIdsByRegistration(memberId, startDate, endDate))
                    .thenReturn(Set.of(registeredEventId, bothEventId));
            when(eventScheduleQuery.findEventIdsByCoordinator(memberId, startDate, endDate))
                    .thenReturn(Set.of(coordinatedEventId, bothEventId));
            when(calendarRepository.findByDateRange(startDate, endDate))
                    .thenReturn(List.of(registeredItem, coordinatedItem, bothItem));

            List<CalendarItem> result = testedSubject.listCalendarItems(startDate, endDate, Sort.unsorted(), memberId);

            assertThat(result).hasSize(3);
            assertThat(result).extracting(CalendarItem::getName)
                    .containsExactlyInAnyOrder("Registered Event", "Coordinated Event", "Both Event");
        }

        @Test
        @DisplayName("should exclude EVENT_REGISTRATION_DATE (deadline) items when mySchedule is set")
        void shouldExcludeDeadlineItemsWhenMyScheduleSet() {
            LocalDate startDate = LocalDate.of(2026, 6, 1);
            LocalDate endDate = LocalDate.of(2026, 6, 30);
            MemberId memberId = new MemberId(UUID.randomUUID());
            EventId eventId = new EventId(UUID.randomUUID());

            EventCalendarItem eventDateItem = CalendarItemTestDataBuilder.aCalendarItem()
                    .withName("My Event")
                    .withStartDate(LocalDate.of(2026, 6, 15))
                    .withEndDate(LocalDate.of(2026, 6, 15))
                    .buildEventLinked(eventId.value());

            EventCalendarItem deadlineItem = CalendarItemTestDataBuilder.aCalendarItem()
                    .withName("Přihlášky - My Event")
                    .withStartDate(LocalDate.of(2026, 6, 5))
                    .withEndDate(LocalDate.of(2026, 6, 5))
                    .buildRegistrationDeadlineLinked(eventId.value());

            when(eventScheduleQuery.findEventIdsByRegistration(memberId, startDate, endDate))
                    .thenReturn(Set.of(eventId));
            when(eventScheduleQuery.findEventIdsByCoordinator(memberId, startDate, endDate))
                    .thenReturn(Set.of());
            when(calendarRepository.findByDateRange(startDate, endDate))
                    .thenReturn(List.of(eventDateItem, deadlineItem));

            List<CalendarItem> result = testedSubject.listCalendarItems(startDate, endDate, Sort.unsorted(), memberId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("My Event");
        }

        @Test
        @DisplayName("should exclude manual items when mySchedule is set")
        void shouldExcludeManualItemsWhenMyScheduleSet() {
            LocalDate startDate = LocalDate.of(2026, 6, 1);
            LocalDate endDate = LocalDate.of(2026, 6, 30);
            MemberId memberId = new MemberId(UUID.randomUUID());
            EventId eventId = new EventId(UUID.randomUUID());

            EventCalendarItem myEventItem = CalendarItemTestDataBuilder.aCalendarItem()
                    .withName("My Event")
                    .withStartDate(LocalDate.of(2026, 6, 15))
                    .withEndDate(LocalDate.of(2026, 6, 15))
                    .buildEventLinked(eventId.value());

            ManualCalendarItem manualItem = CalendarItemTestDataBuilder.aCalendarItem()
                    .withName("Club Meeting")
                    .withStartDate(LocalDate.of(2026, 6, 20))
                    .withEndDate(LocalDate.of(2026, 6, 20))
                    .buildManual();

            when(eventScheduleQuery.findEventIdsByRegistration(memberId, startDate, endDate))
                    .thenReturn(Set.of(eventId));
            when(eventScheduleQuery.findEventIdsByCoordinator(memberId, startDate, endDate))
                    .thenReturn(Set.of());
            when(calendarRepository.findByDateRange(startDate, endDate))
                    .thenReturn(List.of(myEventItem, manualItem));

            List<CalendarItem> result = testedSubject.listCalendarItems(startDate, endDate, Sort.unsorted(), memberId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("My Event");
        }

        @Test
        @DisplayName("should return empty list when member has no involvement when mySchedule is set")
        void shouldReturnEmptyWhenMemberHasNoInvolvement() {
            LocalDate startDate = LocalDate.of(2026, 6, 1);
            LocalDate endDate = LocalDate.of(2026, 6, 30);
            MemberId memberId = new MemberId(UUID.randomUUID());

            EventCalendarItem item = CalendarItemTestDataBuilder.aCalendarItem()
                    .withName("Some Event")
                    .withStartDate(LocalDate.of(2026, 6, 15))
                    .withEndDate(LocalDate.of(2026, 6, 15))
                    .buildEventLinked(UUID.randomUUID());

            when(eventScheduleQuery.findEventIdsByRegistration(memberId, startDate, endDate))
                    .thenReturn(Set.of());
            when(eventScheduleQuery.findEventIdsByCoordinator(memberId, startDate, endDate))
                    .thenReturn(Set.of());
            when(calendarRepository.findByDateRange(startDate, endDate))
                    .thenReturn(List.of(item));

            List<CalendarItem> result = testedSubject.listCalendarItems(startDate, endDate, Sort.unsorted(), memberId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getCalendarItem")
    class GetCalendarItemTests {

        @Test
        @DisplayName("should return calendar item dto when item exists")
        void shouldReturnCalendarItemDtoWhenExists() {
            UUID calendarItemId = UUID.randomUUID();
            ManualCalendarItem calendarItem = CalendarItemTestDataBuilder.aCalendarItemWithId(new CalendarItemId(calendarItemId))
                    .withName("Test Event")
                    .withDescription("Test description")
                    .buildManual();

            when(calendarRepository.findById(any())).thenReturn(Optional.of(calendarItem));

            CalendarItem result = testedSubject.getCalendarItem(new CalendarItemId(calendarItemId));

            assertThat(result.getId().value()).isEqualTo(calendarItemId);
            assertThat(result.getName()).isEqualTo("Test Event");
            assertThat(result.getDescription()).isEqualTo("Test description");
        }

        @Test
        @DisplayName("should throw CalendarNotFoundException when item not found")
        void shouldThrowExceptionWhenNotFound() {
            UUID calendarItemId = UUID.randomUUID();

            when(calendarRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testedSubject.getCalendarItem(new CalendarItemId(calendarItemId)))
                    .isInstanceOf(CalendarNotFoundException.class)
                    .hasMessageContaining(calendarItemId.toString());
        }
    }

    @Nested
    @DisplayName("createCalendarItem")
    class CreateCalendarItemTests {

        @Test
        @DisplayName("should create ManualCalendarItem and return it")
        void shouldCreateCalendarItemAndReturnIt() {
            CalendarItem.CreateCalendarItem command = CalendarItemCreateCalendarItemBuilder.builder()
                    .name("New Event")
                    .description("New event description")
                    .startDate(LocalDate.of(2026, 3, 15))
                    .endDate(LocalDate.of(2026, 3, 15))
                    .build();

            ManualCalendarItem savedItem = CalendarItemTestDataBuilder.aCalendarItem()
                    .withName("New Event")
                    .buildManual();

            when(calendarRepository.save(any(ManualCalendarItem.class))).thenReturn(savedItem);

            CalendarItem result = testedSubject.createCalendarItem(command);

            assertThat(result).isEqualTo(savedItem);
            verify(calendarRepository).save(any(ManualCalendarItem.class));
        }
    }

    @Nested
    @DisplayName("updateCalendarItem")
    class UpdateCalendarItemTests {

        @Test
        @DisplayName("should update ManualCalendarItem")
        void shouldUpdateManualCalendarItem() {
            UUID calendarItemId = UUID.randomUUID();
            ManualCalendarItem existingItem = CalendarItemTestDataBuilder.aCalendarItem()
                    .withName("Old Name")
                    .buildManual();

            CalendarItem.UpdateCalendarItem command = CalendarItemUpdateCalendarItemBuilder.builder()
                    .name("Updated Name")
                    .description("Updated description")
                    .startDate(LocalDate.of(2026, 3, 20))
                    .endDate(LocalDate.of(2026, 3, 20))
                    .build();

            when(calendarRepository.findById(any())).thenReturn(Optional.of(existingItem));
            when(calendarRepository.save(any(ManualCalendarItem.class))).thenReturn(existingItem);

            testedSubject.updateCalendarItem(new CalendarItemId(calendarItemId), command);

            assertThat(existingItem.getName()).isEqualTo("Updated Name");
            assertThat(existingItem.getDescription()).isEqualTo("Updated description");
            verify(calendarRepository).save(existingItem);
        }

        @Test
        @DisplayName("should throw CalendarItemReadOnlyException when updating event-linked item")
        void shouldThrowExceptionWhenUpdatingEventLinkedItem() {
            UUID calendarItemId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            EventCalendarItem eventLinkedItem = CalendarItemTestDataBuilder.aCalendarItem()
                    .withEventId(eventId)
                    .buildEventLinked(eventId);

            CalendarItem.UpdateCalendarItem command = CalendarItemUpdateCalendarItemBuilder.builder()
                    .name("Updated Name")
                    .description("Updated description")
                    .startDate(LocalDate.of(2026, 3, 20))
                    .endDate(LocalDate.of(2026, 3, 20))
                    .build();

            when(calendarRepository.findById(any())).thenReturn(Optional.of(eventLinkedItem));

            assertThatThrownBy(() -> testedSubject.updateCalendarItem(new CalendarItemId(calendarItemId), command))
                    .isInstanceOf(CalendarItemReadOnlyException.class);
        }

        @Test
        @DisplayName("should throw CalendarNotFoundException when item not found")
        void shouldThrowExceptionWhenNotFound() {
            UUID calendarItemId = UUID.randomUUID();
            CalendarItem.UpdateCalendarItem command = CalendarItemUpdateCalendarItemBuilder.builder()
                    .name("Updated Name")
                    .description("Updated description")
                    .startDate(LocalDate.of(2026, 3, 20))
                    .endDate(LocalDate.of(2026, 3, 20))
                    .build();

            when(calendarRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testedSubject.updateCalendarItem(new CalendarItemId(calendarItemId), command))
                    .isInstanceOf(CalendarNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteCalendarItem")
    class DeleteCalendarItemTests {

        @Test
        @DisplayName("should delete ManualCalendarItem")
        void shouldDeleteManualCalendarItem() {
            UUID calendarItemId = UUID.randomUUID();
            ManualCalendarItem manualItem = CalendarItemTestDataBuilder.aCalendarItem()
                    .withName("To Delete")
                    .buildManual();

            when(calendarRepository.findById(any())).thenReturn(Optional.of(manualItem));

            testedSubject.deleteCalendarItem(new CalendarItemId(calendarItemId));

            verify(calendarRepository).delete(manualItem);
        }

        @Test
        @DisplayName("should throw CalendarItemReadOnlyException when deleting event-linked item")
        void shouldThrowExceptionWhenDeletingEventLinkedItem() {
            UUID calendarItemId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            EventCalendarItem eventLinkedItem = CalendarItemTestDataBuilder.aCalendarItem()
                    .withEventId(eventId)
                    .buildEventLinked(eventId);

            when(calendarRepository.findById(any())).thenReturn(Optional.of(eventLinkedItem));

            assertThatThrownBy(() -> testedSubject.deleteCalendarItem(new CalendarItemId(calendarItemId)))
                    .isInstanceOf(CalendarItemReadOnlyException.class);
        }

        @Test
        @DisplayName("should throw CalendarNotFoundException when item not found")
        void shouldThrowExceptionWhenNotFound() {
            UUID calendarItemId = UUID.randomUUID();

            when(calendarRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testedSubject.deleteCalendarItem(new CalendarItemId(calendarItemId)))
                    .isInstanceOf(CalendarNotFoundException.class);
        }
    }
}
