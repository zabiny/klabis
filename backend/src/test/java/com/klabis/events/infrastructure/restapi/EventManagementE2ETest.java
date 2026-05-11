package com.klabis.events.infrastructure.restapi;

import com.klabis.E2ETest;
import com.klabis.common.SecurityTestBase;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.events.EventTypeId;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventCreateEventBuilder;
import com.klabis.events.domain.EventUpdateEventBuilder;
import com.klabis.events.eventtype.application.EventTypeManagementPort;
import com.klabis.events.eventtype.domain.EventType;
import com.klabis.groups.application.LastOwnershipCheckerImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end tests for event management.
 * <p>
 * Tests complete event lifecycle including:
 * - Event creation with all fields
 * - Event lifecycle: DRAFT → ACTIVE → FINISHED
 * - Event lifecycle: DRAFT → CANCELLED
 * - Event updates with status constraints
 * - Event listing with pagination and filtering
 * - HATEOAS link generation based on status
 */
@E2ETest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Event Management E2E Tests")
@WithKlabisMockUser(username = "admin", authorities = {Authority.EVENTS_MANAGE, Authority.EVENTS_READ})
class EventManagementE2ETest extends SecurityTestBase {

    @MockitoBean
    @SuppressWarnings("unused")
    private LastOwnershipCheckerImpl lastOwnershipCheckerImpl;

    @MockitoBean
    @SuppressWarnings("unused")
    private com.klabis.groups.traininggroup.domain.TrainingGroupRepository trainingGroupRepository;

    @MockitoBean
    @SuppressWarnings("unused")
    private com.klabis.groups.familygroup.domain.FamilyGroupRepository familyGroupRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventManagementPort eventManagementPort;

    @Autowired
    private EventTypeManagementPort eventTypeManagementPort;

    @Test
    @DisplayName("Complete event lifecycle: create → publish → finish (via scheduler)")
    void shouldCompleteEventLifecycleFromCreateToFinish() throws Exception {
        // Given: Create an event with a past date so the scheduler will finish it
        LocalDate pastEventDate = LocalDate.now().minusDays(1);
        Event.CreateEvent createCommand = EventCreateEventBuilder.builder()
                .name("Spring Cup 2026")
                .eventDate(pastEventDate)
                .location("Forest Park")
                .organizer("OOB")
                .build();

        MvcResult createResult = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(createCommand))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        String eventId = extractEventIdFromLocation(createResult);

