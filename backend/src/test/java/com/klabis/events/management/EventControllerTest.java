package com.klabis.events.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.events.EventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
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
class EventControllerTest {

    private static final String ADMIN_USERNAME = "admin";
    private static final String MEMBERS_READ_AUTHORITY = "MEMBERS:READ";
    private static final String EVENTS_MANAGE_AUTHORITY = "EVENTS:MANAGE";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventManagementService eventManagementService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("POST /api/events")
    class CreateEventTests {

        @Test
        @DisplayName("should return 201 with Location header and HAL+FORMS links")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {EVENTS_MANAGE_AUTHORITY})
        void shouldCreateEventWithValidData() throws Exception {
            UUID eventId = UUID.randomUUID();
            CreateEventCommand command = new CreateEventCommand(
                    "Spring Cup 2026",
                    LocalDate.of(2026, 3, 15),
                    "Forest Park",
                    "OOB",
                    "https://example.com/spring-cup",
                    null
            );

            EventDto eventDto = new EventDto(
                    eventId,
                    "Spring Cup 2026",
                    LocalDate.of(2026, 3, 15),
                    "Forest Park",
                    "OOB",
                    "https://example.com/spring-cup",
                    null,
                    EventStatus.DRAFT
            );

            when(eventManagementService.createEvent(any(CreateEventCommand.class))).thenReturn(eventId);
            when(eventManagementService.getEvent(eventId)).thenReturn(eventDto);

            mockMvc.perform(
                            post("/api/events")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(command))
                    )
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.id").value(eventId.toString()))
                    .andExpect(jsonPath("$.name").value("Spring Cup 2026"))
                    .andExpect(jsonPath("$.location").value("Forest Park"))
                    .andExpect(jsonPath("$.organizer").value("OOB"))
                    .andExpect(jsonPath("$.status").value("DRAFT"))
                    .andExpect(jsonPath("$._links.self.href").exists());
        }

        @Test
        @DisplayName("should return 403 without EVENTS:MANAGE authority")
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
        @DisplayName("should return 400 with invalid data")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {EVENTS_MANAGE_AUTHORITY})
        void shouldReturn400WithInvalidData() throws Exception {
            CreateEventCommand command = new CreateEventCommand(
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
        @DisplayName("should return 200 with updated event")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {EVENTS_MANAGE_AUTHORITY})
        void shouldUpdateEventSuccessfully() throws Exception {
            UUID eventId = UUID.randomUUID();
            UpdateEventCommand updateCommand = new UpdateEventCommand(
                    "Updated Event",
                    LocalDate.of(2026, 5, 15),
                    "Updated Location",
                    "PRG",
                    "https://updated.com",
                    null
            );

            EventDto eventDto = new EventDto(
                    eventId,
                    "Updated Event",
                    LocalDate.of(2026, 5, 15),
                    "Updated Location",
                    "PRG",
                    "https://updated.com",
                    null,
                    EventStatus.DRAFT
            );

            when(eventManagementService.getEvent(eventId)).thenReturn(eventDto);

            mockMvc.perform(
                            patch("/api/events/{id}", eventId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(updateCommand))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Event"))
                    .andExpect(jsonPath("$.location").value("Updated Location"))
                    .andExpect(jsonPath("$.organizer").value("PRG"));
        }

        @Test
        @DisplayName("should return 403 without EVENTS:MANAGE authority")
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

    @Nested
    @DisplayName("GET /api/events")
    class ListEventsTests {

        @Test
        @DisplayName("should return paginated list with HAL+FORMS")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {EVENTS_MANAGE_AUTHORITY})
        void shouldListEventsWithPagination() throws Exception {
            EventSummaryDto event1 = new EventSummaryDto(
                    UUID.randomUUID(),
                    "Event 1",
                    LocalDate.of(2026, 6, 1),
                    "Location 1",
                    "OOB",
                    EventStatus.DRAFT
            );
            EventSummaryDto event2 = new EventSummaryDto(
                    UUID.randomUUID(),
                    "Event 2",
                    LocalDate.of(2026, 7, 1),
                    "Location 2",
                    "PRG",
                    EventStatus.ACTIVE
            );

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
        @WithMockUser(username = ADMIN_USERNAME, authorities = {EVENTS_MANAGE_AUTHORITY})
        void shouldFilterEventsByStatus() throws Exception {
            EventSummaryDto event = new EventSummaryDto(
                    UUID.randomUUID(),
                    "Active Event",
                    LocalDate.of(2026, 6, 1),
                    "Location",
                    "OOB",
                    EventStatus.ACTIVE
            );

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
        @WithMockUser(username = ADMIN_USERNAME, authorities = {EVENTS_MANAGE_AUTHORITY})
        void shouldGetEventWithHateoasLinks() throws Exception {
            UUID eventId = UUID.randomUUID();
            EventDto eventDto = new EventDto(
                    eventId,
                    "Test Event",
                    LocalDate.of(2026, 6, 1),
                    "Location",
                    "OOB",
                    null,
                    null,
                    EventStatus.DRAFT
            );

            when(eventManagementService.getEvent(eventId)).thenReturn(eventDto);

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
        @WithMockUser(username = ADMIN_USERNAME, authorities = {EVENTS_MANAGE_AUTHORITY})
        void shouldReturn404ForNonExistentEvent() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            when(eventManagementService.getEvent(nonExistentId)).thenThrow(new EventNotFoundException(nonExistentId));

            mockMvc.perform(
                            get("/api/events/{id}", nonExistentId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/events/{id}/publish")
    class PublishEventTests {

        @Test
        @DisplayName("should transition event to ACTIVE")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {EVENTS_MANAGE_AUTHORITY})
        void shouldPublishEvent() throws Exception {
            UUID eventId = UUID.randomUUID();
            EventDto eventDto = new EventDto(
                    eventId,
                    "Test Event",
                    LocalDate.of(2026, 6, 1),
                    "Location",
                    "OOB",
                    null,
                    null,
                    EventStatus.ACTIVE
            );

            when(eventManagementService.getEvent(eventId)).thenReturn(eventDto);

            mockMvc.perform(
                            post("/api/events/{id}/publish", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }
    }

    @Nested
    @DisplayName("POST /api/events/{id}/cancel")
    class CancelEventTests {

        @Test
        @DisplayName("should transition event to CANCELLED")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {EVENTS_MANAGE_AUTHORITY})
        void shouldCancelEvent() throws Exception {
            UUID eventId = UUID.randomUUID();
            EventDto eventDto = new EventDto(
                    eventId,
                    "Test Event",
                    LocalDate.of(2026, 6, 1),
                    "Location",
                    "OOB",
                    null,
                    null,
                    EventStatus.CANCELLED
            );

            when(eventManagementService.getEvent(eventId)).thenReturn(eventDto);

            mockMvc.perform(
                            post("/api/events/{id}/cancel", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }
    }

    @Nested
    @DisplayName("POST /api/events/{id}/finish")
    class FinishEventTests {

        @Test
        @DisplayName("should transition event to FINISHED")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {EVENTS_MANAGE_AUTHORITY})
        void shouldFinishEvent() throws Exception {
            UUID eventId = UUID.randomUUID();
            EventDto eventDto = new EventDto(
                    eventId,
                    "Test Event",
                    LocalDate.of(2026, 6, 1),
                    "Location",
                    "OOB",
                    null,
                    null,
                    EventStatus.FINISHED
            );

            when(eventManagementService.getEvent(eventId)).thenReturn(eventDto);

            mockMvc.perform(
                            post("/api/events/{id}/finish", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("FINISHED"));
        }
    }
}
