package com.klabis.events.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.events.EventId;
import com.klabis.events.EventTestDataBuilder;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.EventNotFoundException;
import com.klabis.events.application.EventRegistrationPort;
import com.klabis.events.domain.*;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("EventController API tests")
@WebMvcTest(controllers = {EventController.class, EventsExceptionHandler.class, EventDetailsPostprocessor.class, EventSummaryPostprocessor.class})
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
@WithPostprocessors
class EventControllerTest {

    private static final String ADMIN_USERNAME = "admin";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventManagementPort eventManagementService;

    @MockitoBean
    private EventRegistrationPort eventRegistrationService;

    @MockitoBean
    private Members members;

    @Nested
    @DisplayName("POST /api/events")
    class CreateEventTests {

        @Test
        @DisplayName("should return 201 with Location header and no body")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldCreateEventWithValidData() throws Exception {
            Event createdEvent = EventTestDataBuilder.anEvent().withName("Spring Cup 2026").build();
            Event.CreateEvent command = EventCreateEventBuilder.builder()
                    .name("Spring Cup 2026")
                    .eventDate(LocalDate.of(2026, 3, 15))
                    .location("Forest Park")
                    .organizer("OOB")
                    .build();

            when(eventManagementService.createEvent(any(Event.CreateEvent.class))).thenReturn(createdEvent);

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
            Event.CreateEvent command = EventCreateEventBuilder.builder()
                    .name("Test Event")
                    .eventDate(LocalDate.of(2026, 5, 1))
                    .location("Location")
                    .organizer("OOB")
                    .build();

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
            Event.CreateEvent command = EventCreateEventBuilder.builder()
                    .name("")
                    .eventDate(LocalDate.of(2026, 5, 1))
                    .location("Location")
                    .organizer("OOB")
                    .build();

            mockMvc.perform(
                            post("/api/events")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(command))
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 201 when location is omitted")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldCreateEventWithoutLocation() throws Exception {
            Event createdEvent = EventTestDataBuilder.anEvent().withName("Event No Location").build();
            when(eventManagementService.createEvent(any(Event.CreateEvent.class))).thenReturn(createdEvent);

            mockMvc.perform(
                            post("/api/events")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"name\":\"Event No Location\",\"eventDate\":\"2026-08-20\",\"organizer\":\"OOB\"}")
                    )
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));
        }

        @Test
        @DisplayName("should return 201 when registrationDeadline is provided")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldCreateEventWithRegistrationDeadline() throws Exception {
            Event createdEvent = EventTestDataBuilder.anEvent().withName("Deadline Event 2026").build();
            Event.CreateEvent command = EventCreateEventBuilder.builder()
                    .name("Deadline Event 2026")
                    .eventDate(LocalDate.of(2026, 8, 20))
                    .location("Forest Park")
                    .organizer("OOB")
                    .registrationDeadline(LocalDate.of(2026, 8, 10))
                    .build();

            when(eventManagementService.createEvent(any(Event.CreateEvent.class))).thenReturn(createdEvent);

            mockMvc.perform(
                            post("/api/events")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(command))
                    )
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));
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
            Event.UpdateEvent updateCommand = EventUpdateEventBuilder.builder()
                    .name("Updated Event")
                    .eventDate(LocalDate.of(2026, 5, 15))
                    .location("Updated Location")
                    .organizer("PRG")
                    .websiteUrl("https://updated.com")
                    .build();

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
            Event.UpdateEvent command = EventUpdateEventBuilder.builder()
                    .name("Updated Event")
                    .eventDate(LocalDate.of(2026, 5, 1))
                    .location("Location")
                    .organizer("OOB")
                    .build();

            mockMvc.perform(
                            patch("/api/events/{id}", eventId)
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(command))
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 204 when location is null in update")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldUpdateEventWithNullLocation() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event.UpdateEvent updateCommand = EventUpdateEventBuilder.builder()
                    .name("Updated Event No Location")
                    .eventDate(LocalDate.of(2026, 5, 15))
                    .location(null)
                    .organizer("PRG")
                    .build();

            mockMvc.perform(
                            patch("/api/events/{id}", eventId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(updateCommand))
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 204 when updating with registrationDeadline")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldUpdateEventWithRegistrationDeadline() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event.UpdateEvent updateCommand = EventUpdateEventBuilder.builder()
                    .name("Updated Race")
                    .eventDate(LocalDate.of(2026, 9, 20))
                    .location("Forest")
                    .organizer("OOB")
                    .registrationDeadline(LocalDate.of(2026, 9, 10))
                    .build();

            mockMvc.perform(
                            patch("/api/events/{id}", eventId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(updateCommand))
                    )
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("GET /api/events")
    class ListEventsTests {

        @Test
        @DisplayName("should return paginated list with HAL+FORMS for manager with EVENTS:MANAGE")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldListEventsWithPaginationForManager() throws Exception {
            Event event1 = Event.create(EventCreateEventBuilder.builder().name("Event 1").eventDate(LocalDate.of(2026, 6, 1)).location("Location 1").organizer("OOB").build());
            Event event2 = Event.create(EventCreateEventBuilder.builder().name("Event 2").eventDate(LocalDate.of(2026, 7, 1)).location("Location 2").organizer("PRG").build());
            event2.publish();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
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
        @DisplayName("regular user with EVENTS:READ only should not see DRAFT events — calls listEvents with none() and canManageEvents=false")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldExcludeDraftEventsForRegularUser() throws Exception {
            Event activeEvent = EventTestDataBuilder.anEvent().build();
            activeEvent.publish();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(activeEvent), PageRequest.of(0, 10), 1));

            mockMvc.perform(
                            get("/api/events")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList").isArray())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0].status").doesNotExist());

            verify(eventManagementService).listEvents(eq(EventFilter.none()), any(), eq(false));
        }

        @Test
        @DisplayName("manager with EVENTS:MANAGE should see all events including DRAFT — calls listEvents with none() and canManageEvents=true")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldCallListEventsForManager() throws Exception {
            Event draftEvent = EventTestDataBuilder.anEvent().build();
            Event activeEvent = EventTestDataBuilder.anEvent().buildPublished();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(draftEvent, activeEvent), PageRequest.of(0, 10), 2));

            mockMvc.perform(
                            get("/api/events")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList").isArray());

            verify(eventManagementService).listEvents(eq(EventFilter.none()), any(), eq(true));
        }

        @Test
        @DisplayName("should NOT include importFromOris affordance when oris profile is inactive")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldNotIncludeImportAffordanceWhenOrisInactive() throws Exception {
            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(
                            get("/api/events")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.importEvent").doesNotExist());
        }

        @Test
        @DisplayName("regular user filtering by DRAFT status should get empty results — service called with canManageEvents=false")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldReturnEmptyForDraftStatusFilterWithoutManageAuthority() throws Exception {
            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(
                            get("/api/events")
                                    .param("status", "DRAFT")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.totalElements").value(0));

            verify(eventManagementService).listEvents(eq(EventFilter.byStatus(EventStatus.DRAFT)), any(), eq(false));
        }

        @Test
        @DisplayName("manager filtering by DRAFT status should get DRAFT events — canManageEvents=true")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldReturnDraftEventsForDraftStatusFilterWithManageAuthority() throws Exception {
            Event draftEvent = EventTestDataBuilder.anEvent().build();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(draftEvent), PageRequest.of(0, 10), 1));

            mockMvc.perform(
                            get("/api/events")
                                    .param("status", "DRAFT")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0].status").value("DRAFT"));

            verify(eventManagementService).listEvents(eq(EventFilter.byStatus(EventStatus.DRAFT)), any(), eq(true));
        }

        @Test
        @DisplayName("should filter by status — status field hidden for regular user")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldFilterEventsByStatus() throws Exception {
            Event event = Event.create(EventCreateEventBuilder.builder().name("Active Event").eventDate(LocalDate.of(2026, 6, 1)).location("Location").organizer("OOB").build());
            event.publish();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1));

            mockMvc.perform(
                            get("/api/events")
                                    .param("status", "ACTIVE")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0].status").doesNotExist());
        }
    }

    @Nested
    @DisplayName("GET /api/events/{id}")
    class GetEventTests {

        @Test
        @DisplayName("regular user should get 404 for DRAFT event — service enforces visibility")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldReturn404ForDraftEventWithoutManageAuthority() throws Exception {
            UUID eventId = UUID.randomUUID();

            when(eventManagementService.getEvent(any(), eq(false)))
                    .thenThrow(new EventNotFoundException(new EventId(eventId)));

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("manager should see DRAFT event detail")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldReturnDraftEventForManager() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event draftEvent = EventTestDataBuilder.anEvent().build();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(draftEvent);

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DRAFT"));
        }

        @Test
        @DisplayName("regular user should see ACTIVE event detail")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldReturnActiveEventForRegularUser() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event activeEvent = EventTestDataBuilder.anEvent().buildPublished();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(activeEvent);

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("should return event detail with status-appropriate links")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldGetEventWithHateoasLinks() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event event = Event.create(EventCreateEventBuilder.builder().name("Test Event").eventDate(LocalDate.of(2026, 6, 1)).location("Location").organizer("OOB").build());

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(event);

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._templates.updateEvent.method").value("PATCH"))  // EDIT
                    .andExpect(jsonPath("$._templates.publishEvent.target").exists())   // PUBLISH
                    .andExpect(jsonPath("$._templates.cancelEvent.target").exists());   // CANCEL
        }

        @Test
        @DisplayName("should return 404 for non-existent event")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldReturn404ForNonExistentEvent() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenThrow(new EventNotFoundException(new EventId(nonExistentId)));

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

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(activeEvent);

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.registrations.href").exists())
                    .andExpect(jsonPath("$._templates.registerForEvent.method").value("POST"));
        }

        @Test
        @DisplayName("should not include registrations link for DRAFT event")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldNotIncludeRegistrationsLinkForDraftEvent() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event draftEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .build();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(draftEvent);

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.registrations").doesNotExist())
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

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(finishedEvent);

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.registrations.href").exists())
                    .andExpect(jsonPath("$._templates.registerForEvent").doesNotExist());
        }

        @Test
        @DisplayName("should include unregisterFromEvent affordance when ACTIVE and user is registered")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000001", authorities = {Authority.EVENTS_READ})
        void shouldIncludeUnregisterAffordanceWhenUserIsRegistered() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
            Event activeEvent = spy(EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .build());
            activeEvent.publish();

            EventRegistration registration = EventRegistration.create(
                    EventRegistrationCreateEventRegistrationBuilder.builder()
                            .memberId(memberId).siCardNumber(new SiCardNumber("12345")).build());
            doReturn(java.util.Optional.of(registration)).when(activeEvent).findRegistration(memberId);

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(activeEvent);

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.unregisterFromEvent.method").value("DELETE"))
                    .andExpect(jsonPath("$._templates.registerForEvent").doesNotExist());
        }

        @Test
        @DisplayName("should not include unregisterFromEvent affordance when ACTIVE and user is NOT registered")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000002", authorities = {Authority.EVENTS_READ})
        void shouldNotIncludeUnregisterAffordanceWhenUserIsNotRegistered() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
            Event activeEvent = spy(EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .build());
            activeEvent.publish();

            doReturn(java.util.Optional.empty()).when(activeEvent).findRegistration(memberId);

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(activeEvent);

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.unregisterFromEvent").doesNotExist())
                    .andExpect(jsonPath("$._templates.registerForEvent.method").value("POST"));
        }

        @Test
        @DisplayName("should embed registrationDtoList in response")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldEmbedRegistrationsInEventResponse() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.randomUUID());
            Event event = EventTestDataBuilder.anEvent().buildPublished();

            EventRegistration registration = EventRegistration.create(
                    EventRegistrationCreateEventRegistrationBuilder.builder()
                            .memberId(memberId).siCardNumber(new SiCardNumber("12345")).build());
            MemberDto memberDto = new MemberDto(memberId.value(), "Jan", "Novak", "jan@example.com");

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(event);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of(registration));
            when(members.findByIds(any())).thenReturn(Map.of(memberId, memberDto));

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.registrationDtoList").isArray())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0].firstName").value("Jan"))
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0].lastName").value("Novak"));
        }

        @Test
        @DisplayName("should include coordinator link when event has a coordinator")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.MEMBERS_READ})
        void shouldIncludeCoordinatorLinkWhenCoordinatorIsSet() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId coordinatorId = new MemberId(UUID.randomUUID());
            Event event = EventTestDataBuilder.anEvent()
                    .withCoordinator(coordinatorId)
                    .buildPublished();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(event);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.coordinator.href").value(
                            containsString("/api/members/" + coordinatorId.value())));
        }

        @Test
        @DisplayName("should not include coordinator link when event has no coordinator")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldNotIncludeCoordinatorLinkWhenCoordinatorIsNotSet() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event event = EventTestDataBuilder.anEvent().buildPublished();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(event);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.coordinator").doesNotExist());
        }

        @Test
        @DisplayName("should return empty embedded registrationDtoList when no registrations")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldEmbedEmptyRegistrationsListWhenNoRegistrations() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event event = EventTestDataBuilder.anEvent().buildPublished();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(event);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());
            when(members.findByIds(any())).thenReturn(Map.of());

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.registrationDtoList").isArray())
                    .andExpect(jsonPath("$._embedded.registrationDtoList").isEmpty());
        }

        @Test
        @DisplayName("should include registrationDeadline in event detail response")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldIncludeRegistrationDeadlineInEventDetail() throws Exception {
            UUID eventId = UUID.randomUUID();
            LocalDate deadline = LocalDate.of(2026, 8, 10);
            Event event = EventTestDataBuilder.anEvent()
                    .withRegistrationDeadline(deadline)
                    .build();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(event);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.registrationDeadline").value("2026-08-10"));
        }

        @Test
        @DisplayName("registerForEvent template should include category property with inline options when event has categories")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000099", authorities = {Authority.EVENTS_READ})
        void shouldIncludeCategoryPropertyWithInlineOptionsInRegisterForEventTemplate() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event activeEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .withCategories(List.of("M21", "W21", "M35"))
                    .buildPublished();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(activeEvent);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.registerForEvent.properties[?(@.name=='category')]").exists())
                    .andExpect(jsonPath("$._templates.registerForEvent.properties[?(@.name=='category')].options.inline").isArray())
                    .andExpect(jsonPath("$._templates.registerForEvent.properties[?(@.name=='category')].options.inline[0]").value("M21"))
                    .andExpect(jsonPath("$._templates.registerForEvent.properties[?(@.name=='category')].options.inline[1]").value("W21"))
                    .andExpect(jsonPath("$._templates.registerForEvent.properties[?(@.name=='category')].options.inline[2]").value("M35"));
        }

        @Test
        @DisplayName("registerForEvent template should have no category options when event has no categories")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000099", authorities = {Authority.EVENTS_READ})
        void shouldHaveNoCategoryOptionsWhenEventHasNoCategories() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event activeEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .buildPublished();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(activeEvent);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.registerForEvent.properties[?(@.name=='category')].options").doesNotExist());
        }

        @Test
        @DisplayName("ACTIVE event detail should NOT contain finishEvent affordance")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void activeEventDetailShouldNotContainFinishEventAffordance() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event activeEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .build();
            activeEvent.publish();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(activeEvent);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.finishEvent").doesNotExist());
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
    @DisplayName("GET /api/events — list item extended fields and links (tasks 3.6, 3.7)")
    class ListEventsExtendedFieldsTests {

        @Test
        @DisplayName("list item includes websiteUrl and registrationDeadline")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldIncludeWebsiteUrlAndRegistrationDeadlineInListItem() throws Exception {
            LocalDate deadline = LocalDate.of(2026, 8, 10);
            Event event = EventTestDataBuilder.anEvent()
                    .withWebsiteUrl("https://example.com/event")
                    .withRegistrationDeadline(deadline)
                    .buildPublished();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1));

            mockMvc.perform(
                            get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0].websiteUrl").value("https://example.com/event"))
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0].registrationDeadline").value("2026-08-10"));
        }

        @Test
        @DisplayName("list item includes coordinator link when event has coordinator")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.MEMBERS_READ})
        void shouldIncludeCoordinatorLinkInListItem() throws Exception {
            MemberId coordinatorId = new MemberId(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
            Event event = EventTestDataBuilder.anEvent()
                    .withCoordinator(coordinatorId)
                    .buildPublished();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1));

            mockMvc.perform(
                            get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0]._links.coordinator.href")
                            .value(containsString("/api/members/" + coordinatorId.value())));
        }

        @Test
        @DisplayName("list item does not include coordinator link when event has no coordinator")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldNotIncludeCoordinatorLinkWhenNoCoordinator() throws Exception {
            Event event = EventTestDataBuilder.anEvent().buildPublished();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1));

            mockMvc.perform(
                            get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0]._links.coordinator").doesNotExist());
        }

        @Test
        @DisplayName("list item includes registerForEvent affordance when registrations are open and user not registered")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000099",
                authorities = {Authority.EVENTS_READ})
        void shouldIncludeRegisterAffordanceOnListItemWhenRegistrationsOpen() throws Exception {
            Event event = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .buildPublished();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1));

            mockMvc.perform(
                            get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0]._templates.registerForEvent.method")
                            .value("POST"));
        }

        @Test
        @DisplayName("list item does not include registration affordance for closed registrations")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldNotIncludeRegisterAffordanceWhenRegistrationsClosed() throws Exception {
            Event pastEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().minusDays(5))
                    .buildPublished();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(pastEvent), PageRequest.of(0, 10), 1));

            mockMvc.perform(
                            get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0]._templates.registerForEvent").doesNotExist())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0]._templates.unregisterFromEvent").doesNotExist());
        }

        @Test
        @DisplayName("list item does not include registration affordance when deadline passed")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldNotIncludeRegisterAffordanceWhenDeadlinePassed() throws Exception {
            Event eventWithPastDeadline = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .withRegistrationDeadline(LocalDate.now().minusDays(1))
                    .buildPublished();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(eventWithPastDeadline), PageRequest.of(0, 10), 1));

            mockMvc.perform(
                            get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0]._templates.registerForEvent").doesNotExist());
        }

        @Test
        @DisplayName("status field hidden for regular users without EVENTS:MANAGE")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldHideStatusFieldForRegularUsers() throws Exception {
            Event event = EventTestDataBuilder.anEvent().buildPublished();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1));

            mockMvc.perform(
                            get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0].status").doesNotExist());
        }

        @Test
        @DisplayName("status field visible for users with EVENTS:MANAGE")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldShowStatusFieldForManagers() throws Exception {
            Event event = EventTestDataBuilder.anEvent().buildPublished();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1));

            mockMvc.perform(
                            get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0].status").value("ACTIVE"));
        }
    }

    @Nested
    @DisplayName("GET /api/events — filter parameter wiring (tasks 3.1-3.7)")
    class ListEventsFilterParamTests {

        @Test
        @DisplayName("q param is passed to service as EventFilter.fulltextQuery")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void qParamIsPassedAsFulltextQuery() throws Exception {
            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(
                            get("/api/events")
                                    .param("q", "jihlava")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk());

            verify(eventManagementService).listEvents(
                    eq(EventFilter.none().withFulltext("jihlava")), any(), anyBoolean());
        }

        @Test
        @DisplayName("organizer param is passed to service as EventFilter.organizer")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void organizerParamIsPassedToFilter() throws Exception {
            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(
                            get("/api/events")
                                    .param("organizer", "OOB")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk());

            verify(eventManagementService).listEvents(
                    eq(EventFilter.byOrganizer("OOB")), any(), anyBoolean());
        }

        @Test
        @DisplayName("coordinator param is passed to service as EventFilter.coordinator")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void coordinatorParamIsPassedToFilter() throws Exception {
            UUID coordinatorUuid = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
            MemberId coordinatorId = new MemberId(coordinatorUuid);

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(
                            get("/api/events")
                                    .param("coordinator", coordinatorUuid.toString())
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk());

            verify(eventManagementService).listEvents(
                    eq(EventFilter.none().withCoordinator(coordinatorId)), any(), anyBoolean());
        }

        @Test
        @DisplayName("registeredBy=me is resolved to the current user's MemberId")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000042",
                authorities = {Authority.EVENTS_READ})
        void registeredByMeIsResolvedToCurrentMemberId() throws Exception {
            MemberId memberId = new MemberId(UUID.fromString("00000000-0000-0000-0000-000000000042"));

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(
                            get("/api/events")
                                    .param("registeredBy", "me")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk());

            verify(eventManagementService).listEvents(
                    eq(EventFilter.none().withRegisteredBy(memberId)), any(), anyBoolean());
        }

        @Test
        @DisplayName("registeredBy with value other than 'me' returns 400 Bad Request")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000042",
                authorities = {Authority.EVENTS_READ})
        void registeredByWithUnknownValueReturns400() throws Exception {
            mockMvc.perform(
                            get("/api/events")
                                    .param("registeredBy", "someone-else")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("registeredBy=me for user without member profile returns 200 with empty page — silent no-op")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void registeredByMeForUserWithoutMemberProfileReturnsEmptyPage() throws Exception {
            mockMvc.perform(
                            get("/api/events")
                                    .param("registeredBy", "me")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.totalElements").value(0));

            verify(eventManagementService, never()).listEvents(any(), any(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("GET /api/events — list row management affordances")
    class ListRowManagementAffordancesTests {

        private static String tpl(String name) {
            return "$._embedded.eventSummaryDtoList[0]._templates." + name;
        }

        @Test
        @DisplayName("DRAFT event row carries updateEvent, publishEvent, cancelEvent affordances")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void draftRowCarriesEditPublishCancelAffordances() throws Exception {
            Event draftEvent = EventTestDataBuilder.anEvent().build();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(draftEvent), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(tpl("updateEvent.method")).value("PATCH"))
                    .andExpect(jsonPath(tpl("publishEvent.target")).exists())
                    .andExpect(jsonPath(tpl("cancelEvent.target")).exists());
        }

        @Test
        @DisplayName("ACTIVE event row carries updateEvent, cancelEvent affordances but NOT finishEvent")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void activeRowCarriesEditCancelButNotFinishAffordances() throws Exception {
            Event activeEvent = EventTestDataBuilder.anEvent().buildPublished();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(activeEvent), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(tpl("updateEvent.method")).value("PATCH"))
                    .andExpect(jsonPath(tpl("cancelEvent.target")).exists())
                    .andExpect(jsonPath(tpl("publishEvent")).doesNotExist())
                    .andExpect(jsonPath(tpl("finishEvent")).doesNotExist());
        }

        @Test
        @DisplayName("Non-ORIS DRAFT row does NOT carry syncEventFromOris affordance")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void nonOrisDraftRowDoesNotCarrySyncAffordance() throws Exception {
            Event nonOrisEvent = EventTestDataBuilder.anEvent().build();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(nonOrisEvent), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(tpl("syncEventFromOris")).doesNotExist());
        }

        @Test
        @DisplayName("FINISHED row carries no management affordances")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void finishedRowCarriesNoManagementAffordances() throws Exception {
            Event finishedEvent = EventTestDataBuilder.anEvent().buildFinished();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(finishedEvent), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(tpl("updateEvent")).doesNotExist())
                    .andExpect(jsonPath(tpl("publishEvent")).doesNotExist())
                    .andExpect(jsonPath(tpl("cancelEvent")).doesNotExist())
                    .andExpect(jsonPath(tpl("syncEventFromOris")).doesNotExist());
        }

        @Test
        @DisplayName("CANCELLED row carries no management affordances")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void cancelledRowCarriesNoManagementAffordances() throws Exception {
            Event cancelledEvent = EventTestDataBuilder.anEvent().buildCancelled();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(cancelledEvent), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(tpl("updateEvent")).doesNotExist())
                    .andExpect(jsonPath(tpl("publishEvent")).doesNotExist())
                    .andExpect(jsonPath(tpl("cancelEvent")).doesNotExist())
                    .andExpect(jsonPath(tpl("syncEventFromOris")).doesNotExist());
        }

        @Test
        @DisplayName("Regular member (no EVENTS:MANAGE) sees only register/unregister — no management actions")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000099",
                authorities = {Authority.EVENTS_READ})
        void regularMemberSeesOnlyRegisterUnregisterNotManagementActions() throws Exception {
            Event activeEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .withOrisId(42)
                    .buildPublished();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(activeEvent), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(tpl("registerForEvent.method")).value("POST"))
                    .andExpect(jsonPath(tpl("updateEvent")).doesNotExist())
                    .andExpect(jsonPath(tpl("publishEvent")).doesNotExist())
                    .andExpect(jsonPath(tpl("cancelEvent")).doesNotExist())
                    .andExpect(jsonPath(tpl("syncEventFromOris")).doesNotExist());
        }
    }

}
