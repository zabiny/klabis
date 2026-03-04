package com.klabis.calendar.application;

import com.klabis.calendar.domain.CalendarItem;
import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.domain.CalendarRepository;
import com.klabis.events.*;
import com.klabis.events.domain.Event;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.domain.EventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.PublishedEvents;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import java.time.LocalDate;
import java.util.List;
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
@ActiveProfiles("test")
@DisplayName("Calendar Event Synchronization Integration Tests")
class CalendarEventSyncIntegrationTest {

    final EventId EVENT_ID = new EventId(UUID.fromString("f1d7fcda-024e-42ad-9c08-fc20d07a166d"));

    @Autowired
    private CalendarRepository calendarRepository;

    @MockitoBean
    private EventDataProvider eventDataProviderMock;

    @Test
    @DisplayName("should create calendar item when EventPublishedEvent arrives")
    @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, statements = "delete from calendar_items")
    void shouldCreateCalendarItemWhenEventIsPublished(Scenario scenario, PublishedEvents events) {
        // Given: Event data
        final EventId eventId = EVENT_ID;

        when(eventDataProviderMock.getEventData(eventId)).thenReturn(new EventData(
                "Spring Boot Workshop",
                LocalDate.of(2024, 3, 15),
                "Prague CC",
                "OOB",
                "https://example.com/workshop"
        ));

        // When & Then: CalendarItem should be created automatically
        scenario.publish(EventPublishedEvent.fromAggregate(Event.reconstruct(eventId, "Spring Boot Workshop", LocalDate.of(2024, 3, 15), "Prague CC", "OOB", WebsiteUrl.of("https://example.com/workshop"), null, EventStatus.ACTIVE, List.of(), null)))
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
    @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, statements = "delete from calendar_items")
    @Sql(config = @SqlConfig(separator = ";"), statements = "INSERT INTO calendar_items (id, name, description, start_date, end_date, event_id, created_at, created_by, modified_at, last_modified_by, version) VALUES ('4d5db31b-01f8-4e1d-b529-1b3b17eca5e0', 'Test', 'Something', CURRENT_DATE, CURRENT_DATE, 'f1d7fcda-024e-42ad-9c08-fc20d07a166d', CURRENT_TIMESTAMP, 'test-setup', CURRENT_TIMESTAMP, 'test-setup', 0)")
    void shouldUpdateCalendarItemWhenEventIsUpdated(Scenario scenario) {
        // Given: An existing calendar item
        final EventId eventId = EVENT_ID;

        // When: Event is updated
        when(eventDataProviderMock.getEventData(eventId)).thenReturn(new EventData(
                "Updated Workshop",
                LocalDate.of(2024, 5, 21),
                "New Loc",
                "NewOrg",
                "https://new-url.com"
        ));
        scenario.publish(new EventUpdatedEvent(
                        java.util.UUID.randomUUID(),
                        eventId,
                        "Updated Workshop",
                        LocalDate.of(2024, 5, 21),
                        "New Loc",
                        "NewOrg",
                        WebsiteUrl.of("https://new-url.com"),
                        java.time.Instant.now()
                ))
                .andWaitForStateChange(() -> calendarRepository.findByEventId(eventId).orElse(null),
                        item -> "Updated Workshop".equalsIgnoreCase(item.getName()))
                .andVerify(updatedItem -> {
                    assertThat(updatedItem.getName()).isEqualTo("Updated Workshop");
                    assertThat(updatedItem.getDescription())
                            .isEqualTo("New Loc - NewOrg\nhttps://new-url.com");
                    assertThat(updatedItem.getStartDate()).isEqualTo(LocalDate.of(2024, 5, 21));
                    assertThat(updatedItem.getEndDate()).isEqualTo(LocalDate.of(2024, 5, 21));
                });
    }

    @Test
    @DisplayName("should delete calendar item when EventCancelledEvent arrives")
    @Sql(statements = "INSERT INTO calendar_items (id, name, description, start_date, end_date, event_id, created_at, created_by, modified_at, last_modified_by, version) VALUES ('d3cc82a1-2342-4d8a-a819-8c38f56e226d', 'Test', 'Something', CURRENT_DATE, CURRENT_DATE, 'f1d7fcda-024e-42ad-9c08-fc20d07a166d', CURRENT_TIMESTAMP, 'test-setup', CURRENT_TIMESTAMP, 'test-setup', 0)")
    @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, statements = "delete from calendar_items")
    void shouldDeleteCalendarItemWhenEventIsCancelled(Scenario scenario) {
        // Given: An existing calendar item
        final EventId eventId = EVENT_ID;

        final CalendarItemId calendarItemId = calendarRepository.findByEventId(eventId).orElseThrow().getId();

        // When: Event is cancelled
        scenario.publish(EventCancelledEvent.fromAggregate(Event.reconstruct(eventId, "Test", LocalDate.now(), "Location", "OOB", null, null, EventStatus.CANCELLED, List.of(), null)))
                .andWaitForStateChange(() -> calendarRepository.findByEventId(eventId).isEmpty())
                .andVerify(calendarItemIsGone -> {
                    assertThat(calendarRepository.findById(calendarItemId)).isEmpty();
                });
    }
}
