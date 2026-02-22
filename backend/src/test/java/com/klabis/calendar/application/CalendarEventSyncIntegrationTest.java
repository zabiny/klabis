package com.klabis.calendar.application;

import com.klabis.calendar.domain.CalendarItem;
import com.klabis.calendar.domain.CalendarItemId;
import com.klabis.calendar.infrastructure.jdbc.CalendarRepository;
import com.klabis.calendar.infrastructure.jdbc.EventDataProvider;
import com.klabis.events.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.PublishedEvents;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional
@DisplayName("Calendar Event Synchronization Integration Tests")
class CalendarEventSyncIntegrationTest {

    private static final EventId EVENT_ID = new EventId(UUID.fromString("f1d7fcda-024e-42ad-9c08-fc20d07a166d"));

    @Autowired
    private CalendarRepository calendarRepository;

    @MockitoBean
    private EventDataProvider eventDataProviderMock;

    // data as they stand in the DB from @Sql annotation on class
    private final EventData INITIAL_EVENT_DATA = new EventData(
            "Spring Boot Workshop",
            LocalDate.of(2024, 3, 15),
            "Prague CC",
            "OOB",
            WebsiteUrl.of("https://example.com/workshop")
    );

    @Test
    @DisplayName("should create calendar item when EventPublishedEvent arrives")
    void shouldCreateCalendarItemWhenEventIsPublished(Scenario scenario, PublishedEvents events) {
        // Given: Event data
        final EventId eventId = EVENT_ID;

        when(eventDataProviderMock.getEventData(eventId)).thenReturn(INITIAL_EVENT_DATA);

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
        final EventId eventId = new EventId(UUID.fromString("4d5db31b-01f8-4e1d-b529-1b3b17eca5e0"));

        // First, create the calendar item
        calendarRepository.save(CalendarItem.createForEvent("Test", "Something", LocalDate.now(), eventId));

        // When: Event is updated
        when(eventDataProviderMock.getEventData(eventId)).thenReturn(INITIAL_EVENT_DATA);
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

        final CalendarItemId calendarItemId = calendarRepository.save(CalendarItem.createForEvent("Test", "Something", LocalDate.now(), eventId)).getId();

        // When: Event is cancelled
        scenario.publish(new EventCancelledEvent(eventId))
                .andWaitForStateChange(() -> calendarRepository.findByEventId(eventId).isEmpty())
                .andVerify(calendarItemIsGone -> {
                    assertThat(calendarRepository.findById(calendarItemId)).isEmpty();
                });
    }
}
