package com.klabis.calendar.application;

import com.klabis.CleanupTestData;
import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.CalendarItemKind;
import com.klabis.calendar.domain.CalendarRepository;
import com.klabis.calendar.domain.EventCalendarItem;
import com.klabis.events.*;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
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
 * - CalendarItem creation when Event is published (with and without deadline)
 * - CalendarItem update when Event is updated (rename, add/clear deadline)
 * - CalendarItem deletion when Event is cancelled
 * - Description formatting (location + " - " + organizer + optional website URL)
 * - Idempotent behavior (no duplicate calendar items)
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.STANDALONE)
@ActiveProfiles("test")
@CleanupTestData
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
    void shouldCreateCalendarItemWhenEventIsPublished(Scenario scenario) {
        // Given: Event data
        final EventId eventId = EVENT_ID;

        when(eventDataProviderMock.getEventData(eventId)).thenReturn(new EventData(
                "Spring Boot Workshop",
                LocalDate.of(2024, 3, 15),
                "Prague CC",
                "OOB",
                "https://example.com/workshop",
                null
        ));

        // When & Then: CalendarItem should be created automatically
        scenario.publish(EventPublishedEvent.fromAggregate(Event.reconstruct(eventId, "Spring Boot Workshop", LocalDate.of(2024, 3, 15), "Prague CC", "OOB", WebsiteUrl.of("https://example.com/workshop"), null, null, EventStatus.ACTIVE, null, List.of(), List.of(), null)))
                .andWaitForStateChange(() -> !calendarRepository.findByEventId(eventId).isEmpty())
                .andVerify(isPresent -> {
                    EventCalendarItem calendarItem = calendarRepository.findByEventId(eventId).stream()
                            .filter(EventCalendarItem.class::isInstance)
                            .map(EventCalendarItem.class::cast)
                            .findFirst().orElseThrow();
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
                "https://new-url.com",
                null
        ));
        scenario.publish(new EventUpdatedEvent(
                        java.util.UUID.randomUUID(),
                        eventId,
                        "Updated Workshop",
                        LocalDate.of(2024, 5, 21),
                        "New Loc",
                        "NewOrg",
                        WebsiteUrl.of("https://new-url.com"),
                        java.util.List.of(),
                        java.time.Instant.now()
                ))
                .andWaitForStateChange(() -> calendarRepository.findByEventId(eventId).stream()
                                .filter(EventCalendarItem.class::isInstance)
                                .findFirst().orElse(null),
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

        final CalendarItemId calendarItemId = calendarRepository.findByEventId(eventId).stream()
                .filter(EventCalendarItem.class::isInstance)
                .findFirst().orElseThrow().getId();

        // When: Event is cancelled
        scenario.publish(EventCancelledEvent.fromAggregate(Event.reconstruct(eventId, "Test", LocalDate.now(), "Location", "OOB", null, null, null, EventStatus.CANCELLED, null, List.of(), List.of(), null)))
                .andWaitForStateChange(() -> calendarRepository.findByEventId(eventId).isEmpty())
                .andVerify(calendarItemIsGone -> {
                    assertThat(calendarRepository.findById(calendarItemId)).isEmpty();
                });
    }

    @Nested
    @DisplayName("Registration deadline scenarios")
    class RegistrationDeadlineScenarios {

        static final LocalDate EVENT_DATE = LocalDate.of(2025, 6, 14);
        static final LocalDate DEADLINE_DATE = LocalDate.of(2025, 6, 7);

        @Test
        @DisplayName("publish event without deadline creates exactly one EVENT_DATE item")
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, statements = "delete from calendar_items")
        void publishWithoutDeadlineCreatesOneEventDateItem(Scenario scenario) {
            final EventId eventId = EVENT_ID;

            when(eventDataProviderMock.getEventData(eventId)).thenReturn(new EventData(
                    "Jarní sprint",
                    EVENT_DATE,
                    "Les Brdy",
                    "OOB",
                    null,
                    null
            ));

            scenario.publish(EventPublishedEvent.fromAggregate(Event.reconstruct(
                            eventId, "Jarní sprint", EVENT_DATE, "Les Brdy", "OOB",
                            null, null, null, EventStatus.ACTIVE, null, List.of(), List.of(), null)))
                    .andWaitForStateChange(() -> !calendarRepository.findByEventId(eventId).isEmpty())
                    .andVerify(ignored -> {
                        List<EventCalendarItem> items = findEventItems(eventId);
                        assertThat(items).hasSize(1);
                        assertThat(items.get(0).getKind()).isEqualTo(CalendarItemKind.EVENT_DATE);
                    });
        }

        @Test
        @DisplayName("publish event with deadline creates EVENT_DATE and EVENT_REGISTRATION_DATE items")
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, statements = "delete from calendar_items")
        void publishWithDeadlineCreatesTwoItems(Scenario scenario) {
            final EventId eventId = EVENT_ID;

            when(eventDataProviderMock.getEventData(eventId)).thenReturn(new EventData(
                    "Jarní sprint",
                    EVENT_DATE,
                    "Les Brdy",
                    "OOB",
                    null,
                    DEADLINE_DATE
            ));

            scenario.publish(EventPublishedEvent.fromAggregate(Event.reconstruct(
                            eventId, "Jarní sprint", EVENT_DATE, "Les Brdy", "OOB",
                            null, null, DEADLINE_DATE, EventStatus.ACTIVE, null, List.of(), List.of(), null)))
                    .andWaitForStateChange(() -> calendarRepository.findByEventId(eventId).size() >= 2)
                    .andVerify(ignored -> {
                        List<EventCalendarItem> items = findEventItems(eventId);
                        assertThat(items).hasSize(2);
                        assertThat(items)
                                .extracting(EventCalendarItem::getKind)
                                .containsExactlyInAnyOrder(
                                        CalendarItemKind.EVENT_DATE,
                                        CalendarItemKind.EVENT_REGISTRATION_DATE);
                    });
        }

        @Test
        @DisplayName("update event to add deadline creates EVENT_REGISTRATION_DATE item alongside existing EVENT_DATE")
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, statements = "delete from calendar_items")
        @Sql(config = @SqlConfig(separator = ";"), statements = "INSERT INTO calendar_items (id, kind, name, description, start_date, end_date, event_id, created_at, created_by, modified_at, last_modified_by, version) VALUES ('4d5db31b-01f8-4e1d-b529-1b3b17eca5e0', 'EVENT_DATE', 'Jarní sprint', 'Les Brdy - OOB', '2025-06-14', '2025-06-14', 'f1d7fcda-024e-42ad-9c08-fc20d07a166d', CURRENT_TIMESTAMP, 'test-setup', CURRENT_TIMESTAMP, 'test-setup', 0)")
        void updateToAddDeadlineCreatesTwoItems(Scenario scenario) {
            final EventId eventId = EVENT_ID;

            when(eventDataProviderMock.getEventData(eventId)).thenReturn(new EventData(
                    "Jarní sprint",
                    EVENT_DATE,
                    "Les Brdy",
                    "OOB",
                    null,
                    DEADLINE_DATE
            ));

            scenario.publish(new EventUpdatedEvent(
                            UUID.randomUUID(), eventId, "Jarní sprint", EVENT_DATE,
                            "Les Brdy", "OOB", null, List.of(), java.time.Instant.now()))
                    .andWaitForStateChange(() -> calendarRepository.findByEventId(eventId).size() >= 2)
                    .andVerify(ignored -> {
                        List<EventCalendarItem> items = findEventItems(eventId);
                        assertThat(items).hasSize(2);
                        assertThat(items)
                                .extracting(EventCalendarItem::getKind)
                                .containsExactlyInAnyOrder(
                                        CalendarItemKind.EVENT_DATE,
                                        CalendarItemKind.EVENT_REGISTRATION_DATE);

                        EventCalendarItem deadlineItem = items.stream()
                                .filter(i -> i.getKind() == CalendarItemKind.EVENT_REGISTRATION_DATE)
                                .findFirst().orElseThrow();
                        assertThat(deadlineItem.getName()).isEqualTo("Přihlášky - Jarní sprint");
                        assertThat(deadlineItem.getStartDate()).isEqualTo(DEADLINE_DATE);
                        assertThat(deadlineItem.getEndDate()).isEqualTo(DEADLINE_DATE);
                    });
        }

        @Test
        @DisplayName("update event to clear deadline removes EVENT_REGISTRATION_DATE, keeps EVENT_DATE")
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, statements = "delete from calendar_items")
        @Sql(config = @SqlConfig(separator = ";"), statements = """
                INSERT INTO calendar_items (id, kind, name, description, start_date, end_date, event_id, created_at, created_by, modified_at, last_modified_by, version)
                VALUES ('4d5db31b-01f8-4e1d-b529-1b3b17eca5e0', 'EVENT_DATE', 'Jarní sprint', 'Les Brdy - OOB', '2025-06-14', '2025-06-14', 'f1d7fcda-024e-42ad-9c08-fc20d07a166d', CURRENT_TIMESTAMP, 'test-setup', CURRENT_TIMESTAMP, 'test-setup', 0);
                INSERT INTO calendar_items (id, kind, name, description, start_date, end_date, event_id, created_at, created_by, modified_at, last_modified_by, version)
                VALUES ('7a9e2c11-bbbb-4321-9876-aabbccddeeff', 'EVENT_REGISTRATION_DATE', 'Přihlášky - Jarní sprint', NULL, '2025-06-07', '2025-06-07', 'f1d7fcda-024e-42ad-9c08-fc20d07a166d', CURRENT_TIMESTAMP, 'test-setup', CURRENT_TIMESTAMP, 'test-setup', 0)
                """)
        void updateToClearDeadlineRemovesDeadlineItem(Scenario scenario) {
            final EventId eventId = EVENT_ID;
            final CalendarItemId deadlineItemId = new CalendarItemId(UUID.fromString("7a9e2c11-bbbb-4321-9876-aabbccddeeff"));

            assertThat(calendarRepository.findByEventId(eventId)).hasSize(2);

            when(eventDataProviderMock.getEventData(eventId)).thenReturn(new EventData(
                    "Jarní sprint",
                    EVENT_DATE,
                    "Les Brdy",
                    "OOB",
                    null,
                    null
            ));

            scenario.publish(new EventUpdatedEvent(
                            UUID.randomUUID(), eventId, "Jarní sprint", EVENT_DATE,
                            "Les Brdy", "OOB", null, List.of(), java.time.Instant.now()))
                    .andWaitForStateChange(() -> calendarRepository.findByEventId(eventId).size() == 1)
                    .andVerify(ignored -> {
                        List<EventCalendarItem> items = findEventItems(eventId);
                        assertThat(items).hasSize(1);
                        assertThat(items.get(0).getKind()).isEqualTo(CalendarItemKind.EVENT_DATE);
                        assertThat(calendarRepository.findById(deadlineItemId)).isEmpty();
                    });
        }

        @Test
        @DisplayName("update event name propagates to both EVENT_DATE and EVENT_REGISTRATION_DATE labels")
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, statements = "delete from calendar_items")
        @Sql(config = @SqlConfig(separator = ";"), statements = """
                INSERT INTO calendar_items (id, kind, name, description, start_date, end_date, event_id, created_at, created_by, modified_at, last_modified_by, version)
                VALUES ('4d5db31b-01f8-4e1d-b529-1b3b17eca5e0', 'EVENT_DATE', 'Jarní sprint', 'Les Brdy - OOB', '2025-06-14', '2025-06-14', 'f1d7fcda-024e-42ad-9c08-fc20d07a166d', CURRENT_TIMESTAMP, 'test-setup', CURRENT_TIMESTAMP, 'test-setup', 0);
                INSERT INTO calendar_items (id, kind, name, description, start_date, end_date, event_id, created_at, created_by, modified_at, last_modified_by, version)
                VALUES ('7a9e2c11-bbbb-4321-9876-aabbccddeeff', 'EVENT_REGISTRATION_DATE', 'Přihlášky - Jarní sprint', NULL, '2025-06-07', '2025-06-07', 'f1d7fcda-024e-42ad-9c08-fc20d07a166d', CURRENT_TIMESTAMP, 'test-setup', CURRENT_TIMESTAMP, 'test-setup', 0)
                """)
        void renameEventUpdatesBothItemLabels(Scenario scenario) {
            final EventId eventId = EVENT_ID;
            final String newName = "Jarní sprint 2025";

            when(eventDataProviderMock.getEventData(eventId)).thenReturn(new EventData(
                    newName,
                    EVENT_DATE,
                    "Les Brdy",
                    "OOB",
                    null,
                    DEADLINE_DATE
            ));

            scenario.publish(new EventUpdatedEvent(
                            UUID.randomUUID(), eventId, newName, EVENT_DATE,
                            "Les Brdy", "OOB", null, List.of(), java.time.Instant.now()))
                    .andWaitForStateChange(() -> {
                        List<EventCalendarItem> items = findEventItems(eventId);
                        return items.stream().anyMatch(i -> i.getKind() == CalendarItemKind.EVENT_DATE
                                && newName.equals(i.getName()));
                    })
                    .andVerify(ignored -> {
                        List<EventCalendarItem> items = findEventItems(eventId);
                        assertThat(items).hasSize(2);

                        EventCalendarItem eventDateItem = items.stream()
                                .filter(i -> i.getKind() == CalendarItemKind.EVENT_DATE)
                                .findFirst().orElseThrow();
                        assertThat(eventDateItem.getName()).isEqualTo(newName);

                        EventCalendarItem deadlineItem = items.stream()
                                .filter(i -> i.getKind() == CalendarItemKind.EVENT_REGISTRATION_DATE)
                                .findFirst().orElseThrow();
                        assertThat(deadlineItem.getName()).isEqualTo("Přihlášky - " + newName);
                    });
        }

        @Test
        @DisplayName("cancel event with two items removes both")
        @Sql(statements = """
                INSERT INTO calendar_items (id, kind, name, description, start_date, end_date, event_id, created_at, created_by, modified_at, last_modified_by, version)
                VALUES ('4d5db31b-01f8-4e1d-b529-1b3b17eca5e0', 'EVENT_DATE', 'Jarní sprint', 'Les Brdy - OOB', '2025-06-14', '2025-06-14', 'f1d7fcda-024e-42ad-9c08-fc20d07a166d', CURRENT_TIMESTAMP, 'test-setup', CURRENT_TIMESTAMP, 'test-setup', 0);
                INSERT INTO calendar_items (id, kind, name, description, start_date, end_date, event_id, created_at, created_by, modified_at, last_modified_by, version)
                VALUES ('7a9e2c11-bbbb-4321-9876-aabbccddeeff', 'EVENT_REGISTRATION_DATE', 'Přihlášky - Jarní sprint', NULL, '2025-06-07', '2025-06-07', 'f1d7fcda-024e-42ad-9c08-fc20d07a166d', CURRENT_TIMESTAMP, 'test-setup', CURRENT_TIMESTAMP, 'test-setup', 0)
                """)
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, statements = "delete from calendar_items")
        void cancelEventWithTwoItemsRemovesBoth(Scenario scenario) {
            final EventId eventId = EVENT_ID;

            assertThat(calendarRepository.findByEventId(eventId)).hasSize(2);

            scenario.publish(EventCancelledEvent.fromAggregate(Event.reconstruct(
                            eventId, "Jarní sprint", EVENT_DATE, "Les Brdy", "OOB",
                            null, null, null, EventStatus.CANCELLED, null, List.of(), List.of(), null)))
                    .andWaitForStateChange(() -> calendarRepository.findByEventId(eventId).isEmpty())
                    .andVerify(ignored -> assertThat(calendarRepository.findByEventId(eventId)).isEmpty());
        }

        private List<EventCalendarItem> findEventItems(EventId eventId) {
            return calendarRepository.findByEventId(eventId).stream()
                    .filter(EventCalendarItem.class::isInstance)
                    .map(EventCalendarItem.class::cast)
                    .toList();
        }
    }
}
