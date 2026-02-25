package com.klabis.events.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.E2ETest;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.security.JwtParams;
import com.klabis.common.security.KlabisMvcRequestBuilders;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

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
@E2ETest
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
    private static final String TEST_MEMBER_ID_STRING = "11111111-1111-1111-1111-111111111111";
    private static final UUID TEST_MEMBER_ID = UUID.fromString(TEST_MEMBER_ID_STRING);

    RequestPostProcessor eventsManageUserAuthentication() {
        return KlabisMvcRequestBuilders.klabisAuthentication(
                JwtParams.jwtTokenParams(ADMIN_USERNAME, UserId.fromString("14ad69f4-8fd0-4314-9cf2-d57e9e6f64a6"))
                        .withAuthorities(Authority.EVENTS_MANAGE)
        );
    }

    @Test
    @DisplayName("Complete registration flow: register → view own → list all → unregister")
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", authorities = {})
    void shouldCompleteRegistrationFlow() throws Exception {
        // Given: Create an PUBLISHED event
        String publishedEventId = createPublishedEvent("Registration flow test event", LocalDate.now().plusDays(10));

        // When: Register for the event
        RegisterForEventCommand registerCommand = new RegisterForEventCommand("123456");

        mockMvc.perform(
                        post("/api/events/{id}/registrations", publishedEventId)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(registerCommand))
                                .with(KlabisMvcRequestBuilders.klabisAuthentication(
                                        JwtParams.jwtTokenParams("11111111-1111-1111-1111-111111111111",
                                                        new UserId(TEST_MEMBER_ID))
                                                .withAuthorities(Authority.EVENTS_MANAGE)
                                ))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.siCardNumber").value("123456"))
                .andExpect(jsonPath("$.registeredAt").exists());

        // Then: View own registration (should include SI card)
        mockMvc.perform(
                        get("/api/events/{id}/registrations/me", publishedEventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(KlabisMvcRequestBuilders.klabisAuthentication(
                                        JwtParams.jwtTokenParams("11111111-1111-1111-1111-111111111111",
                                                        new UserId(TEST_MEMBER_ID))
                                                .withAuthorities(Authority.EVENTS_MANAGE)
                                ))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.siCardNumber").value("123456"))
                .andExpect(jsonPath("$.registeredAt").exists());

        // Then: List all registrations (should NOT include SI cards)
        mockMvc.perform(
                        get("/api/events/{id}/registrations", publishedEventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(KlabisMvcRequestBuilders.klabisAuthentication(
                                        JwtParams.jwtTokenParams("11111111-1111-1111-1111-111111111111",
                                                        new UserId(TEST_MEMBER_ID))
                                                .withAuthorities(Authority.EVENTS_MANAGE)
                                ))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.registrationDtoList[0].firstName").value("Test"))
                .andExpect(jsonPath("$._embedded.registrationDtoList[0].lastName").value("User"))
                .andExpect(jsonPath("$._embedded.registrationDtoList[0].registeredAt").exists())
                .andExpect(jsonPath("$._embedded.registrationDtoList[0].siCardNumber").doesNotExist());

        // When: Unregister from event (before event date)
        mockMvc.perform(
                        delete("/api/events/{id}/registrations", publishedEventId)
                                .contentType("application/json")
                                .with(KlabisMvcRequestBuilders.klabisAuthentication(
                                        JwtParams.jwtTokenParams("11111111-1111-1111-1111-111111111111",
                                                        new UserId(TEST_MEMBER_ID))
                                                .withAuthorities(Authority.EVENTS_MANAGE)
                                ))
                )
                .andDo(print())
                .andExpect(status().isNoContent());

        // Then: Verify registration is removed
        mockMvc.perform(
                        get("/api/events/{id}/registrations/me", publishedEventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(KlabisMvcRequestBuilders.klabisAuthentication(
                                        JwtParams.jwtTokenParams("11111111-1111-1111-1111-111111111111",
                                                        new UserId(TEST_MEMBER_ID))
                                                .withAuthorities(Authority.EVENTS_MANAGE)
                                ))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Duplicate registration should return 409 Conflict")
    void shouldReturn409ForDuplicateRegistration() throws Exception {
        // Given: Create a DRAFT event, publish it, and register
        String eventId = createDraftEvent("Duplicate Test Event");
        mockMvc.perform(
                post("/api/events/{id}/publish", eventId)
                        .with(KlabisMvcRequestBuilders.klabisAuthentication(
                                JwtParams.jwtTokenParams(ADMIN_USERNAME, new UserId(UUID.randomUUID()))
                                        .withAuthorities(Authority.EVENTS_MANAGE)
                        ))
        ).andExpect(status().isOk());

        RegisterForEventCommand registerCommand = new RegisterForEventCommand("876543");

        mockMvc.perform(
                post("/api/events/{id}/registrations", eventId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerCommand))
                        .with(KlabisMvcRequestBuilders.klabisAuthentication(
                                JwtParams.jwtTokenParams("11111111-1111-1111-1111-111111111111",
                                                new UserId(TEST_MEMBER_ID))
                                        .withAuthorities(Authority.EVENTS_MANAGE)
                        ))
        ).andExpect(status().isCreated());

        // When: Try to register again with same SI card
        mockMvc.perform(
                        post("/api/events/{id}/registrations", eventId)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(registerCommand))
                                .with(KlabisMvcRequestBuilders.klabisAuthentication(
                                        JwtParams.jwtTokenParams("11111111-1111-1111-1111-111111111111",
                                                        new UserId(TEST_MEMBER_ID))
                                                .withAuthorities(Authority.EVENTS_MANAGE)
                                ))
                )
                .andDo(print())
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Registration should be rejected for non-ACTIVE events")
    void shouldRejectRegistrationForNonActiveEvents() throws Exception {
        // Given: Create a DRAFT event
        String eventId = createDraftEvent("Draft Event");

        // When: Try to register for DRAFT event
        RegisterForEventCommand registerCommand = new RegisterForEventCommand("111122");

        mockMvc.perform(
                        post("/api/events/{id}/registrations", eventId)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(registerCommand))
                                .with(KlabisMvcRequestBuilders.klabisAuthentication(
                                        JwtParams.jwtTokenParams("11111111-1111-1111-1111-111111111111",
                                                        new UserId(TEST_MEMBER_ID))
                                                .withAuthorities(Authority.EVENTS_MANAGE)
                                ))
                )
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Registration privacy: list shows names only, /me shows SI card")
    void shouldProtectPrivacyInRegistrationList() throws Exception {
        // Given: Create a PUBLISHED event, and register two users
        String eventId = createPublishedEvent("Privacy Test Event", LocalDate.now().plusDays(3));

        // User 1 registration
        RegisterForEventCommand registerCommand1 = new RegisterForEventCommand("123456");
        mockMvc.perform(
                post("/api/events/{id}/registrations", eventId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerCommand1))
                        .with(KlabisMvcRequestBuilders.klabisAuthentication(
                                JwtParams.jwtTokenParams("11111111-1111-1111-1111-111111111111",
                                                new UserId(TEST_MEMBER_ID))
                        ))
        ).andExpect(status().isCreated());

        // User 2 registration
        RegisterForEventCommand registerCommand2 = new RegisterForEventCommand("789012");
        mockMvc.perform(
                post("/api/events/{id}/registrations", eventId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerCommand2))
                        .with(KlabisMvcRequestBuilders.klabisAuthentication(
                                JwtParams.jwtTokenParams("22222222-2222-2222-2222-222222222222",
                                                new UserId(UUID.fromString("22222222-2222-2222-2222-222222222222")))
                        ))
        ).andExpect(status().isCreated());

        // When: List all registrations (as User 1)
        mockMvc.perform(
                        get("/api/events/{id}/registrations", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(KlabisMvcRequestBuilders.klabisAuthentication(
                                        JwtParams.jwtTokenParams("11111111-1111-1111-1111-111111111111",
                                                        new UserId(TEST_MEMBER_ID))
                                ))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.registrationDtoList").isArray())
                .andExpect(jsonPath("$._embedded.registrationDtoList.length()").value(2))
                .andExpect(jsonPath("$._embedded.registrationDtoList[0].firstName").exists())
                .andExpect(jsonPath("$._embedded.registrationDtoList[0].lastName").exists())
                .andExpect(jsonPath("$._embedded.registrationDtoList[0].siCardNumber").doesNotExist())
                .andExpect(jsonPath("$._embedded.registrationDtoList[1].firstName").exists())
                .andExpect(jsonPath("$._embedded.registrationDtoList[1].lastName").exists())
                .andExpect(jsonPath("$._embedded.registrationDtoList[1].siCardNumber").doesNotExist());

        // When: Get own registration (as User 1)
        mockMvc.perform(
                        get("/api/events/{id}/registrations/me", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(KlabisMvcRequestBuilders.klabisAuthentication(
                                        JwtParams.jwtTokenParams("11111111-1111-1111-1111-111111111111",
                                                        new UserId(TEST_MEMBER_ID))
                                ))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").exists())
                .andExpect(jsonPath("$.lastName").exists())
                .andExpect(jsonPath("$.siCardNumber").exists());
    }

    @Test
    @DisplayName("Unregistration should fail on or after event date")
    @WithKlabisMockUser(memberId = TEST_MEMBER_ID_STRING)
    void shouldFailUnregistrationOnOrAfterEventDate() throws Exception {
        // Given: Create a PUBLISHED event in the past
        String eventId = createPublishedEvent("Past Event", LocalDate.now().minusDays(10));

        // And: Register for the event first
        RegisterForEventCommand registerCommand = new RegisterForEventCommand("123456");
        mockMvc.perform(
                post("/api/events/{id}/registrations", eventId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerCommand))
        ).andExpect(status().isCreated());

        // When: Try to unregister (after event date)
        mockMvc.perform(
                        delete("/api/events/{id}/registrations", eventId)
                                .contentType("application/json")
                )
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/events/{id}/registrations/me should return 404 when not registered")
    void shouldReturn404WhenNotRegistered() throws Exception {
        // Given: Create a DRAFT event and publish it to ACTIVE
        String eventId = createPublishedEvent("Not Registered Test", LocalDate.now().plusDays(10));

        // When: Get own registration without being registered
        mockMvc.perform(
                        get("/api/events/{id}/registrations/me", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(KlabisMvcRequestBuilders.klabisAuthentication(
                                        JwtParams.jwtTokenParams("11111111-1111-1111-1111-111111111111",
                                                        new UserId(TEST_MEMBER_ID))
                                                .withAuthorities(Authority.EVENTS_MANAGE)
                                ))
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    private String createPublishedEvent(String name, LocalDate date) throws Exception {
        String eventId = createDraftEvent(name, date);

        mockMvc.perform(
                        post("/api/events/{id}/publish", eventId)
                                .with(eventsManageUserAuthentication())
                )
                .andExpect(status().isOk())
                .andReturn();

        return eventId;
    }

    private String createDraftEvent(String name) throws Exception {
        return createDraftEvent(name, LocalDate.now().plusMonths(1));
    }

    private String createDraftEvent(String name, LocalDate eventDate) throws Exception {
        Map<String, Object> event = new java.util.HashMap<>();
        event.put("name", name);
        event.put("eventDate", eventDate.toString());
        event.put("location", "Test Location");
        event.put("organizer", "TEST");
        event.put("websiteUrl", null);
        event.put("eventCoordinatorId", null);

        MvcResult result = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(event))
                                .with(eventsManageUserAuthentication())
                )
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }
}