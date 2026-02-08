package com.klabis.events.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.SecurityTestBase;
import com.klabis.events.Event;
import com.klabis.events.SiCardNumber;
import com.klabis.events.persistence.EventRepository;
import com.klabis.users.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for EventRegistrationController.
 * <p>
 * Tests REST API endpoints for event registration including:
 * - Member registration for events
 * - Duplicate registration handling
 * - Unregistration
 * - Listing registrations with privacy enforcement
 * - Viewing own registration with full details
 * - Security authorization
 * - HATEOAS link generation
 */
@DisplayName("Event Registration Controller API Tests")
@ApplicationModuleTest(extraIncludes = {"users", "common", "members"}, mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
@Import(TestApplicationConfiguration.class)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = {
        // Delete in correct order (respecting foreign keys)
        "DELETE FROM event_registrations",
        "DELETE FROM events",
        "DELETE FROM members",
        // Insert test members (note: column names match V001 schema)
        "INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by, modified_at, modified_by, version) VALUES ('11111111-1111-1111-1111-111111111111', 'ZBM0501', 'John', 'Doe', '1990-01-01', 'CZ', 'MALE', 'john.doe@example.com', '+420123456789', 'Test St 1', 'Prague', '10000', 'CZ', true, CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'system', 0)",
        "INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by, modified_at, modified_by, version) VALUES ('22222222-2222-2222-2222-222222222222', 'ZBM0502', 'Jane', 'Smith', '1992-05-15', 'CZ', 'FEMALE', 'jane.smith@example.com', '+420987654321', 'Test St 2', 'Brno', '60200', 'CZ', true, CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'system', 0)"
})
class EventRegistrationControllerTest extends SecurityTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    private static final String MEMBER_1_ID = "11111111-1111-1111-1111-111111111111";
    private static final String MEMBER_2_ID = "22222222-2222-2222-2222-222222222222";

    @Test
    @DisplayName("POST /api/events/{id}/registrations should return 201 Created")
    @WithMockUser(username = MEMBER_1_ID)
    void shouldRegisterMemberForEvent() throws Exception {
        // Given - create an ACTIVE event
        Event event = Event.create(
                "Test Event",
                LocalDate.of(2026, 6, 15),
                "Test Location",
                "OOB",
                null,
                null
        );
        event.publish();
        Event savedEvent = eventRepository.save(event);

        RegisterForEventCommand command = new RegisterForEventCommand("123456");

        // When/Then
        mockMvc.perform(
                        post("/api/events/{eventId}/registrations", savedEvent.getId().value())
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(command))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.siCardNumber").value("123456"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.event.href").exists())
                .andExpect(jsonPath("$._links.unregister.href").exists());
    }

    @Test
    @DisplayName("POST /api/events/{id}/registrations should return 409 Conflict for duplicate registration")
    @WithMockUser(username = MEMBER_1_ID)
    void shouldReturn409ForDuplicateRegistration() throws Exception {
        // Given - create an ACTIVE event with existing registration
        Event event = Event.create(
                "Test Event",
                LocalDate.of(2026, 6, 15),
                "Test Location",
                "OOB",
                null,
                null
        );
        event.publish();
        event.registerMember(new UserId(UUID.fromString(MEMBER_1_ID)), SiCardNumber.of("111111"));
        Event savedEvent = eventRepository.save(event);

        RegisterForEventCommand command = new RegisterForEventCommand("123456");

        // When/Then
        mockMvc.perform(
                        post("/api/events/{eventId}/registrations", savedEvent.getId().value())
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(command))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Registration Conflict"));
    }

    @Test
    @DisplayName("DELETE /api/events/{id}/registrations should return 204 No Content")
    @WithMockUser(username = MEMBER_1_ID)
    void shouldUnregisterMemberFromEvent() throws Exception {
        // Given - create an ACTIVE event with member registered
        Event event = Event.create(
                "Test Event",
                LocalDate.of(2026, 6, 15),
                "Test Location",
                "OOB",
                null,
                null
        );
        event.publish();
        event.registerMember(new UserId(UUID.fromString(MEMBER_1_ID)), SiCardNumber.of("123456"));
        Event savedEvent = eventRepository.save(event);

        // When/Then
        mockMvc.perform(
                        delete("/api/events/{eventId}/registrations", savedEvent.getId().value())
                )
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/events/{id}/registrations should return list without SI card numbers")
    @WithMockUser(username = MEMBER_1_ID)
    void shouldListRegistrationsWithoutSiCardNumbers() throws Exception {
        // Given - create an ACTIVE event with multiple registrations
        Event event = Event.create(
                "Test Event",
                LocalDate.of(2026, 6, 15),
                "Test Location",
                "OOB",
                null,
                null
        );
        event.publish();
        event.registerMember(new UserId(UUID.fromString(MEMBER_1_ID)), SiCardNumber.of("111111"));
        event.registerMember(new UserId(UUID.fromString(MEMBER_2_ID)), SiCardNumber.of("222222"));
        Event savedEvent = eventRepository.save(event);

        // When/Then
        mockMvc.perform(
                        get("/api/events/{eventId}/registrations", savedEvent.getId().value())
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.registrationDtoList").isArray())
                .andExpect(jsonPath("$._embedded.registrationDtoList.length()").value(2))
                // Verify both members are in the list (order may vary)
                .andExpect(jsonPath("$._embedded.registrationDtoList[?(@.firstName == 'John' && @.lastName == 'Doe')]").exists())
                .andExpect(jsonPath("$._embedded.registrationDtoList[?(@.firstName == 'Jane' && @.lastName == 'Smith')]").exists())
                // Verify SI card numbers are NOT present for privacy
                .andExpect(jsonPath("$._embedded.registrationDtoList[0].siCardNumber").doesNotExist())
                .andExpect(jsonPath("$._embedded.registrationDtoList[1].siCardNumber").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/events/{id}/registrations/me should return full registration with SI card")
    @WithMockUser(username = MEMBER_1_ID)
    void shouldReturnOwnRegistrationWithSiCard() throws Exception {
        // Given - create an ACTIVE event with member registered
        Event event = Event.create(
                "Test Event",
                LocalDate.of(2026, 6, 15),
                "Test Location",
                "OOB",
                null,
                null
        );
        event.publish();
        event.registerMember(new UserId(UUID.fromString(MEMBER_1_ID)), SiCardNumber.of("123456"));
        Event savedEvent = eventRepository.save(event);

        // When/Then
        mockMvc.perform(
                        get("/api/events/{eventId}/registrations/me", savedEvent.getId().value())
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.siCardNumber").value("123456")) // Full details including SI card
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.event.href").exists())
                .andExpect(jsonPath("$._links.unregister.href").exists());
    }

    @Test
    @DisplayName("GET /api/events/{id}/registrations/me should return 404 when not registered")
    @WithMockUser(username = MEMBER_1_ID)
    void shouldReturn404WhenNotRegistered() throws Exception {
        // Given - create an ACTIVE event without member registration
        Event event = Event.create(
                "Test Event",
                LocalDate.of(2026, 6, 15),
                "Test Location",
                "OOB",
                null,
                null
        );
        event.publish();
        Event savedEvent = eventRepository.save(event);

        // When/Then
        mockMvc.perform(
                        get("/api/events/{eventId}/registrations/me", savedEvent.getId().value())
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Registration Not Found"));
    }

    @Test
    @DisplayName("POST /api/events/{id}/registrations should return 401 for unauthenticated users")
    void shouldReturn401ForUnauthenticatedUser() throws Exception {
        // Given - create an ACTIVE event
        Event event = Event.create(
                "Test Event",
                LocalDate.of(2026, 6, 15),
                "Test Location",
                "OOB",
                null,
                null
        );
        event.publish();
        Event savedEvent = eventRepository.save(event);

        RegisterForEventCommand command = new RegisterForEventCommand("123456");

        // When/Then - no authentication
        mockMvc.perform(
                        post("/api/events/{eventId}/registrations", savedEvent.getId().value())
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(command))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/events/{id}/registrations should return 401 for unauthenticated users")
    void shouldReturn401ForUnauthenticatedUserOnDelete() throws Exception {
        // Given - create an ACTIVE event
        Event event = Event.create(
                "Test Event",
                LocalDate.of(2026, 6, 15),
                "Test Location",
                "OOB",
                null,
                null
        );
        event.publish();
        Event savedEvent = eventRepository.save(event);

        // When/Then - no authentication
        mockMvc.perform(
                        delete("/api/events/{eventId}/registrations", savedEvent.getId().value())
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/events/{id}/registrations/me should return 401 for unauthenticated users")
    void shouldReturn401ForUnauthenticatedUserOnGetOwn() throws Exception {
        // Given - create an ACTIVE event
        Event event = Event.create(
                "Test Event",
                LocalDate.of(2026, 6, 15),
                "Test Location",
                "OOB",
                null,
                null
        );
        event.publish();
        Event savedEvent = eventRepository.save(event);

        // When/Then - no authentication
        mockMvc.perform(
                        get("/api/events/{eventId}/registrations/me", savedEvent.getId().value())
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isUnauthorized());
    }
}
