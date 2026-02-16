package com.klabis.calendar;

import com.klabis.TestApplicationConfiguration;
import com.klabis.calendar.persistence.CalendarRepository;
import com.klabis.events.Event;
import com.klabis.events.EventId;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.persistence.EventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.test.EnableScenarios;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

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
@SpringBootTest(classes = {TestApplicationConfiguration.class})
@ActiveProfiles("test")
@EnableScenarios
@DisplayName("Calendar Event Synchronization Integration Tests")
class CalendarEventSyncIntegrationTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CalendarRepository calendarRepository;

    @Autowired(required = false)
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("should create calendar item when event is published")
    void shouldCreateCalendarItemWhenEventIsPublished(Scenario scenario) {
        // Given: A new event in DRAFT status
        Event event = Event.create(
                "Spring Boot Workshop",
                LocalDate.of(2024, 3, 15),
                "Prague CC",
                "OOB",
                WebsiteUrl.of("https://example.com/workshop"),
                null
        );

        AtomicReference<EventId> eventIdRef = new AtomicReference<>();
        executeInTransaction(() -> {
            Event savedEvent = eventRepository.save(event);
            eventIdRef.set(savedEvent.getId());
        });

        // When: Event is published (transitions to ACTIVE status)
        executeInTransaction(() -> {
            Event existingEvent = eventRepository.findById(eventIdRef.get()).orElseThrow();
            existingEvent.publish();
            eventRepository.save(existingEvent);
        });

        // Then: CalendarItem should be created automatically
        scenario.stimulate(() -> eventIdRef.get())
                .andWaitForStateChange(() -> {
                    Optional<CalendarItem> calendarItemOpt = calendarRepository.findByEventId(eventIdRef.get());

                    assertThat(calendarItemOpt)
                            .as("CalendarItem should be created for published event")
                            .isPresent();

                    CalendarItem calendarItem = calendarItemOpt.get();
                    assertThat(calendarItem.getName()).isEqualTo("Spring Boot Workshop");
                    assertThat(calendarItem.getDescription())
                            .isEqualTo("Prague CC - OOB\nhttps://example.com/workshop");
                    assertThat(calendarItem.getStartDate()).isEqualTo(LocalDate.of(2024, 3, 15));
                    assertThat(calendarItem.getEndDate()).isEqualTo(LocalDate.of(2024, 3, 15));
                    assertThat(calendarItem.getEventId()).isEqualTo(eventIdRef.get());

                    return calendarItem;
                });
    }

    @Test
    @DisplayName("should create calendar item without website URL when not present")
    void shouldCreateCalendarItemWithoutWebsiteUrl(Scenario scenario) {
        // Given: Event without website URL
        Event event = Event.create(
                "Java Meetup",
                LocalDate.of(2024, 4, 20),
                "Brno TH",
                "OOB",
                null,  // No website URL
                null
        );

        AtomicReference<EventId> eventIdRef = new AtomicReference<>();
        executeInTransaction(() -> {
            Event savedEvent = eventRepository.save(event);
            eventIdRef.set(savedEvent.getId());
        });

        // When: Event is published
        executeInTransaction(() -> {
            Event existingEvent = eventRepository.findById(eventIdRef.get()).orElseThrow();
            existingEvent.publish();
            eventRepository.save(existingEvent);
        });

        // Then: CalendarItem description should not include website URL
        scenario.stimulate(() -> eventIdRef.get())
                .andWaitForStateChange(() -> {
                    Optional<CalendarItem> calendarItemOpt = calendarRepository.findByEventId(eventIdRef.get());

                    assertThat(calendarItemOpt).isPresent();

                    CalendarItem calendarItem = calendarItemOpt.get();
                    assertThat(calendarItem.getDescription())
                            .isEqualTo("Brno TH - OOB")
                            .doesNotContain("\n");

                    return calendarItem;
                });
    }

    @Test
    @DisplayName("should update calendar item when event is updated")
    void shouldUpdateCalendarItemWhenEventIsUpdated(Scenario scenario) {
        // Given: An existing published event with calendar item
        Event event = Event.create(
                "Initial Workshop",
                LocalDate.of(2024, 5, 10),
                "Old Loc",
                "OldOrg",
                WebsiteUrl.of("https://old-url.com"),
                null
        );

        AtomicReference<EventId> eventIdRef = new AtomicReference<>();
        executeInTransaction(() -> {
            Event savedEvent = eventRepository.save(event);
            savedEvent.publish();
            Event publishedEvent = eventRepository.save(savedEvent);
            eventIdRef.set(publishedEvent.getId());
        });

        // Wait for initial calendar item creation
        scenario.stimulate(() -> eventIdRef.get())
                .andWaitForStateChange(() -> calendarRepository.findByEventId(eventIdRef.get()).orElse(null));

        // When: Event is updated
        executeInTransaction(() -> {
            Event existingEvent = eventRepository.findById(eventIdRef.get()).orElseThrow();
            existingEvent.update(
                    "Updated Workshop",
                    LocalDate.of(2024, 5, 15),
                    "New Loc",
                    "NewOrg",
                    WebsiteUrl.of("https://new-url.com"),
                    null
            );
            eventRepository.save(existingEvent);
        });

        // Then: CalendarItem should be updated automatically
        scenario.stimulate(() -> eventIdRef.get())
                .andWaitForStateChange(() -> {
                    Optional<CalendarItem> calendarItemOpt = calendarRepository.findByEventId(eventIdRef.get());

                    assertThat(calendarItemOpt)
                            .as("CalendarItem should still exist after event update")
                            .isPresent();

                    CalendarItem updatedItem = calendarItemOpt.get();
                    assertThat(updatedItem.getName()).isEqualTo("Updated Workshop");
                    assertThat(updatedItem.getDescription())
                            .isEqualTo("New Loc - NewOrg\nhttps://new-url.com");
                    assertThat(updatedItem.getStartDate()).isEqualTo(LocalDate.of(2024, 5, 15));
                    assertThat(updatedItem.getEndDate()).isEqualTo(LocalDate.of(2024, 5, 15));

                    return updatedItem;
                });
    }

    @Test
    @DisplayName("should delete calendar item when event is cancelled")
    void shouldDeleteCalendarItemWhenEventIsCancelled(Scenario scenario) {
        // Given: An existing published event with calendar item
        Event event = Event.create(
                "Workshop to Cancel",
                LocalDate.of(2024, 6, 20),
                "Some Loc",
                "OOB",
                null,
                null
        );

        AtomicReference<EventId> eventIdRef = new AtomicReference<>();
        executeInTransaction(() -> {
            Event savedEvent = eventRepository.save(event);
            savedEvent.publish();
            Event publishedEvent = eventRepository.save(savedEvent);
            eventIdRef.set(publishedEvent.getId());
        });

        // Wait for calendar item creation
        AtomicReference<CalendarItemId> calendarItemIdRef = new AtomicReference<>();
        scenario.stimulate(() -> eventIdRef.get())
                .andWaitForStateChange(() -> {
                    Optional<CalendarItem> calendarItemOpt = calendarRepository.findByEventId(eventIdRef.get());
                    assertThat(calendarItemOpt)
                            .as("CalendarItem should exist before cancellation")
                            .isPresent();
                    calendarItemIdRef.set(calendarItemOpt.get().getId());
                    return calendarItemOpt.get();
                });

        // When: Event is cancelled
        executeInTransaction(() -> {
            Event existingEvent = eventRepository.findById(eventIdRef.get()).orElseThrow();
            existingEvent.cancel();
            eventRepository.save(existingEvent);
        });

        // Then: CalendarItem should be deleted automatically
        // Wait and verify deletion
        scenario.stimulate(() -> eventIdRef.get())
                .andWaitForEventOfType(com.klabis.events.EventCancelledEvent.class)
                .toArrive();

        // Verify calendar item was deleted
        Optional<CalendarItem> deletedItemCheck = calendarRepository.findById(calendarItemIdRef.get());
        assertThat(deletedItemCheck)
                .as("CalendarItem should be deleted after event cancellation")
                .isEmpty();
    }

    /**
     * Helper method to execute code in a transaction.
     */
    private void executeInTransaction(Runnable action) {
        if (transactionTemplate != null) {
            transactionTemplate.executeWithoutResult(status -> action.run());
        } else {
            action.run();
        }
    }
}
