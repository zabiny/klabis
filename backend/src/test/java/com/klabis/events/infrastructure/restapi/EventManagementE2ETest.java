package com.klabis.events.infrastructure.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.E2ETest;
import com.klabis.common.SecurityTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.hateoas.MediaTypes;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.events.domain.Event;
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
@WithKlabisMockUser(username = "admin", authorities = {Authority.EVENTS_MANAGE, Authority.EVENTS_READ})
class EventManagementE2ETest extends SecurityTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Complete event lifecycle: create → publish → finish")
    void shouldCompleteEventLifecycleFromCreateToFinish() throws Exception {
        // Given: Create an event
        Event.EventCommand createCommand = new Event.EventCommand(
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

        // When: Finish the event (ACTIVE → FINISHED)
        mockMvc.perform(
                        post("/api/events/{id}/finish", eventId)
                                .contentType("application/json")
                )
                .andDo(print())
                .andExpect(status().isNoContent());

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
        Event.EventCommand createCommand = new Event.EventCommand(
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
        Event.EventCommand createCommand = new Event.EventCommand(
                "Summer Championship 2026",
                LocalDate.of(2026, 7, 20),
                "Mountain Valley",
                "BRN",
                "https://example.com/summer-champ",
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
        Event.EventCommand updateCommand = new Event.EventCommand(
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
        Event.EventCommand draftEvent = new Event.EventCommand(
                "Draft Event",
                LocalDate.of(2026, 5, 1),
                "Location 1",
                "OOB",
                null,
                null
        );

        Event.EventCommand activeEvent = new Event.EventCommand(
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
        Event.EventCommand oobEvent = new Event.EventCommand(
                "OOB Event",
                LocalDate.of(2026, 3, 15),
                "Forest",
                "OOB",
                null,
                null
        );

        Event.EventCommand prgEvent = new Event.EventCommand(
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
        Event.EventCommand createCommand = new Event.EventCommand(
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
                .andExpect(jsonPath("$._templates.default.method").value("PATCH"))  // EDIT
                .andExpect(jsonPath("$._templates.publishEvent").exists())
                .andExpect(jsonPath("$._templates.cancelEvent").exists())
                .andExpect(jsonPath("$._links.registrations.href").exists());

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
                .andExpect(jsonPath("$._templates.default.method").value("PATCH"))  // EDIT
                .andExpect(jsonPath("$._templates.cancelEvent").exists())
                .andExpect(jsonPath("$._templates.finishEvent").exists())
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
        // Given: Create and finish an event
        Event.EventCommand createCommand = new Event.EventCommand(
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

        String eventId = extractEventIdFromLocation(createResult);

        mockMvc.perform(
                post("/api/events/{id}/publish", eventId)
                        .contentType("application/json")
        ).andExpect(status().isNoContent());

        mockMvc.perform(
                post("/api/events/{id}/finish", eventId)
                        .contentType("application/json")
        ).andExpect(status().isNoContent());

        // When: Try to update finished event
        Event.EventCommand updateCommand = new Event.EventCommand(
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

    private String extractEventIdFromLocation(MvcResult result) {
        String location = result.getResponse().getHeader("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }
}
