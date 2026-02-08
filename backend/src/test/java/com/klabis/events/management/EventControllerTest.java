package com.klabis.events.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.SecurityTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for EventController.
 * <p>
 * Tests REST API endpoints for event management including:
 * - Event creation with proper validation
 * - Event updates with status constraints
 * - Event listing with pagination and filtering
 * - Status transitions (publish, cancel, finish)
 * - Security authorization (EVENTS:MANAGE)
 * - HATEOAS link generation based on event status
 */
@DisplayName("Event Controller API Tests")
@ApplicationModuleTest(extraIncludes = {"users", "common", "members"}, mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
@Import(TestApplicationConfiguration.class)
class EventControllerTest extends SecurityTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    private static final String EVENTS_MANAGE_AUTHORITY = "EVENTS:MANAGE";

    @Test
    @DisplayName("POST /api/events should return 201 with Location header and HAL+FORMS links")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {EVENTS_MANAGE_AUTHORITY})
    void shouldCreateEventWithValidData() throws Exception {
        CreateEventCommand command = new CreateEventCommand(
                "Spring Cup 2026",
                LocalDate.of(2026, 3, 15),
                "Forest Park",
                "OOB",
                "https://example.com/spring-cup",
                null  // No coordinator (optional field, would require FK constraint to members table)
        );

        mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(command))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.name").value("Spring Cup 2026"))
                .andExpect(jsonPath("$.location").value("Forest Park"))
                .andExpect(jsonPath("$.organizer").value("OOB"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    @DisplayName("POST /api/events should return 403 without EVENTS:MANAGE authority")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_READ_AUTHORITY})
    void shouldReturn403WithoutEventsManageAuthority() throws Exception {
        CreateEventCommand command = new CreateEventCommand(
                "Test Event",
                LocalDate.of(2026, 5, 1),
                "Location",
                "OOB",
                null,
                null
        );

        mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(command))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/events should return 400 with invalid data")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {EVENTS_MANAGE_AUTHORITY})
    void shouldReturn400WithInvalidData() throws Exception {
        CreateEventCommand command = new CreateEventCommand(
                "",  // Invalid: empty name
                LocalDate.of(2026, 5, 1),
                "Location",
                "OOB",
                null,
                null
        );

        mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(command))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/events/{id} should return 200 with updated event")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {EVENTS_MANAGE_AUTHORITY})
    void shouldUpdateEventSuccessfully() throws Exception {
        // First create event
        CreateEventCommand createCommand = new CreateEventCommand(
                "Original Event",
                LocalDate.of(2026, 5, 1),
                "Original Location",
                "OOB",
                null,
                null
        );

        String createResponse = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(createCommand))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String eventId = objectMapper.readTree(createResponse).get("id").asText();

        // Update event
        UpdateEventCommand updateCommand = new UpdateEventCommand(
                "Updated Event",
                LocalDate.of(2026, 5, 15),
                "Updated Location",
                "PRG",
                "https://updated.com",
                null
        );

        mockMvc.perform(
                        patch("/api/events/{id}", eventId)
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(updateCommand))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Event"))
                .andExpect(jsonPath("$.location").value("Updated Location"))
                .andExpect(jsonPath("$.organizer").value("PRG"));
    }

    @Test
    @DisplayName("GET /api/events should return paginated list with HAL+FORMS")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {EVENTS_MANAGE_AUTHORITY})
    void shouldListEventsWithPagination() throws Exception {
        // Create test events
        CreateEventCommand event1 = new CreateEventCommand(
                "Event 1",
                LocalDate.of(2026, 6, 1),
                "Location 1",
                "OOB",
                null,
                null
        );
        CreateEventCommand event2 = new CreateEventCommand(
                "Event 2",
                LocalDate.of(2026, 7, 1),
                "Location 2",
                "PRG",
                null,
                null
        );

        mockMvc.perform(
                post("/api/events")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(event1))
        );
        mockMvc.perform(
                post("/api/events")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(event2))
        );

        // List events
        mockMvc.perform(
                        get("/api/events")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.eventSummaryDtoList").isArray())
                .andExpect(jsonPath("$.page").exists());
    }

    @Test
    @DisplayName("GET /api/events?status=ACTIVE should filter by status")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {EVENTS_MANAGE_AUTHORITY})
    void shouldFilterEventsByStatus() throws Exception {
        // Create and publish event
        CreateEventCommand command = new CreateEventCommand(
                "Active Event",
                LocalDate.of(2026, 6, 1),
                "Location",
                "OOB",
                null,
                null
        );

        String createResponse = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(command))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String eventId = objectMapper.readTree(createResponse).get("id").asText();

        // Publish event
        mockMvc.perform(
                post("/api/events/{id}/publish", eventId)
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
        );

        // Filter by ACTIVE status
        mockMvc.perform(
                        get("/api/events")
                                .param("status", "ACTIVE")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/events/{id} should return event detail with status-appropriate links")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {EVENTS_MANAGE_AUTHORITY})
    void shouldGetEventWithHateoasLinks() throws Exception {
        // Create event
        CreateEventCommand command = new CreateEventCommand(
                "Test Event",
                LocalDate.of(2026, 6, 1),
                "Location",
                "OOB",
                null,
                null
        );

        String createResponse = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(command))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String eventId = objectMapper.readTree(createResponse).get("id").asText();

        // Get event (DRAFT status should have publish/cancel links)
        mockMvc.perform(
                        get("/api/events/{id}", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.edit.href").exists())
                .andExpect(jsonPath("$._links.publish.href").exists())
                .andExpect(jsonPath("$._links.cancel.href").exists());
    }

    @Test
    @DisplayName("POST /api/events/{id}/publish should transition event to ACTIVE")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {EVENTS_MANAGE_AUTHORITY})
    void shouldPublishEvent() throws Exception {
        // Create event
        CreateEventCommand command = new CreateEventCommand(
                "Test Event",
                LocalDate.of(2026, 6, 1),
                "Location",
                "OOB",
                null,
                null
        );

        String createResponse = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(command))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String eventId = objectMapper.readTree(createResponse).get("id").asText();

        // Publish event
        mockMvc.perform(
                        post("/api/events/{id}/publish", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/events/{id}/cancel should transition event to CANCELLED")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {EVENTS_MANAGE_AUTHORITY})
    void shouldCancelEvent() throws Exception {
        // Create event
        CreateEventCommand command = new CreateEventCommand(
                "Test Event",
                LocalDate.of(2026, 6, 1),
                "Location",
                "OOB",
                null,
                null
        );

        String createResponse = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(command))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String eventId = objectMapper.readTree(createResponse).get("id").asText();

        // Cancel event
        mockMvc.perform(
                        post("/api/events/{id}/cancel", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("POST /api/events/{id}/finish should transition event to FINISHED")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {EVENTS_MANAGE_AUTHORITY})
    void shouldFinishEvent() throws Exception {
        // Create and publish event
        CreateEventCommand command = new CreateEventCommand(
                "Test Event",
                LocalDate.of(2026, 6, 1),
                "Location",
                "OOB",
                null,
                null
        );

        String createResponse = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(command))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String eventId = objectMapper.readTree(createResponse).get("id").asText();

        // Publish first (must be ACTIVE to finish)
        mockMvc.perform(post("/api/events/{id}/publish", eventId));

        // Finish event
        mockMvc.perform(
                        post("/api/events/{id}/finish", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));
    }

    @Test
    @DisplayName("GET /api/events/{id} should return 404 for non-existent event")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {EVENTS_MANAGE_AUTHORITY})
    void shouldReturn404ForNonExistentEvent() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(
                        get("/api/events/{id}", nonExistentId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/events/{id} should return 403 without EVENTS:MANAGE authority")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_READ_AUTHORITY})
    void shouldReturn403WhenUpdatingWithoutAuthority() throws Exception {
        UUID eventId = UUID.randomUUID();
        UpdateEventCommand command = new UpdateEventCommand(
                "Updated Event",
                LocalDate.of(2026, 5, 1),
                "Location",
                "OOB",
                null,
                null
        );

        mockMvc.perform(
                        patch("/api/events/{id}", eventId)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(command))
                )
                .andExpect(status().isForbidden());
    }
}
