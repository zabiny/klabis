package com.klabis.events.infrastructure.restapi;

import tools.jackson.databind.ObjectMapper;
import com.klabis.E2ETest;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.security.JwtParams;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRegisterCommandBuilder;
import com.klabis.groups.application.LastOwnershipCheckerImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.klabis.common.security.JwtParams.member;
import static com.klabis.common.security.KlabisMvcRequestBuilders.klabisAuthentication;
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

    private static final String ADMIN_USERNAME = "admin";
    private static final String TEST_MEMBER_ID_STRING = "11111111-1111-1111-1111-111111111111";
    private static final UUID TEST_MEMBER_ID = UUID.fromString(TEST_MEMBER_ID_STRING);

    RequestPostProcessor eventsManageUserAuthentication() {
        return klabisAuthentication(
                JwtParams.jwtTokenParams(ADMIN_USERNAME, UserId.fromString("14ad69f4-8fd0-4314-9cf2-d57e9e6f64a6"))
                        .withAuthorities(Authority.EVENTS_MANAGE)
        );
    }

    @Test
    @DisplayName("Complete registration flow: register → view own → list all → unregister")
    @WithKlabisMockUser(memberId = "11111111-1111-1111-1111-111111111111", authorities = {})
    void shouldCompleteRegistrationFlow() throws Exception {
        // Given: Create an PUBLISHED event
        String publishedEventId = createPublishedEvent("Registration flow test event", LocalDate.now().plusDays(10));

        // When: Register for the event
        Event.RegisterCommand registerCommand = EventRegisterCommandBuilder.builder().siCardNumber("123456").build();

        mockMvc.perform(
                        post("/api/events/{id}/registrations", publishedEventId)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(registerCommand))
                                .with(klabisAuthentication(member(TEST_MEMBER_ID)))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));

        // Then: View own registration (should include SI card)
        mockMvc.perform(
                        get("/api/events/{id}/registrations/me", publishedEventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(klabisAuthentication(
                                        member(TEST_MEMBER_ID)
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
                                .with(klabisAuthentication(
                                        member(TEST_MEMBER_ID)
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
                                .with(klabisAuthentication(
                                        member(TEST_MEMBER_ID)
                                                .withAuthorities(Authority.EVENTS_MANAGE)
                                ))
                )
                .andDo(print())
                .andExpect(status().isNoContent());

        // Then: Verify registration is removed
        mockMvc.perform(
                        get("/api/events/{id}/registrations/me", publishedEventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(klabisAuthentication(
                                        member(TEST_MEMBER_ID)
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
                        .with(klabisAuthentication(
                                JwtParams.jwtTokenParams(ADMIN_USERNAME, new UserId(UUID.randomUUID()))
                                        .withAuthorities(Authority.EVENTS_MANAGE)
                        ))
        ).andExpect(status().isNoContent());

        Event.RegisterCommand registerCommand = EventRegisterCommandBuilder.builder().siCardNumber("876543").build();

        mockMvc.perform(
                post("/api/events/{id}/registrations", eventId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerCommand))
                        .with(klabisAuthentication(member(TEST_MEMBER_ID)))
        ).andExpect(status().isCreated());

        // When: Try to register again with same SI card
        mockMvc.perform(
                        post("/api/events/{id}/registrations", eventId)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(registerCommand))
                                .with(klabisAuthentication(
                                        member(TEST_MEMBER_ID)
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
        Event.RegisterCommand registerCommand = EventRegisterCommandBuilder.builder().siCardNumber("111122").build();

        mockMvc.perform(
                        post("/api/events/{id}/registrations", eventId)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(registerCommand))
                                .with(klabisAuthentication(member(TEST_MEMBER_ID)))
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
        Event.RegisterCommand registerCommand1 = EventRegisterCommandBuilder.builder().siCardNumber("123456").build();
        mockMvc.perform(
                post("/api/events/{id}/registrations", eventId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerCommand1))
                        .with(klabisAuthentication(member(TEST_MEMBER_ID)))
        ).andExpect(status().isCreated());

        // User 2 registration
        Event.RegisterCommand registerCommand2 = EventRegisterCommandBuilder.builder().siCardNumber("789012").build();
        mockMvc.perform(
                post("/api/events/{id}/registrations", eventId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerCommand2))
                        .with(klabisAuthentication(member(UUID.fromString("22222222-2222-2222-2222-222222222222"))))
        ).andExpect(status().isCreated());

        // When: List all registrations (as User 1)
        mockMvc.perform(
                        get("/api/events/{id}/registrations", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(klabisAuthentication(member(TEST_MEMBER_ID)))
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
                                .with(klabisAuthentication(member(TEST_MEMBER_ID)))
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
    @Sql(scripts = "/sql/test-past-event-with-registration.sql",
         executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldFailUnregistrationOnOrAfterEventDate() throws Exception {
        // Given: A past ACTIVE event with a pre-existing registration (set up via SQL to bypass domain validation)
        String eventId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

        // When: Try to unregister (rejected because event date is in the past)
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
                                .with(klabisAuthentication(member(TEST_MEMBER_ID)))
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    private String createPublishedEvent(String name, LocalDate date, List<String> categories) throws Exception {
        String eventId = createDraftEvent(name, date, categories);

        mockMvc.perform(
                        post("/api/events/{id}/publish", eventId)
                                .with(eventsManageUserAuthentication())
                )
                .andExpect(status().isNoContent())
                .andReturn();

        return eventId;
    }

    private String createPublishedEvent(String name, LocalDate date) throws Exception {
        return createPublishedEvent(name, date, List.of());
    }

    @Test
    @DisplayName("5.1 Editing SI card number preserves registeredAt")
    void shouldUpdateSiCardNumberAndPreserveRegisteredAt() throws Exception {
        String eventId = createPublishedEvent("SI card edit test", LocalDate.now().plusDays(10));

        mockMvc.perform(
                post("/api/events/{id}/registrations", eventId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("siCardNumber", "123456")))
                        .with(klabisAuthentication(member(TEST_MEMBER_ID)))
        ).andExpect(status().isCreated());

        String registeredAt = mockMvc.perform(
                        get("/api/events/{id}/registrations/me", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(klabisAuthentication(member(TEST_MEMBER_ID)))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String originalRegisteredAt = objectMapper.readTree(registeredAt).get("registeredAt").asText();

        mockMvc.perform(
                        put("/api/events/{eventId}/registrations/{memberId}", eventId, TEST_MEMBER_ID)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(Map.of("siCardNumber", "999999")))
                                .with(klabisAuthentication(member(TEST_MEMBER_ID)))
                )
                .andDo(print())
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/events/{id}/registrations/me", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(klabisAuthentication(member(TEST_MEMBER_ID)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siCardNumber").value("999999"))
                .andExpect(jsonPath("$.registeredAt").value(originalRegisteredAt));
    }

    @Test
    @DisplayName("5.2 Editing category is reflected in registration list")
    void shouldUpdateCategoryAndShowInRegistrationList() throws Exception {
        String eventId = createPublishedEvent("Category edit test", LocalDate.now().plusDays(10), List.of("M21", "W35"));

        mockMvc.perform(
                post("/api/events/{id}/registrations", eventId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("siCardNumber", "123456", "category", "M21")))
                        .with(klabisAuthentication(member(TEST_MEMBER_ID)))
        ).andExpect(status().isCreated());

        mockMvc.perform(
                        put("/api/events/{eventId}/registrations/{memberId}", eventId, TEST_MEMBER_ID)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(Map.of("siCardNumber", "123456", "category", "W35")))
                                .with(klabisAuthentication(member(TEST_MEMBER_ID)))
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/events/{id}/registrations", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(klabisAuthentication(member(TEST_MEMBER_ID)))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.registrationDtoList[0].category").value("W35"));
    }

    @Test
    @DisplayName("5.3 Member cannot edit another member's registration (403)")
    void shouldReturn403WhenEditingAnotherMembersRegistration() throws Exception {
        UUID member2Id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        String eventId = createPublishedEvent("Forbidden edit test", LocalDate.now().plusDays(10));

        mockMvc.perform(
                post("/api/events/{id}/registrations", eventId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("siCardNumber", "111111")))
                        .with(klabisAuthentication(member(TEST_MEMBER_ID)))
        ).andExpect(status().isCreated());

        mockMvc.perform(
                        put("/api/events/{eventId}/registrations/{memberId}", eventId, TEST_MEMBER_ID)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(Map.of("siCardNumber", "999999")))
                                .with(klabisAuthentication(member(member2Id)))
                )
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5.4 Second member can edit their own registration")
    void shouldAllowSecondMemberToEditOwnRegistration() throws Exception {
        UUID member2Id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        String eventId = createPublishedEvent("Second member edit test", LocalDate.now().plusDays(10));

        mockMvc.perform(
                post("/api/events/{id}/registrations", eventId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("siCardNumber", "777777")))
                        .with(klabisAuthentication(member(member2Id)))
        ).andExpect(status().isCreated());

        mockMvc.perform(
                        put("/api/events/{eventId}/registrations/{memberId}", eventId, member2Id)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(Map.of("siCardNumber", "888888")))
                                .with(klabisAuthentication(member(member2Id)))
                )
                .andDo(print())
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/events/{id}/registrations/me", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(klabisAuthentication(member(member2Id)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siCardNumber").value("888888"));
    }

    @Test
    @DisplayName("5.5 Editing registration after deadline returns 400 and leaves registration unchanged")
    @WithKlabisMockUser(memberId = TEST_MEMBER_ID_STRING)
    @Sql(scripts = "/sql/test-past-deadline-event-with-registration.sql",
         executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldReturn400WhenEditingRegistrationAfterDeadline() throws Exception {
        String eventId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

        mockMvc.perform(
                        put("/api/events/{eventId}/registrations/{memberId}", eventId, TEST_MEMBER_ID)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(Map.of("siCardNumber", "999999")))
                )
                .andDo(print())
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        get("/api/events/{id}/registrations/me", eventId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siCardNumber").value("123456"));
    }

    private String createDraftEvent(String name) throws Exception {
        return createDraftEvent(name, LocalDate.now().plusMonths(1));
    }

    private String createDraftEvent(String name, LocalDate eventDate, List<String> categories) throws Exception {
        Map<String, Object> event = new java.util.HashMap<>();
        event.put("name", name);
        event.put("eventDate", eventDate.toString());
        event.put("location", "Test Location");
        event.put("organizer", "TEST");
        event.put("websiteUrl", null);
        event.put("eventCoordinatorId", null);
        if (!categories.isEmpty()) {
            event.put("categories", categories);
        }

        MvcResult result = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(event))
                                .with(eventsManageUserAuthentication())
                )
                .andExpect(status().isCreated())
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private String createDraftEvent(String name, LocalDate eventDate) throws Exception {
        return createDraftEvent(name, eventDate, List.of());
    }
}
