package com.klabis.events.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.E2ETest;
import com.klabis.common.SecurityTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
@WithMockUser(username = "admin", authorities = {EventManagementE2ETest.EVENTS_MANAGE_AUTHORITY, EventManagementE2ETest.EVENTS_READ_AUTHORITY})
class EventManagementE2ETest extends SecurityTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    static final String EVENTS_MANAGE_AUTHORITY = "EVENTS:MANAGE";
    static final String EVENTS_READ_AUTHORITY = "EVENTS:READ";

    @Test
    @DisplayName("Complete event lifecycle: create → publish → finish")
    void shouldCompleteEventLifecycleFromCreateToFinish() throws Exception {
        // Given: Create an event
        CreateEventCommand createCommand = new CreateEventCommand(
                "Spring Cup 2026",
                LocalDate.of(2026, 3, 15),
                "Forest Park",
                "OOB",
                "https://example.com/spring-cup",
                null
        );

        MvcResult createResult = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(createCommand))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.name").value("Spring Cup 2026"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();

        String eventId = extractEventId(createResult);

        // When: Publish the event (DRAFT → ACTIVE)
        mockMvc.perform(
                        post("/api/events/{id}/publish", eventId)
                                .contentType("application/json")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // When: Finish the event (ACTIVE → FINISHED)
        mockMvc.perform(
                        post("/api/events/{id}/finish", eventId)
                                .contentType("application/json")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.status").value("FINISHED"));

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
        CreateEventCommand createCommand = new CreateEventCommand(
                "Autumn Race 2026",
                LocalDate.of(2026, 10, 12),
                "City Park",
                "PRG",
                null,
                null
        );

        MvcResult createResult = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(createCommand))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();

        String eventId = extractEventId(createResult);

        // When: Cancel the event (DRAFT → CANCELLED)
        mockMvc.perform(
                        post("/api/events/{id}/cancel", eventId)
                                .contentType("application/json")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

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
        // Given: Create event with optional websiteUrl (no coordinator due to FK constraint)
        CreateEventCommand createCommand = new CreateEventCommand(
                "Summer Championship 2026",
                LocalDate.of(2026, 7, 20),
                "Mountain Valley",
                "BRN",
                "https://example.com/summer-champ",
                null  // no coordinator - would require member in DB
        );

        MvcResult createResult = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(createCommand))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Summer Championship 2026"))
                .andExpect(jsonPath("$.location").value("Mountain Valley"))
                .andExpect(jsonPath("$.organizer").value("BRN"))
                .andExpect(jsonPath("$.websiteUrl").value("https://example.com/summer-champ"))
                .andReturn();

        String eventId = extractEventId(createResult);

        // When: Update event with new values
        UpdateEventCommand updateCommand = new UpdateEventCommand(
                "Summer Championship 2026 (Updated)",
                LocalDate.of(2026, 7, 21),
                "Mountain Valley (Updated)",
                "OOB",
                "https://example.com/summer-champ-updated",
                null
        );

        mockMvc.perform(
                        patch("/api/events/{id}", eventId)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(updateCommand))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Summer Championship 2026 (Updated)"))
                .andExpect(jsonPath("$.location").value("Mountain Valley (Updated)"))
                .andExpect(jsonPath("$.websiteUrl").value("https://example.com/summer-champ-updated"));

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
        CreateEventCommand draftEvent = new CreateEventCommand(
                "Draft Event",
                LocalDate.of(2026, 5, 1),
                "Location 1",
                "OOB",
                null,
                null
        );

        CreateEventCommand activeEvent = new CreateEventCommand(
                "Active Event",
                LocalDate.of(2026, 6, 1),
                "Location 2",
                "PRG",
                null,
                null
        );

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

        String activeEventId = extractEventId(activeResult);

        // Publish one event to ACTIVE
        mockMvc.perform(
                post("/api/events/{id}/publish", activeEventId)
                        .contentType("application/json")
        ).andExpect(status().isOk());

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
        CreateEventCommand oobEvent = new CreateEventCommand(
                "OOB Event",
                LocalDate.of(2026, 3, 15),
                "Forest",
                "OOB",
                null,
                null
        );

        CreateEventCommand prgEvent = new CreateEventCommand(
                "PRG Event",
                LocalDate.of(2026, 4, 20),
                "City",
                "PRG",
                null,
                null
        );

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
        CreateEventCommand createCommand = new CreateEventCommand(
                "Links Test Event",
                LocalDate.of(2026, 8, 10),
                "Test Location",
                "BRN",
                null,
                null
        );

        MvcResult createResult = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(createCommand))
                )
                .andExpect(status().isCreated())
                .andReturn();

        String eventId = extractEventId(createResult);

        // When: Get DRAFT event detail
        mockMvc.perform(
                        get("/api/events/{id}", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._templates.default.method").value("PATCH"))  // EDIT
                .andExpect(jsonPath("$._templates.publishEvent").exists())
                .andExpect(jsonPath("$._templates.cancelEvent").exists())
                .andExpect(jsonPath("$._links.registrations.href").exists());

        // When: Publish to ACTIVE and check links
        mockMvc.perform(
                post("/api/events/{id}/publish", eventId)
                        .contentType("application/json")
        ).andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/events/{id}", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._templates.default.method").value("PATCH"))  // EDIT
                .andExpect(jsonPath("$._templates.cancelEvent").exists())
                .andExpect(jsonPath("$._templates.finishEvent").exists())
                .andExpect(jsonPath("$._links.registrations.href").exists());
    }

    @Test
    @DisplayName("GET /api/events/{id} should return 404 for non-existent event")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {EVENTS_READ_AUTHORITY})
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
    @WithMockUser(username = ADMIN_USERNAME, authorities = {EVENTS_MANAGE_AUTHORITY})
    void shouldRejectUpdateForFinishedEvent() throws Exception {
        // Given: Create and finish an event
        CreateEventCommand createCommand = new CreateEventCommand(
                "Immutable Event",
                LocalDate.of(2026, 9, 5),
                "Immutable Location",
                "OOB",
                null,
                null
        );

        MvcResult createResult = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(createCommand))
                )
                .andExpect(status().isCreated())
                .andReturn();

        String eventId = extractEventId(createResult);

        mockMvc.perform(
                post("/api/events/{id}/publish", eventId)
                        .contentType("application/json")
        ).andExpect(status().isOk());

        mockMvc.perform(
                post("/api/events/{id}/finish", eventId)
                        .contentType("application/json")
        ).andExpect(status().isOk());

        // When: Try to update finished event
        UpdateEventCommand updateCommand = new UpdateEventCommand(
                "Updated Name",
                LocalDate.of(2026, 9, 6),
                "Updated Location",
                "PRG",
                null,
                null
        );

        // Then: Update should be rejected
        mockMvc.perform(
                        patch("/api/events/{id}", eventId)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(updateCommand))
                )
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    private String extractEventId(MvcResult result) throws Exception {
        String responseJson = result.getResponse().getContentAsString();
        return objectMapper.readTree(responseJson).get("id").asText();
    }
}
