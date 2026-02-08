package com.klabis.events.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.E2EIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end tests for event registration.
 * <p>
 * Tests complete registration flow including:
 * - Member registration for ACTIVE events
 * - Duplicate registration prevention
 * - Unregistration before event date
 * - Privacy enforcement (SI card only visible to owner)
 * - Permission enforcement (authentication required)
 */
@E2EIntegrationTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/sql/test-members-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Event Registration E2E Tests")
class EventRegistrationE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ADMIN_USERNAME = "admin";
    private static final String EVENTS_MANAGE_AUTHORITY = "EVENTS:MANAGE";
    private static final UUID TEST_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    @DisplayName("Complete registration flow: register → view own → list all → unregister")
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", authorities = {EVENTS_MANAGE_AUTHORITY})
    void shouldCompleteRegistrationFlow() throws Exception {
        // Given: Create an ACTIVE event
        String eventId = createActiveEvent("Test Event 1", LocalDate.of(2026, 5, 15));

        // When: Register for the event
        RegisterForEventCommand registerCommand = new RegisterForEventCommand("123456");

        MvcResult registerResult = mockMvc.perform(
                        post("/api/events/{id}/registrations", eventId)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(registerCommand))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.siCardNumber").value("123456"))
                .andExpect(jsonPath("$.registeredAt").exists())
                .andReturn();

        // Then: View own registration (should include SI card)
        mockMvc.perform(
                        get("/api/events/{id}/registrations/me", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.siCardNumber").value("123456"))
                .andExpect(jsonPath("$.registeredAt").exists());

        // Then: List all registrations (should NOT include SI cards)
        mockMvc.perform(
                        get("/api/events/{id}/registrations", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.registrationDtoList[0].firstName").value("Test"))
                .andExpect(jsonPath("$._embedded.registrationDtoList[0].lastName").value("User"))
                .andExpect(jsonPath("$._embedded.registrationDtoList[0].registeredAt").exists())
                .andExpect(jsonPath("$._embedded.registrationDtoList[0].siCardNumber").doesNotExist());

        // When: Unregister from event (before event date)
        mockMvc.perform(
                        delete("/api/events/{id}/registrations", eventId)
                                .contentType("application/json")
                )
                .andDo(print())
                .andExpect(status().isNoContent());

        // Then: Verify registration is removed
        mockMvc.perform(
                        get("/api/events/{id}/registrations/me", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Duplicate registration should return 409 Conflict")
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", authorities = {EVENTS_MANAGE_AUTHORITY})
    void shouldReturn409ForDuplicateRegistration() throws Exception {
        // Given: Create an ACTIVE event and register
        String eventId = createActiveEvent("Duplicate Test Event", LocalDate.of(2026, 6, 20));

        RegisterForEventCommand registerCommand = new RegisterForEventCommand("876543");

        mockMvc.perform(
                post("/api/events/{id}/registrations", eventId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerCommand))
        ).andExpect(status().isCreated());

        // When: Try to register again with same SI card
        mockMvc.perform(
                        post("/api/events/{id}/registrations", eventId)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(registerCommand))
                )
                .andDo(print())
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Registration should be rejected for non-ACTIVE events")
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", authorities = {EVENTS_MANAGE_AUTHORITY})
    void shouldRejectRegistrationForNonActiveEvents() throws Exception {
        // Given: Create a DRAFT event
        String eventId = createDraftEvent("Draft Event");

        // When: Try to register for DRAFT event
        RegisterForEventCommand registerCommand = new RegisterForEventCommand("111122");

        mockMvc.perform(
                        post("/api/events/{id}/registrations", eventId)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(registerCommand))
                )
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Given: Create FINISHED event
        String finishedEventId = createFinishedEvent("Finished Event", LocalDate.of(2026, 3, 1));

        // When: Try to register for FINISHED event
        mockMvc.perform(
                        post("/api/events/{id}/registrations", finishedEventId)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(registerCommand))
                )
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Unregistration should fail on or after event date")
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", authorities = {EVENTS_MANAGE_AUTHORITY})
    void shouldRejectUnregistrationAfterEventDate() throws Exception {
        // Given: Create an ACTIVE event with date in the past
        String eventId = createActiveEvent("Past Event", LocalDate.now().minusDays(1));

        // And: Register for the event
        RegisterForEventCommand registerCommand = new RegisterForEventCommand("333344");

        mockMvc.perform(
                post("/api/events/{id}/registrations", eventId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerCommand))
        ).andExpect(status().isCreated());

        // When: Try to unregister after event date
        mockMvc.perform(
                        delete("/api/events/{id}/registrations", eventId)
                                .contentType("application/json")
                )
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Registration privacy: list shows names only, /me shows SI card")
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", authorities = {EVENTS_MANAGE_AUTHORITY})
    void shouldEnforceRegistrationPrivacy() throws Exception {
        // Given: Create an ACTIVE event and register
        String eventId = createActiveEvent("Privacy Test Event", LocalDate.of(2026, 7, 10));

        RegisterForEventCommand registerCommand = new RegisterForEventCommand("999988");

        mockMvc.perform(
                post("/api/events/{id}/registrations", eventId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerCommand))
        ).andExpect(status().isCreated());

        // When: Get public registration list
        mockMvc.perform(
                        get("/api/events/{id}/registrations", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.registrationDtoList[0].firstName").value("Test"))
                .andExpect(jsonPath("$._embedded.registrationDtoList[0].lastName").value("User"))
                .andExpect(jsonPath("$._embedded.registrationDtoList[0].registeredAt").exists())
                // SI card should NOT be in public list
                .andExpect(jsonPath("$._embedded.registrationDtoList[0].siCardNumber").doesNotExist());

        // When: Get own registration
        mockMvc.perform(
                        get("/api/events/{id}/registrations/me", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.registeredAt").exists())
                // SI card SHOULD be visible in own registration
                .andExpect(jsonPath("$.siCardNumber").value("999988"));
    }

    @Test
    @DisplayName("Registration endpoints should return 401 for unauthenticated users")
    @Sql(statements = {
            "DELETE FROM event_registrations",
            "DELETE FROM events",
            "INSERT INTO events (id, name, event_date, location, organizer, website_url, event_coordinator_id, status, created_at, created_by, modified_at, modified_by, version) VALUES " +
            "('12345678-1234-1234-1234-123456789001', 'Auth Test Event', '2026-08-15', 'Test Location', 'OOB', NULL, NULL, 'ACTIVE', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)"
    })
    void shouldReturn401ForUnauthenticatedUsers() throws Exception {
        // Given: An ACTIVE event exists (created via SQL)
        String eventId = "12345678-1234-1234-1234-123456789001";

        RegisterForEventCommand registerCommand = new RegisterForEventCommand("555566");

        // When & Then: Unauthenticated registration should fail
        mockMvc.perform(
                        post("/api/events/{id}/registrations", eventId)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(registerCommand))
                )
                .andDo(print())
                .andExpect(status().isUnauthorized());

        // When & Then: Unauthenticated list should fail
        mockMvc.perform(
                        get("/api/events/{id}/registrations", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized());

        // When & Then: Unauthenticated own registration should fail
        mockMvc.perform(
                        get("/api/events/{id}/registrations/me", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized());

        // When & Then: Unauthenticated unregistration should fail
        mockMvc.perform(
                        delete("/api/events/{id}/registrations", eventId)
                                .contentType("application/json")
                )
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/events/{id}/registrations/me should return 404 when not registered")
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", authorities = {EVENTS_MANAGE_AUTHORITY})
    void shouldReturn404WhenNotRegistered() throws Exception {
        // Given: Create an ACTIVE event (but don't register)
        String eventId = createActiveEvent("Not Registered Event", LocalDate.of(2026, 9, 1));

        // When & Then: Get own registration should return 404
        mockMvc.perform(
                        get("/api/events/{id}/registrations/me", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    // Helper methods - use raw JSON to avoid package-private imports

    private String createActiveEvent(String name, LocalDate eventDate) throws Exception {
        Map<String, Object> event = new java.util.HashMap<>();
        event.put("name", name);
        event.put("eventDate", eventDate.toString());
        event.put("location", "Test Location");
        event.put("organizer", "OOB");
        event.put("websiteUrl", null);
        event.put("eventCoordinatorId", null);

        MvcResult result = mockMvc.perform(
                post("/api/events")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(event))
        ).andExpect(status().isCreated()).andReturn();

        String eventId = extractEventId(result);

        // Publish to ACTIVE
        mockMvc.perform(
                post("/api/events/{id}/publish", eventId)
                        .contentType("application/json")
        ).andExpect(status().isOk());

        return eventId;
    }

    private String createDraftEvent(String name) throws Exception {
        Map<String, Object> event = new java.util.HashMap<>();
        event.put("name", name);
        event.put("eventDate", LocalDate.of(2026, 10, 1).toString());
        event.put("location", "Draft Location");
        event.put("organizer", "PRG");
        event.put("websiteUrl", null);
        event.put("eventCoordinatorId", null);

        MvcResult result = mockMvc.perform(
                post("/api/events")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(event))
        ).andExpect(status().isCreated()).andReturn();

        return extractEventId(result);
    }

    private String createFinishedEvent(String name, LocalDate eventDate) throws Exception {
        Map<String, Object> event = new java.util.HashMap<>();
        event.put("name", name);
        event.put("eventDate", eventDate.toString());
        event.put("location", "Finished Location");
        event.put("organizer", "BRN");
        event.put("websiteUrl", null);
        event.put("eventCoordinatorId", null);

        MvcResult result = mockMvc.perform(
                post("/api/events")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(event))
        ).andExpect(status().isCreated()).andReturn();

        String eventId = extractEventId(result);

        // Publish and finish
        mockMvc.perform(
                post("/api/events/{id}/publish", eventId)
                        .contentType("application/json")
        ).andExpect(status().isOk());

        mockMvc.perform(
                post("/api/events/{id}/finish", eventId)
                        .contentType("application/json")
        ).andExpect(status().isOk());

        return eventId;
    }

    private String extractEventId(MvcResult result) throws Exception {
        String responseJson = result.getResponse().getContentAsString();
        return objectMapper.readTree(responseJson).get("id").asText();
    }
}
