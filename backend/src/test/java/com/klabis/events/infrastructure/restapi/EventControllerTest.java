package com.klabis.events.infrastructure.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserService;
import com.klabis.events.EventTestDataBuilder;
import com.klabis.events.application.EventManagementService;
import com.klabis.events.application.EventNotFoundException;
import com.klabis.events.domain.Event;
import com.klabis.events.EventId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("EventController API tests")
@WebMvcTest(controllers = EventController.class)
@Import(EncryptionConfiguration.class)
class EventControllerTest {

    private static final String ADMIN_USERNAME = "admin";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventManagementService eventManagementService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("POST /api/events")
    class CreateEventTests {

        @Test
        @DisplayName("should return 201 with Location header and no body")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldCreateEventWithValidData() throws Exception {
            Event createdEvent = EventTestDataBuilder.anEvent().withName("Spring Cup 2026").build();
            Event.CreateCommand command = new Event.CreateCommand(
                    "Spring Cup 2026",
                    LocalDate.of(2026, 3, 15),
                    "Forest Park",
                    "OOB",
                    "https://example.com/spring-cup",
                    null
            );

            when(eventManagementService.createEvent(any(Event.CreateCommand.class))).thenReturn(createdEvent);

            mockMvc.perform(
                            post("/api/events")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(command))
                    )
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));
        }

        @Test
        @DisplayName("should return 403 without EVENTS:MANAGE authority")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldReturn403WithoutEventsManageAuthority() throws Exception {
            Event.CreateCommand command = new Event.CreateCommand(
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
        @DisplayName("should return 400 with invalid data")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn400WithInvalidData() throws Exception {
            Event.CreateCommand command = new Event.CreateCommand(
                    "",
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
    }

    @Nested
    @DisplayName("PATCH /api/events/{id}")
    class UpdateEventTests {

        @Test
        @DisplayName("should return 204 No Content")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldUpdateEventSuccessfully() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event.UpdateCommand updateCommand = new Event.UpdateCommand(
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
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 403 without EVENTS:MANAGE authority")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldReturn403WhenUpdatingWithoutAuthority() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event.UpdateCommand command = new Event.UpdateCommand(
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

    @Nested
    @DisplayName("GET /api/events")
    class ListEventsTests {

        @Test
        @DisplayName("should return paginated list with HAL+FORMS")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldListEventsWithPagination() throws Exception {
            Event event1 = Event.create("Event 1", LocalDate.of(2026, 6, 1), "Location 1", "OOB", null, null);
            Event event2 = Event.create("Event 2", LocalDate.of(2026, 7, 1), "Location 2", "PRG", null, null);
            event2.publish();

            when(eventManagementService.listEvents(any()))
                    .thenReturn(new PageImpl<>(List.of(event1, event2), PageRequest.of(0, 10), 2));

            mockMvc.perform(
                            get("/api/events")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList").isArray())
                    .andExpect(jsonPath("$.page").exists());
        }

        @Test
        @DisplayName("should filter by status")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldFilterEventsByStatus() throws Exception {
            Event event = Event.create("Active Event", LocalDate.of(2026, 6, 1), "Location", "OOB", null, null);
            event.publish();

            when(eventManagementService.listEventsByStatus(any(), any()))
                    .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1));

            mockMvc.perform(
                            get("/api/events")
                                    .param("status", "ACTIVE")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0].status").value("ACTIVE"));
        }
    }

    @Nested
    @DisplayName("GET /api/events/{id}")
    class GetEventTests {

        @Test
        @DisplayName("should return event detail with status-appropriate links")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldGetEventWithHateoasLinks() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2026, 6, 1),
                    "Location",
                    "OOB",
                    null,
                    null
            );

            when(eventManagementService.getEvent(any())).thenReturn(event);

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._templates.default.method").value("PATCH"))  // EDIT
                    .andExpect(jsonPath("$._templates.publishEvent.target").exists())   // PUBLISH
                    .andExpect(jsonPath("$._templates.cancelEvent.target").exists());   // CANCEL
        }

        @Test
        @DisplayName("should return 404 for non-existent event")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldReturn404ForNonExistentEvent() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            when(eventManagementService.getEvent(any())).thenThrow(new EventNotFoundException(new EventId(nonExistentId)));

            mockMvc.perform(
                            get("/api/events/{id}", nonExistentId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should include POST affordance on registrations link for ACTIVE event with future date")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldIncludeRegisterAffordanceForActiveEvent() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event activeEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .build();
            activeEvent.publish();

            when(eventManagementService.getEvent(any())).thenReturn(activeEvent);

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.registrations.href").exists())
                    .andExpect(jsonPath("$._templates.registerForEvent.method").value("POST"));
        }

        @Test
        @DisplayName("should not include POST affordance on registrations link for DRAFT event")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldNotIncludeRegisterAffordanceForDraftEvent() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event draftEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .build();

            when(eventManagementService.getEvent(any())).thenReturn(draftEvent);

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.registrations.href").exists())
                    .andExpect(jsonPath("$._templates.registerForEvent").doesNotExist());
        }

        @Test
        @DisplayName("should not include POST affordance on registrations link for FINISHED event")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldNotIncludeRegisterAffordanceForFinishedEvent() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event finishedEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().minusDays(5))
                    .build();
            finishedEvent.publish();
            finishedEvent.finish();

            when(eventManagementService.getEvent(any())).thenReturn(finishedEvent);

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.registrations.href").exists())
                    .andExpect(jsonPath("$._templates.registerForEvent").doesNotExist());
        }
    }

    @Nested
    @DisplayName("POST /api/events/{id}/publish")
    class PublishEventTests {

        @Test
        @DisplayName("should return 204 No Content")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldPublishEvent() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(
                            post("/api/events/{id}/publish", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("POST /api/events/{id}/cancel")
    class CancelEventTests {

        @Test
        @DisplayName("should return 204 No Content")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldCancelEvent() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(
                            post("/api/events/{id}/cancel", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("POST /api/events/{id}/finish")
    class FinishEventTests {

        @Test
        @DisplayName("should return 204 No Content")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldFinishEvent() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(
                            post("/api/events/{id}/finish", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());
        }
    }
}
