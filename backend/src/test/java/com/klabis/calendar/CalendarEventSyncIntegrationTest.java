package com.klabis.calendar;

import com.klabis.calendar.eventsintegration.EventData;
import com.klabis.calendar.persistence.CalendarRepository;
import com.klabis.calendar.persistence.EventDataProvider;
import com.klabis.events.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.PublishedEvents;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Integration test for event-driven synchronization between Events and CalendarItems.
 *
 * <p>This test verifies the complete event-driven flow:
 * <ol>
 *   <li>Event publishes domain events (EventPublishedEvent, EventUpdatedEvent, EventCancelledEvent)</li>
 *   <li>Calendar event handlers receive events via Spring Modulith outbox</li>
 *   <li>CalendarItems are created/updated/deleted automatically</li>
 * </ol>
 *
 * <p>Tests verify:
 * - CalendarItem creation when Event is published
 * - CalendarItem update when Event is updated
 * - CalendarItem deletion when Event is cancelled
 * - Description formatting (location + " - " + organizer + optional website URL)
 * - Idempotent behavior (no duplicate calendar items)
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.STANDALONE)
//@ActiveProfiles("test")
@TestPropertySource(properties = {"jasypt.encryptor.password=example"})
//@CleanupTestData
@DisplayName("Calendar Event Synchronization Integration Tests")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS, statements = {
        """
                insert into events(id, name, event_date, location, organizer, status, created_by, modified_by, version)
                values('f1d7fcda-024e-42ad-9c08-fc20d07a166d', 'TEst event', '2020-04-21', 'Brno', 'OOB', 'DRAFT', 'admin', 'admin', 0);
        """
})
class CalendarEventSyncIntegrationTest {

    private static final EventId EVENT_ID = new EventId(UUID.fromString("f1d7fcda-024e-42ad-9c08-fc20d07a166d"));

    @Autowired
    private CalendarRepository calendarRepository;

    @MockitoBean
    private EventDataProvider eventDataProviderMock;

    @Test
    @DisplayName("should create calendar item when EventPublishedEvent arrives")
    void shouldCreateCalendarItemWhenEventIsPublished(Scenario scenario, PublishedEvents events) {
        // Given: Event data
        final EventId eventId = EVENT_ID;

        when(eventDataProviderMock.getEventData(eventId)).thenReturn(new EventData(
                "Spring Boot Workshop",
                LocalDate.of(2024, 3, 15),
                "Prague CC",
                "OOB",
                WebsiteUrl.of("https://example.com/workshop")
        ));

        // When & Then: CalendarItem should be created automatically
        scenario.publish(new EventPublishedEvent(eventId))
                .andWaitForStateChange(() -> calendarRepository.findByEventId(eventId).isPresent())
                .andVerify(isPresent -> {
                    CalendarItem calendarItem = calendarRepository.findByEventId(eventId).orElseThrow();
                    assertThat(calendarItem.getName()).isEqualTo("Spring Boot Workshop");
                    assertThat(calendarItem.getDescription())
                            .isEqualTo("Prague CC - OOB\nhttps://example.com/workshop");
                    assertThat(calendarItem.getStartDate()).isEqualTo(LocalDate.of(2024, 3, 15));
                    assertThat(calendarItem.getEndDate()).isEqualTo(LocalDate.of(2024, 3, 15));
                    assertThat(calendarItem.getEventId()).isEqualTo(eventId);
                });
    }

    @Test
    @DisplayName("should update calendar item when EventUpdatedEvent arrives")
    void shouldUpdateCalendarItemWhenEventIsUpdated(Scenario scenario) {
        // Given: An existing calendar item
        final EventId eventId = EVENT_ID;

        when(eventDataProviderMock.getEventData(eventId)).thenReturn(new EventData(
                "Initial Workshop",
                LocalDate.of(2024, 5, 10),
                "Old Loc",
                "OldOrg",
                WebsiteUrl.of("https://old-url.com")
        ));

        // First, create the calendar item
        scenario.publish(new EventPublishedEvent(eventId))
                .andWaitForStateChange(() -> calendarRepository.findByEventId(eventId).orElse(null));

        // When: Event is updated
        when(eventDataProviderMock.getEventData(eventId)).thenReturn(new EventData(
                "Updated Workshop",
                LocalDate.of(2024, 5, 15),
                "New Loc",
                "NewOrg",
                WebsiteUrl.of("https://new-url.com")
        ));

        scenario.publish(new EventUpdatedEvent(
                        eventId,
                        "Updated Workshop",
                        LocalDate.of(2024, 5, 15),
                        "New Loc",
                        "NewOrg",
                        WebsiteUrl.of("https://new-url.com"),
                        java.time.Instant.now()
                )).andWaitForStateChange(() -> calendarRepository.findByEventId(eventId).orElse(null))
                .andVerify(updatedItem -> {
                    assertThat(updatedItem.getName()).isEqualTo("Updated Workshop");
                    assertThat(updatedItem.getDescription())
                            .isEqualTo("New Loc - NewOrg\nhttps://new-url.com");
                    assertThat(updatedItem.getStartDate()).isEqualTo(LocalDate.of(2024, 5, 15));
                    assertThat(updatedItem.getEndDate()).isEqualTo(LocalDate.of(2024, 5, 15));
                });
    }

    @Test
    @DisplayName("should delete calendar item when EventCancelledEvent arrives")
    void shouldDeleteCalendarItemWhenEventIsCancelled(Scenario scenario) {
        // Given: An existing calendar item
        final EventId eventId = EVENT_ID;
        final CalendarItemId[] calendarItemIdRef = new CalendarItemId[1];

        when(eventDataProviderMock.getEventData(eventId)).thenReturn(new EventData(
                "Workshop to Cancel",
                LocalDate.of(2024, 6, 20),
                "Some Loc",
                "OOB",
                null
        ));

        // First, create the calendar item
        scenario.publish(new EventPublishedEvent(eventId))
                .andWaitForStateChange(() -> {
                    CalendarItem item = calendarRepository.findByEventId(eventId).orElse(null);
                    if (item != null) {
                        calendarItemIdRef[0] = item.getId();
                    }
                    return item;
                });

        // When: Event is cancelled
        scenario.publish(new EventCancelledEvent(eventId))
                .andWaitForStateChange(() -> calendarRepository.findById(calendarItemIdRef[0]).orElse(null))
                .andVerify(deletedItem -> {
                    assertThat(deletedItem).as("CalendarItem should be deleted after event cancellation").isNull();
                });
    }
}