        // Verify event was created correctly
        mockMvc.perform(get("/api/events/{id}", eventId).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.name").value("Spring Cup 2026"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        // When: Publish the event (DRAFT → ACTIVE)
        mockMvc.perform(
                        post("/api/events/{id}/publish", eventId)
                                .contentType("application/json")
                )
                .andDo(print())
                .andExpect(status().isNoContent());

        // Verify ACTIVE status
        mockMvc.perform(get("/api/events/{id}", eventId).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // When: Finish the event via the scheduler path (ACTIVE → FINISHED)
        eventManagementPort.finishExpiredActiveEvents(LocalDate.now());

        // Then: Verify final state
        mockMvc.perform(
                        get("/api/events/{id}", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));
    }

    @Test
    @DisplayName("Complete event lifecycle: create → cancel")
    void shouldCompleteEventLifecycleFromCreateToCancel() throws Exception {
        // Given: Create an event
        Event.CreateEvent createCommand = EventCreateEventBuilder.builder()
                .name("Autumn Race 2026")
                .eventDate(LocalDate.of(2026, 10, 12))
                .location("City Park")
                .organizer("PRG")
                .build();

        MvcResult createResult = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(createCommand))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();

        String eventId = extractEventIdFromLocation(createResult);

        // When: Cancel the event (DRAFT → CANCELLED)
        mockMvc.perform(
                        post("/api/events/{id}/cancel", eventId)
                                .contentType("application/json")
                )
                .andDo(print())
                .andExpect(status().isNoContent());

        // Then: Verify final state
        mockMvc.perform(
                        get("/api/events/{id}", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("Event CRUD with optional websiteUrl")
    void shouldCreateEventWithOptionalWebsiteUrl() throws Exception {
        // Given: Create event with optional websiteUrl
        Event.CreateEvent createCommand = EventCreateEventBuilder.builder()
                .name("Summer Championship 2026")
                .eventDate(LocalDate.of(2026, 7, 20))
                .location("Mountain Valley")
                .organizer("BRN")
                .websiteUrl("https://example.com/summer-champ")
                .build();

        MvcResult createResult = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(createCommand))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();

        String eventId = extractEventIdFromLocation(createResult);

        // Verify creation
        mockMvc.perform(get("/api/events/{id}", eventId).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Summer Championship 2026"))
                .andExpect(jsonPath("$.location").value("Mountain Valley"))
                .andExpect(jsonPath("$.organizer").value("BRN"))
                .andExpect(jsonPath("$.websiteUrl").value("https://example.com/summer-champ"));

        // When: Update event with new values
        Event.UpdateEvent updateCommand = EventUpdateEventBuilder.builder()
                .name("Summer Championship 2026 (Updated)")
                .eventDate(LocalDate.of(2026, 7, 21))
                .location("Mountain Valley (Updated)")
                .organizer("OOB")
                .websiteUrl("https://example.com/summer-champ-updated")
                .build();

        mockMvc.perform(
                        patch("/api/events/{id}", eventId)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(updateCommand))
                )
                .andDo(print())
                .andExpect(status().isNoContent());

        // Then: Verify updated event
        mockMvc.perform(
                        get("/api/events/{id}", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Summer Championship 2026 (Updated)"))
                .andExpect(jsonPath("$.eventDate").value("2026-07-21"))
                .andExpect(jsonPath("$.location").value("Mountain Valley (Updated)"));
    }

    @Test
    @DisplayName("Event list with pagination and filtering by status")
    void shouldListEventsWithPaginationAndStatusFilter() throws Exception {
        // Given: Create multiple events with different statuses
        Event.CreateEvent draftEvent = EventCreateEventBuilder.builder()
                .name("Draft Event")
                .eventDate(LocalDate.of(2026, 5, 1))
                .location("Location 1")
                .organizer("OOB")
                .build();

        Event.CreateEvent activeEvent = EventCreateEventBuilder.builder()
                .name("Active Event")
                .eventDate(LocalDate.of(2026, 6, 1))
                .location("Location 2")
                .organizer("PRG")
                .build();

        // Create both events
        mockMvc.perform(
                post("/api/events")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(draftEvent))
        ).andExpect(status().isCreated());

        MvcResult activeResult = mockMvc.perform(
                post("/api/events")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(activeEvent))
        ).andExpect(status().isCreated()).andReturn();

        String activeEventId = extractEventIdFromLocation(activeResult);

        // Publish one event to ACTIVE
        mockMvc.perform(
                post("/api/events/{id}/publish", activeEventId)
                        .contentType("application/json")
        ).andExpect(status().isNoContent());

        // When: List all events
        mockMvc.perform(
                        get("/api/events")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").exists())
                .andExpect(jsonPath("$.page.totalElements").exists())
                .andExpect(jsonPath("$._embedded.eventSummaryDtoList").isArray());

        // When: Filter by ACTIVE status
        mockMvc.perform(
                        get("/api/events?status=ACTIVE")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.eventSummaryDtoList").isArray())
                .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Event list with filtering by organizer and date range")
    void shouldListEventsWithOrganizerAndDateRangeFilter() throws Exception {
        // Given: Create events for different organizers
        Event.CreateEvent oobEvent = EventCreateEventBuilder.builder()
                .name("OOB Event")
                .eventDate(LocalDate.of(2026, 3, 15))
                .location("Forest")
                .organizer("OOB")
                .build();

        Event.CreateEvent prgEvent = EventCreateEventBuilder.builder()
                .name("PRG Event")
                .eventDate(LocalDate.of(2026, 4, 20))
                .location("City")
                .organizer("PRG")
                .build();

        mockMvc.perform(
                post("/api/events")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(oobEvent))
        ).andExpect(status().isCreated());

        mockMvc.perform(
                post("/api/events")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(prgEvent))
        ).andExpect(status().isCreated());

        // When: Filter by organizer
        mockMvc.perform(
                        get("/api/events?organizer=OOB")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.eventSummaryDtoList").isArray())
                .andExpect(jsonPath("$._embedded.eventSummaryDtoList[?(@.organizer == 'OOB')]").exists());

        // When: Filter by date range
        mockMvc.perform(
                        get("/api/events?from=2026-03-01&to=2026-03-31")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.eventSummaryDtoList").isArray());
    }

    @Test
    @DisplayName("Event detail should show status-appropriate HAL+FORMS links")
    void shouldShowStatusAppropriateHateoasLinks() throws Exception {
        // Given: Create a DRAFT event
        Event.CreateEvent createCommand = EventCreateEventBuilder.builder()
                .name("Links Test Event")
                .eventDate(LocalDate.of(2026, 8, 10))
                .location("Test Location")
                .organizer("BRN")
                .build();

        MvcResult createResult = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(createCommand))
                )
                .andExpect(status().isCreated())
                .andReturn();

        String eventId = extractEventIdFromLocation(createResult);

        // When: Get DRAFT event detail
        mockMvc.perform(
                        get("/api/events/{id}", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._templates.updateEvent.method").value("PATCH"))  // EDIT
                .andExpect(jsonPath("$._templates.publishEvent").exists())
                .andExpect(jsonPath("$._templates.cancelEvent").exists())
                .andExpect(jsonPath("$._links.registrations.href").doesNotExist()); // registrations shouldnt be displayed on DRAFT event.

        // When: Publish to ACTIVE and check links
        mockMvc.perform(
                post("/api/events/{id}/publish", eventId)
                        .contentType("application/json")
        ).andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/events/{id}", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._templates.updateEvent.method").value("PATCH"))  // EDIT
                .andExpect(jsonPath("$._templates.cancelEvent").exists())
                .andExpect(jsonPath("$._templates.finishEvent").doesNotExist())
                .andExpect(jsonPath("$._links.registrations.href").exists());
    }

    @Test
    @DisplayName("GET /api/events/{id} should return 404 for non-existent event")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
    void shouldReturn404ForNonExistentEventId() throws Exception {
        // Given: A non-existent UUID
        String nonExistentId = UUID.randomUUID().toString();

        // When & Then: Get should return 404
        mockMvc.perform(
                        get("/api/events/{id}", nonExistentId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Event update should be rejected for FINISHED events")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
    void shouldRejectUpdateForFinishedEvent() throws Exception {
        // Given: Create and finish an event via the scheduler path
        LocalDate pastEventDate = LocalDate.now().minusDays(1);
        Event.CreateEvent createCommand = EventCreateEventBuilder.builder()
                .name("Immutable Event")
                .eventDate(pastEventDate)
                .location("Immutable Location")
                .organizer("OOB")
                .build();

        MvcResult createResult = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(createCommand))
                )
                .andExpect(status().isCreated())
                .andReturn();

        String eventId = extractEventIdFromLocation(createResult);

        mockMvc.perform(
                post("/api/events/{id}/publish", eventId)
                        .contentType("application/json")
        ).andExpect(status().isNoContent());

        eventManagementPort.finishExpiredActiveEvents(LocalDate.now());

        // When: Try to update finished event
        Event.UpdateEvent updateCommand = EventUpdateEventBuilder.builder()
                .name("Updated Name")
                .eventDate(LocalDate.of(2026, 9, 6))
                .location("Updated Location")
                .organizer("PRG")
                .build();

        // Then: Update should be rejected
        mockMvc.perform(
                        patch("/api/events/{id}", eventId)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(updateCommand))
                )
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should create and retrieve event without a location")
    void shouldCreateEventWithoutLocation() throws Exception {
        Event.CreateEvent createCommand = EventCreateEventBuilder.builder()
                .name("No Location Event")
                .eventDate(LocalDate.of(2026, 10, 1))
                .location(null)
                .organizer("OOB")
                .build();

        MvcResult createResult = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(createCommand))
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        String eventId = extractEventIdFromLocation(createResult);

        mockMvc.perform(
                        get("/api/events/{id}", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("No Location Event"))
                .andExpect(jsonPath("$.location").isEmpty());
    }

    @Test
    @DisplayName("Event with eventTypeId: create returns type in response and HAL link")
    void shouldCreateEventWithEventTypeAndExposeItInResponse() throws Exception {
        // Given — create an event type to reference
        EventType eventType = eventTypeManagementPort.createEventType(
                new EventType.CreateEventType("Trénink", "#0000ff", 1));
        EventTypeId eventTypeId = eventType.getId();

        Event.CreateEvent createCommand = EventCreateEventBuilder.builder()
                .name("Typed Training Event")
                .eventDate(LocalDate.of(2026, 8, 15))
                .location("Forest")
                .organizer("OOB")
                .eventTypeId(eventTypeId)
                .build();

        MvcResult createResult = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(createCommand))
                )
                .andExpect(status().isCreated())
                .andReturn();

        String eventId = extractEventIdFromLocation(createResult);

        // When: Retrieve the event
        mockMvc.perform(get("/api/events/{id}", eventId).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventTypeId").value(eventTypeId.value().toString()))
                .andExpect(jsonPath("$._links['event-type'].href").exists());
    }

    @Test
    @DisplayName("Event without eventTypeId: eventTypeId field and link absent from response")
    void shouldCreateEventWithoutEventTypeAndOmitFieldFromResponse() throws Exception {
        // Given
        Event.CreateEvent createCommand = EventCreateEventBuilder.builder()
                .name("Untyped Event")
                .eventDate(LocalDate.of(2026, 9, 10))
                .location("Park")
                .organizer("OOB")
                .build();

        MvcResult createResult = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(createCommand))
                )
                .andExpect(status().isCreated())
                .andReturn();

        String eventId = extractEventIdFromLocation(createResult);

        // When: Retrieve the event
        mockMvc.perform(get("/api/events/{id}", eventId).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventTypeId").doesNotExist())
                .andExpect(jsonPath("$._links['event-type']").doesNotExist());
    }

    @Test
    @DisplayName("Event update: change eventTypeId to a different type")
    void shouldUpdateEventTypeId() throws Exception {
        // Given — create two event types
        EventType typeA = eventTypeManagementPort.createEventType(
                new EventType.CreateEventType("Závod", "#ff0000", 1));
        EventType typeB = eventTypeManagementPort.createEventType(
                new EventType.CreateEventType("Sprint", "#00ff00", 2));

        Event.CreateEvent createCommand = EventCreateEventBuilder.builder()
                .name("Changeable Event")
                .eventDate(LocalDate.of(2026, 10, 5))
                .location("City")
                .organizer("OOB")
                .eventTypeId(typeA.getId())
                .build();

        MvcResult createResult = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(createCommand))
                )
                .andExpect(status().isCreated())
                .andReturn();

        String eventId = extractEventIdFromLocation(createResult);

        // When: Update eventTypeId to typeB
        Event.UpdateEvent updateCommand = EventUpdateEventBuilder.builder()
                .name("Changeable Event")
                .eventDate(LocalDate.of(2026, 10, 5))
                .location("City")
                .organizer("OOB")
                .eventTypeId(typeB.getId())
                .build();

        mockMvc.perform(
                        patch("/api/events/{id}", eventId)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(updateCommand))
                )
                .andExpect(status().isNoContent());

