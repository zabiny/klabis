package com.klabis.events.infrastructure.restapi;

import tools.jackson.databind.ObjectMapper;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserService;
import com.klabis.events.EventTestDataBuilder;
import com.klabis.events.application.DuplicateOrisImportException;
import com.klabis.events.application.EventManagementService;
import com.klabis.events.domain.EventCreateEventBuilder;
import com.klabis.events.domain.EventUpdateEventBuilder;
import com.klabis.events.application.EventNotFoundException;
import com.klabis.events.application.EventRegistrationService;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventFilter;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.EventRegistrationCreateEventRegistrationBuilder;
import com.klabis.events.domain.EventStatus;
import com.klabis.events.domain.SiCardNumber;
import com.klabis.events.EventId;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("EventController API tests")
@WebMvcTest(controllers = {EventController.class, EventsExceptionHandler.class},
        properties = "oris-integration.enabled=true")
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
class EventControllerTest {

    private static final String ADMIN_USERNAME = "admin";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventManagementService eventManagementService;

    @MockitoBean
    private EventRegistrationService eventRegistrationService;

    @MockitoBean
    private Members members;

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

            when(eventManagementService.listEvents(any(EventFilter.class), any()))
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
        @DisplayName("regular user with EVENTS:READ only should not see DRAFT events — calls listEvents with byNotHavingStatus(DRAFT)")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldExcludeDraftEventsForRegularUser() throws Exception {
            Event activeEvent = EventTestDataBuilder.anEvent().build();
            activeEvent.publish();

            when(eventManagementService.listEvents(any(EventFilter.class), any()))
                    .thenReturn(new PageImpl<>(List.of(activeEvent), PageRequest.of(0, 10), 1));

            mockMvc.perform(
                            get("/api/events")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList").isArray())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0].status").doesNotExist());

            verify(eventManagementService).listEvents(eq(EventFilter.byNotHavingStatus(EventStatus.DRAFT)), any());
        }

        @Test
        @DisplayName("manager with EVENTS:MANAGE should see all events including DRAFT — calls listEvents with none()")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldCallListEventsForManager() throws Exception {
            Event draftEvent = EventTestDataBuilder.anEvent().build();
            Event activeEvent = EventTestDataBuilder.anEvent().buildPublished();

            when(eventManagementService.listEvents(any(EventFilter.class), any()))
                    .thenReturn(new PageImpl<>(List.of(draftEvent, activeEvent), PageRequest.of(0, 10), 2));

            mockMvc.perform(
                            get("/api/events")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList").isArray());

            verify(eventManagementService).listEvents(eq(EventFilter.none()), any());
        }

        @Test
        @DisplayName("regular user filtering by DRAFT status should get empty results")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldReturnEmptyForDraftStatusFilterWithoutManageAuthority() throws Exception {
            mockMvc.perform(
                            get("/api/events")
                                    .param("status", "DRAFT")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.totalElements").value(0));

            verify(eventManagementService, never()).listEvents(any(EventFilter.class), any());
        }

        @Test
        @DisplayName("manager filtering by DRAFT status should get DRAFT events")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldReturnDraftEventsForDraftStatusFilterWithManageAuthority() throws Exception {
            Event draftEvent = EventTestDataBuilder.anEvent().build();

            when(eventManagementService.listEvents(any(EventFilter.class), any()))
                    .thenReturn(new PageImpl<>(List.of(draftEvent), PageRequest.of(0, 10), 1));

            mockMvc.perform(
                            get("/api/events")
                                    .param("status", "DRAFT")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0].status").value("DRAFT"));

            verify(eventManagementService).listEvents(eq(EventFilter.byStatus(EventStatus.DRAFT)), any());
        }

        @Test
        @DisplayName("should filter by status — status field hidden for regular user")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldFilterEventsByStatus() throws Exception {
            Event event = Event.create(EventCreateEventBuilder.builder().name("Active Event").eventDate(LocalDate.of(2026, 6, 1)).location("Location").organizer("OOB").build());
            event.publish();

            when(eventManagementService.listEvents(any(EventFilter.class), any()))
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
        @DisplayName("regular user should get 404 for DRAFT event")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldReturn404ForDraftEventWithoutManageAuthority() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event draftEvent = EventTestDataBuilder.anEvent().build();

            when(eventManagementService.getEvent(any())).thenReturn(draftEvent);

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

            when(eventManagementService.getEvent(any())).thenReturn(draftEvent);

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

            when(eventManagementService.getEvent(any())).thenReturn(activeEvent);

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

            when(eventManagementService.getEvent(any())).thenReturn(event);

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
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
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

            when(eventManagementService.getEvent(any())).thenReturn(activeEvent);

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

            when(eventManagementService.getEvent(any())).thenReturn(activeEvent);

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

            when(eventManagementService.getEvent(any())).thenReturn(event);
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

            when(eventManagementService.getEvent(any())).thenReturn(event);
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

            when(eventManagementService.getEvent(any())).thenReturn(event);
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

            when(eventManagementService.getEvent(any())).thenReturn(event);
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

            when(eventManagementService.getEvent(any())).thenReturn(event);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.registrationDeadline").value("2026-08-10"));
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

    @Nested
    @DisplayName("POST /api/events/import")
    class ImportEventTests {

        @Test
        @DisplayName("should return 201 with Location header on successful import")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldImportEventSuccessfully() throws Exception {
            Event importedEvent = EventTestDataBuilder.anEvent().withName("ORIS Sprint Race").build();
            when(eventManagementService.importEventFromOris(9876)).thenReturn(importedEvent);

            mockMvc.perform(
                            post("/api/events/import")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"orisId\": 9876}")
                    )
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));
        }

        @Test
        @DisplayName("should return 403 without EVENTS:MANAGE authority")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldReturn403WithoutEventsManageAuthority() throws Exception {
            mockMvc.perform(
                            post("/api/events/import")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"orisId\": 9876}")
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 409 when ORIS event already imported")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn409WhenDuplicate() throws Exception {
            when(eventManagementService.importEventFromOris(9876))
                    .thenThrow(new DuplicateOrisImportException(9876));

            mockMvc.perform(
                            post("/api/events/import")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"orisId\": 9876}")
                    )
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 404 when ORIS event not found")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn404WhenOrisEventNotFound() throws Exception {
            when(eventManagementService.importEventFromOris(9999))
                    .thenThrow(new EventNotFoundException(9999));

            mockMvc.perform(
                            post("/api/events/import")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"orisId\": 9999}")
                    )
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/events — importFromOris affordance visibility")
    class ImportAffordanceTests {

        @Test
        @DisplayName("should include importFromOris affordance when OrisApiClient bean is present")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldIncludeImportAffordanceWhenOrisActive() throws Exception {
            when(eventManagementService.listEvents(any(EventFilter.class), any()))
                    .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(
                            get("/api/events")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.importEvent.target").exists());
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

            when(eventManagementService.listEvents(any(EventFilter.class), any()))
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

            when(eventManagementService.listEvents(any(EventFilter.class), any()))
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

            when(eventManagementService.listEvents(any(EventFilter.class), any()))
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

            when(eventManagementService.listEvents(any(EventFilter.class), any()))
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

            when(eventManagementService.listEvents(any(EventFilter.class), any()))
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

            when(eventManagementService.listEvents(any(EventFilter.class), any()))
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

            when(eventManagementService.listEvents(any(EventFilter.class), any()))
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

            when(eventManagementService.listEvents(any(EventFilter.class), any()))
                    .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1));

            mockMvc.perform(
                            get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0].status").value("ACTIVE"));
        }
    }

}