        // Then: Response reflects updated eventTypeId
        mockMvc.perform(get("/api/events/{id}", eventId).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventTypeId").value(typeB.getId().value().toString()));
    }

    @Test
    @DisplayName("Event update: clear eventTypeId by setting it to null")
    void shouldClearEventTypeIdOnUpdate() throws Exception {
        // Given — create event with a type
        EventType eventType = eventTypeManagementPort.createEventType(
                new EventType.CreateEventType("Orientační závod", "#ff8800", 3));

        Event.CreateEvent createCommand = EventCreateEventBuilder.builder()
                .name("Event To Clear Type")
                .eventDate(LocalDate.of(2026, 11, 1))
                .location("Woods")
                .organizer("OOB")
                .eventTypeId(eventType.getId())
                .build();

        MvcResult createResult = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(createCommand))
                )
                .andExpect(status().isCreated())
                .andReturn();

        String eventId = extractEventIdFromLocation(createResult);

        // When: Update with null eventTypeId (clear the type)
        Event.UpdateEvent updateCommand = EventUpdateEventBuilder.builder()
                .name("Event To Clear Type")
                .eventDate(LocalDate.of(2026, 11, 1))
                .location("Woods")
                .organizer("OOB")
                .eventTypeId(null)
                .build();

        mockMvc.perform(
                        patch("/api/events/{id}", eventId)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(updateCommand))
                )
                .andExpect(status().isNoContent());

        // Then: eventTypeId is absent from the response
        mockMvc.perform(get("/api/events/{id}", eventId).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventTypeId").doesNotExist())
                .andExpect(jsonPath("$._links['event-type']").doesNotExist());
    }

    private String extractEventIdFromLocation(MvcResult result) {
        String location = result.getResponse().getHeader("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }
}
