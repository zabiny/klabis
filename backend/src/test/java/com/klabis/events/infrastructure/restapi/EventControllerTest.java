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
import com.klabis.events.application.MemberRegistrationSanctionPort;

import com.klabis.events.domain.*;
import com.klabis.members.MemberAccommodationDto;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
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

    @MockitoBean
    private AccommodationListCsvRenderer csvRenderer;

    @Autowired
    private MemberRegistrationSanctionPort memberRegistrationSanctionPort;

    @Nested
    @DisplayName("POST /api/events")
    class CreateEventTests {

        @Test
        @DisplayName("should return 201 with Location header and no body")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldCreateEventWithValidData() throws Exception {
            Event createdEvent = EventTestDataBuilder.anEvent().withName("Spring Cup 2026").build();

            when(eventManagementService.createEvent(any(Event.CreateEvent.class))).thenReturn(createdEvent);

            mockMvc.perform(
                            post("/api/events")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"name\":\"Spring Cup 2026\",\"eventDate\":\"2026-03-15\",\"location\":\"Forest Park\",\"organizer\":\"OOB\"}")
                    )
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));
        }

        @Test
        @DisplayName("should return 403 without EVENTS:MANAGE authority")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldReturn403WithoutEventsManageAuthority() throws Exception {
            mockMvc.perform(
                            post("/api/events")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"name\":\"Test Event\",\"eventDate\":\"2026-05-01\",\"location\":\"Location\",\"organizer\":\"OOB\"}")
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 400 with invalid data")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn400WithInvalidData() throws Exception {
            mockMvc.perform(
                            post("/api/events")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"name\":\"\",\"eventDate\":\"2026-05-01\",\"location\":\"Location\",\"organizer\":\"OOB\"}")
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
        @DisplayName("should return 201 when single deadline is provided in deadlines array")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldCreateEventWithSingleDeadline() throws Exception {
            Event createdEvent = EventTestDataBuilder.anEvent().withName("Deadline Event 2026").build();
            when(eventManagementService.createEvent(any(Event.CreateEvent.class))).thenReturn(createdEvent);

            mockMvc.perform(
                            post("/api/events")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"name\":\"Deadline Event 2026\",\"eventDate\":\"2026-08-20\",\"location\":\"Forest Park\",\"organizer\":\"OOB\",\"deadlines\":[\"2026-08-10\"]}")
                    )
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));

            verify(eventManagementService).createEvent(argThat((Event.CreateEvent cmd) ->
                    cmd.registrationDeadlines().deadline1().map(d -> d.equals(LocalDate.of(2026, 8, 10))).orElse(false)
                    && cmd.registrationDeadlines().deadline2().isEmpty()
            ));
        }

        @Test
        @DisplayName("should return 201 when three deadlines are provided in deadlines array")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldCreateEventWithThreeDeadlines() throws Exception {
            Event createdEvent = EventTestDataBuilder.anEvent().withName("Multi-Deadline Event").build();
            when(eventManagementService.createEvent(any(Event.CreateEvent.class))).thenReturn(createdEvent);

            mockMvc.perform(
                            post("/api/events")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"name\":\"Multi-Deadline Event\",\"eventDate\":\"2026-09-20\",\"organizer\":\"OOB\",\"deadlines\":[\"2026-08-01\",\"2026-08-15\",\"2026-09-01\"]}")
                    )
                    .andExpect(status().isCreated());

            verify(eventManagementService).createEvent(argThat((Event.CreateEvent cmd) ->
                    cmd.registrationDeadlines().deadline1().map(d -> d.equals(LocalDate.of(2026, 8, 1))).orElse(false)
                    && cmd.registrationDeadlines().deadline2().map(d -> d.equals(LocalDate.of(2026, 8, 15))).orElse(false)
                    && cmd.registrationDeadlines().deadline3().map(d -> d.equals(LocalDate.of(2026, 9, 1))).orElse(false)
            ));
        }

        @Test
        @DisplayName("should return 400 when deadlines are out of order")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldRejectOutOfOrderDeadlines() throws Exception {
            mockMvc.perform(
                            post("/api/events")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"name\":\"Bad Deadlines\",\"eventDate\":\"2026-09-20\",\"organizer\":\"OOB\",\"deadlines\":[\"2026-08-15\",\"2026-08-01\"]}")
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when more than 3 deadlines are provided")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldRejectMoreThanThreeDeadlines() throws Exception {
            mockMvc.perform(
                            post("/api/events")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"name\":\"Too Many Deadlines\",\"eventDate\":\"2026-10-20\",\"organizer\":\"OOB\",\"deadlines\":[\"2026-08-01\",\"2026-08-15\",\"2026-09-01\",\"2026-09-15\"]}")
                    )
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /api/events/{id}")
    class UpdateEventTests {

        private Event defaultExistingEvent;

        @org.junit.jupiter.api.BeforeEach
        void setUpExistingEvent() {
            defaultExistingEvent = EventTestDataBuilder.anEvent()
                    .withName("Existing Event")
                    .withDate(LocalDate.of(2026, 5, 1))
                    .withLocation("Existing Location")
                    .withOrganizer("OOB")
                    .build();
            when(eventManagementService.getEvent(any(), eq(true))).thenReturn(defaultExistingEvent);
        }

        @Test
        @DisplayName("should return 204 No Content")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldUpdateEventSuccessfully() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(
                            patch("/api/events/{id}", eventId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"name\":\"Updated Event\",\"eventDate\":\"2026-05-15\",\"location\":\"Updated Location\",\"organizer\":\"PRG\",\"websiteUrl\":\"https://updated.com\"}")
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 403 without EVENTS:MANAGE authority")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldReturn403WhenUpdatingWithoutAuthority() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(
                            patch("/api/events/{id}", eventId)
                                    .contentType("application/json")
                                    .content("{\"name\":\"Updated Event\",\"eventDate\":\"2026-05-01\",\"location\":\"Location\",\"organizer\":\"OOB\"}")
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 204 when location is absent — location kept from existing event")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldUpdateEventWithNullLocation() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(
                            patch("/api/events/{id}", eventId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"name\":\"Updated Event No Location\",\"eventDate\":\"2026-05-15\",\"organizer\":\"PRG\"}")
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 204 when updating with two deadlines")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldUpdateEventWithTwoDeadlines() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(
                            patch("/api/events/{id}", eventId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"name\":\"Updated Race\",\"eventDate\":\"2026-09-20\",\"location\":\"Forest\",\"organizer\":\"OOB\",\"deadlines\":[\"2026-09-01\",\"2026-09-10\"]}")
                    )
                    .andExpect(status().isNoContent());

            verify(eventManagementService).updateEvent(any(), argThat((Event.UpdateEvent cmd) ->
                    cmd.registrationDeadlines().deadline1().map(d -> d.equals(LocalDate.of(2026, 9, 1))).orElse(false)
                    && cmd.registrationDeadlines().deadline2().map(d -> d.equals(LocalDate.of(2026, 9, 10))).orElse(false)
            ));
        }

        @Test
        @DisplayName("regression: PATCH with eventTypeId field must return 204 (not 405)")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldAcceptPatchWithEventTypeId() throws Exception {
            UUID eventId = UUID.randomUUID();
            UUID typeId = UUID.randomUUID();

            mockMvc.perform(
                            patch("/api/events/{id}", eventId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"name\":\"Typed Event\",\"eventDate\":\"2026-06-01\",\"organizer\":\"OOB\",\"eventTypeId\":\"" + typeId + "\"}")
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 400 when update deadlines are out of order")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldRejectOutOfOrderDeadlinesOnUpdate() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(
                            patch("/api/events/{id}", eventId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"name\":\"Bad Deadlines\",\"eventDate\":\"2026-09-20\",\"organizer\":\"OOB\",\"deadlines\":[\"2026-09-10\",\"2026-09-01\"]}")
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("regression: PATCH with only eventTypeId succeeds and passes eventTypeId to service")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldPatchOnlyEventTypeId() throws Exception {
            UUID eventId = UUID.randomUUID();
            UUID typeId = UUID.randomUUID();

            mockMvc.perform(
                            patch("/api/events/{id}", eventId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"eventTypeId\":\"" + typeId + "\"}")
                    )
                    .andExpect(status().isNoContent());

            verify(eventManagementService).updateEvent(any(), argThat((Event.UpdateEvent cmd) ->
                    new com.klabis.events.EventTypeId(typeId).equals(cmd.eventTypeId())
                    && "Existing Event".equals(cmd.name())
                    && "OOB".equals(cmd.organizer())
            ));
        }

        @Test
        @DisplayName("should return 204 and pass ranking and baseEntryFee to service when provided")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldUpdateEventWithRankingAndBaseEntryFee() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(
                            patch("/api/events/{id}", eventId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"ranking\":{\"levelId\":3,\"shortName\":\"A\",\"name\":\"Národní závod\"},\"baseEntryFee\":{\"amount\":350,\"currency\":\"CZK\"}}")
                    )
                    .andExpect(status().isNoContent());

            verify(eventManagementService).updateEvent(any(), argThat((Event.UpdateEvent cmd) ->
                    cmd.ranking() != null
                    && cmd.ranking().levelId() == 3
                    && "A".equals(cmd.ranking().shortName())
                    && "Národní závod".equals(cmd.ranking().name())
                    && cmd.baseEntryFee() != null
                    && new java.math.BigDecimal("350").compareTo(cmd.baseEntryFee().amount()) == 0
                    && "CZK".equals(cmd.baseEntryFee().currency().getCurrencyCode())
            ));
        }

        @Test
        @DisplayName("regression: PATCH with empty body is a no-op and returns 204")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldAcceptEmptyBodyAsNoOp() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(
                            patch("/api/events/{id}", eventId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{}")
                    )
                    .andExpect(status().isNoContent());

            verify(eventManagementService).updateEvent(any(), argThat((Event.UpdateEvent cmd) ->
                    "Existing Event".equals(cmd.name())
                    && LocalDate.of(2026, 5, 1).equals(cmd.eventDate())
                    && "OOB".equals(cmd.organizer())
            ));
        }

        @Test
        @DisplayName("coordinator of the event can update it without EVENTS:MANAGE — returns 204")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000042", authorities = {Authority.EVENTS_READ})
        void coordinatorCanUpdateEventWithoutEventsManage() throws Exception {
            MemberId coordinatorId = new MemberId(UUID.fromString("00000000-0000-0000-0000-000000000042"));
            Event coordinatorEvent = EventTestDataBuilder.anEvent()
                    .withCoordinator(coordinatorId)
                    .build();
            when(eventManagementService.getEvent(any(), eq(true))).thenReturn(coordinatorEvent);

            mockMvc.perform(
                            patch("/api/events/{id}", coordinatorEvent.getId().value())
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"name\":\"Updated By Coordinator\"}")
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("coordinator of event A cannot update event B — returns 403")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000042", authorities = {Authority.EVENTS_READ})
        void coordinatorOfOtherEventIsRejected() throws Exception {
            MemberId otherCoordinatorId = new MemberId(UUID.fromString("00000000-0000-0000-0000-000000000099"));
            Event eventWithDifferentCoordinator = EventTestDataBuilder.anEvent()
                    .withCoordinator(otherCoordinatorId)
                    .build();
            when(eventManagementService.getEvent(any(), eq(true))).thenReturn(eventWithDifferentCoordinator);

            mockMvc.perform(
                            patch("/api/events/{id}", eventWithDifferentCoordinator.getId().value())
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"name\":\"Should Be Rejected\"}")
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("member without EVENTS:MANAGE who is not a coordinator is rejected — returns 403")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000099", authorities = {Authority.EVENTS_READ})
        void nonCoordinatorMemberWithoutManageIsRejected() throws Exception {
            MemberId coordinatorId = new MemberId(UUID.fromString("00000000-0000-0000-0000-000000000042"));
            Event eventWithDifferentCoordinator = EventTestDataBuilder.anEvent()
                    .withCoordinator(coordinatorId)
                    .build();
            when(eventManagementService.getEvent(any(), eq(true))).thenReturn(eventWithDifferentCoordinator);

            mockMvc.perform(
                            patch("/api/events/{id}", eventWithDifferentCoordinator.getId().value())
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"name\":\"Should Be Rejected\"}")
                    )
                    .andExpect(status().isForbidden());
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
        @DisplayName("registrations link href must not contain URI template syntax")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void registrationsLinkHrefMustNotContainUriTemplateSyntax() throws Exception {
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
                    .andExpect(jsonPath("$._links.registrations.href").value(not(containsString("{?sort}"))));
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
        @DisplayName("event detail includes newRegistration link with ?newRegistration=true when user eligible and not registered")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000002", authorities = {Authority.EVENTS_READ})
        void shouldIncludeNewRegistrationLinkForEligibleUnregisteredMember() throws Exception {
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
                    .andExpect(jsonPath("$._links.newRegistration.href").value(
                            containsString("newRegistration=true")));
        }

        @Test
        @DisplayName("event detail does NOT include newRegistration link when user is already registered")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000001", authorities = {Authority.EVENTS_READ})
        void shouldNotIncludeNewRegistrationLinkWhenUserAlreadyRegistered() throws Exception {
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
                    .andExpect(jsonPath("$._links.newRegistration").doesNotExist());
        }

        @Test
        @DisplayName("event detail does NOT include newRegistration link when no member profile")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldNotIncludeNewRegistrationLinkWhenNoMemberProfile() throws Exception {
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
                    .andExpect(jsonPath("$._links.newRegistration").doesNotExist());
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
        @DisplayName("should include coordinator links as array when event has multiple coordinators")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.MEMBERS_READ})
        void shouldIncludeCoordinatorLinksAsArrayWhenMultipleCoordinatorsAreSet() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId coordA = new MemberId(UUID.randomUUID());
            MemberId coordB = new MemberId(UUID.randomUUID());
            Event event = EventTestDataBuilder.anEvent()
                    .withCoordinators(new LinkedHashSet<>(List.of(coordA, coordB)))
                    .buildPublished();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(event);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.coordinators").isArray())
                    .andExpect(jsonPath("$.coordinators", hasSize(2)))
                    .andExpect(jsonPath("$._links.coordinator").isArray())
                    .andExpect(jsonPath("$._links.coordinator", hasSize(2)))
                    .andExpect(jsonPath("$._links.coordinator[0].href").value(
                            containsString("/api/members/" + coordA.value())))
                    .andExpect(jsonPath("$._links.coordinator[1].href").value(
                            containsString("/api/members/" + coordB.value())));
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
        @DisplayName("accommodation-list link present for EVENTS:REGISTRATIONS user")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_REGISTRATIONS})
        void accommodationListLinkPresentForEventsRegistrationsUser() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event event = EventTestDataBuilder.anEvent().buildPublished();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(event);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links['accommodation-list'].href").exists());
        }

        @Test
        @DisplayName("accommodation-list link present for event coordinator")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000042", authorities = {Authority.EVENTS_READ})
        void accommodationListLinkPresentForEventCoordinator() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId coordinatorId = new MemberId(UUID.fromString("00000000-0000-0000-0000-000000000042"));
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
                    .andExpect(jsonPath("$._links['accommodation-list'].href").exists());
        }

        @Test
        @DisplayName("accommodation-list link absent for regular member")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000099", authorities = {Authority.EVENTS_READ})
        void accommodationListLinkAbsentForRegularMember() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId coordinatorId = new MemberId(UUID.fromString("00000000-0000-0000-0000-000000000042"));
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
                    .andExpect(jsonPath("$._links['accommodation-list']").doesNotExist());
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
        @DisplayName("should include deadlines array in event detail response")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldIncludeDeadlinesInEventDetail() throws Exception {
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
                    .andExpect(jsonPath("$.deadlines").isArray())
                    .andExpect(jsonPath("$.deadlines[0]").value("2026-08-10"));
        }

        @Test
        @DisplayName("should include all three deadlines in event detail response when all are set")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldIncludeAllThreeDeadlinesInEventDetail() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event event = EventTestDataBuilder.anEvent()
                    .withRegistrationDeadlines(RegistrationDeadlines.of(
                            LocalDate.of(2026, 7, 1),
                            LocalDate.of(2026, 7, 15),
                            LocalDate.of(2026, 8, 1)))
                    .build();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(event);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deadlines").isArray())
                    .andExpect(jsonPath("$.deadlines[0]").value("2026-07-01"))
                    .andExpect(jsonPath("$.deadlines[1]").value("2026-07-15"))
                    .andExpect(jsonPath("$.deadlines[2]").value("2026-08-01"));
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

        @Test
        @DisplayName("event detail includes ranking shortName and name when event has ranking")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldIncludeRankingInEventDetail() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event event = EventTestDataBuilder.anEvent()
                    .withRanking(new EventRanking(3, "A", "Národní závod"))
                    .build();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(event);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ranking.shortName").value("A"))
                    .andExpect(jsonPath("$.ranking.name").value("Národní závod"));
        }

        @Test
        @DisplayName("event detail includes baseEntryFee amount and currency when event has fee")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldIncludeBaseEntryFeeInEventDetail() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event event = EventTestDataBuilder.anEvent()
                    .withBaseEntryFee(Money.ofCzk(new java.math.BigDecimal("250")))
                    .build();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(event);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.baseEntryFee.amount").value(250))
                    .andExpect(jsonPath("$.baseEntryFee.currency").value("CZK"));
        }

        @Test
        @DisplayName("event detail omits ranking and baseEntryFee when they are null")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldOmitRankingAndFeeWhenNull() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event event = EventTestDataBuilder.anEvent().build();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(event);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ranking").doesNotExist())
                    .andExpect(jsonPath("$.baseEntryFee").doesNotExist());
        }

        @Test
        @DisplayName("updateEvent HAL-Forms template should have deadlines property with multi=true, max=3, type=date (deadlines are optional)")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void updateEventTemplateShouldHaveDeadlinesPropertyMetadata() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event draftEvent = EventTestDataBuilder.anEvent().build();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(draftEvent);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.updateEvent.properties[?(@.name=='deadlines')]").exists())
                    .andExpect(jsonPath("$._templates.updateEvent.properties[?(@.name=='deadlines')].type").value("date"))
                    .andExpect(jsonPath("$._templates.updateEvent.properties[?(@.name=='deadlines')].multi").value(true))
                    .andExpect(jsonPath("$._templates.updateEvent.properties[?(@.name=='deadlines')].max").value(3));
        }

        @Test
        @DisplayName("update affordance present in DRAFT event detail for event coordinator without EVENTS:MANAGE")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000042", authorities = {Authority.EVENTS_READ})
        void updateAffordancePresentForCoordinator() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId coordinatorId = new MemberId(UUID.fromString("00000000-0000-0000-0000-000000000042"));
            Event draftEvent = EventTestDataBuilder.anEvent()
                    .withCoordinator(coordinatorId)
                    .build();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(draftEvent);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.updateEvent.method").value("PATCH"));
        }

        @Test
        @DisplayName("update affordance absent in DRAFT event detail for unrelated member without EVENTS:MANAGE")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000099", authorities = {Authority.EVENTS_READ})
        void updateAffordanceAbsentForNonCoordinator() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId coordinatorId = new MemberId(UUID.fromString("00000000-0000-0000-0000-000000000042"));
            Event draftEvent = EventTestDataBuilder.anEvent()
                    .withCoordinator(coordinatorId)
                    .build();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(draftEvent);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.updateEvent").doesNotExist());
        }

        @Test
        @DisplayName("registrationTime visible for the second coordinator in the coordinators collection")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000002", authorities = {Authority.EVENTS_READ})
        void registrationTimeVisibleForSecondCoordinator() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId firstCoordinator = new MemberId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
            MemberId secondCoordinator = new MemberId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
            MemberId registeredMember = new MemberId(UUID.randomUUID());
            LinkedHashSet<MemberId> coordinators = new LinkedHashSet<>(List.of(firstCoordinator, secondCoordinator));
            Event activeEvent = EventTestDataBuilder.anEvent()
                    .withCoordinators(coordinators)
                    .withDate(LocalDate.now().plusDays(30))
                    .buildPublished();

            EventRegistration registration = EventRegistration.create(
                    EventRegistrationCreateEventRegistrationBuilder.builder()
                            .memberId(registeredMember)
                            .siCardNumber(new SiCardNumber("99001"))
                            .build());

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(activeEvent);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of(registration));
            when(members.findByIds(any())).thenReturn(Map.of(registeredMember, new MemberDto(registeredMember.value(), "Jan", "Novak", null)));

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0].registrationTime").exists());
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
        @DisplayName("should return 204 No Content without body")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldCancelEvent() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(
                            post("/api/events/{id}/cancel", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should accept cancellation reason and pass it to service")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldCancelEventWithReason() throws Exception {
            UUID eventId = UUID.randomUUID();
            String reason = "Bad weather forecast";

            mockMvc.perform(
                            post("/api/events/{id}/cancel", eventId)
                                    .contentType("application/json")
                                    .content("{\"cancellationReason\":\"" + reason + "\"}")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());

            verify(eventManagementService).cancelEvent(
                    eq(new EventId(eventId)),
                    argThat((Event.CancelEvent cmd) -> reason.equals(cmd.cancellationReason()))
            );
        }

        @Test
        @DisplayName("should reject cancellation reason exceeding 500 characters")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldRejectTooLongCancellationReason() throws Exception {
            UUID eventId = UUID.randomUUID();
            String tooLong = "x".repeat(501);

            mockMvc.perform(
                            post("/api/events/{id}/cancel", eventId)
                                    .contentType("application/json")
                                    .content("{\"cancellationReason\":\"" + tooLong + "\"}")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("event detail exposes cancellationReason when event is cancelled with reason")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldExposeReasonInEventDetail() throws Exception {
            Event event = EventTestDataBuilder.anEvent()
                    .withName("Cancelled Event")
                    .build();
            event.cancel(new Event.CancelEvent("Organizer is ill"));

            when(eventManagementService.getEvent(any(EventId.class), anyBoolean())).thenReturn(event);
            when(eventRegistrationService.listRegistrations(any(EventId.class))).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/events/{id}", event.getId().value())
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cancellationReason").value("Organizer is ill"));
        }

        @Test
        @DisplayName("cancelEvent HAL-Forms template should expose cancellationReason as textarea with maxLength 500")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void cancelEventTemplateShouldHaveCancellationReasonAsTextareaWithMaxLength() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event draftEvent = EventTestDataBuilder.anEvent().build();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(draftEvent);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.cancelEvent.properties[?(@.name=='cancellationReason')]").exists())
                    .andExpect(jsonPath("$._templates.cancelEvent.properties[?(@.name=='cancellationReason')].type").value("textarea"))
                    .andExpect(jsonPath("$._templates.cancelEvent.properties[?(@.name=='cancellationReason')].max").value(500));
        }
    }

    @Nested
    @DisplayName("GET /api/events — list item extended fields and links (tasks 3.6, 3.7)")
    class ListEventsExtendedFieldsTests {

        @Test
        @DisplayName("list item includes websiteUrl and deadlines array")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldIncludeWebsiteUrlAndDeadlinesInListItem() throws Exception {
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
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0].deadlines[0]").value("2026-08-10"));
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
        @DisplayName("list item includes coordinators array with all coordinator UUIDs")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.MEMBERS_READ})
        void shouldIncludeCoordinatorsArrayInListItem() throws Exception {
            MemberId coordA = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
            MemberId coordB = new MemberId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
            Event event = EventTestDataBuilder.anEvent()
                    .withCoordinators(new LinkedHashSet<>(List.of(coordA, coordB)))
                    .buildPublished();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1));

            mockMvc.perform(
                            get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0].coordinators",
                            org.hamcrest.Matchers.containsInAnyOrder(
                                    coordA.value().toString(),
                                    coordB.value().toString()
                            )));
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
        @DisplayName("list item includes newRegistration link targeting ?newRegistration=true when member not registered")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000099",
                authorities = {Authority.EVENTS_READ})
        void shouldIncludeNewRegistrationLinkOnListItemForUnregisteredMember() throws Exception {
            Event event = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .buildPublished();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1));

            mockMvc.perform(
                            get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0]._links.newRegistration.href")
                            .value(containsString("newRegistration=true")));
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

        @Test
        @DisplayName("dateFrom param is passed to service as EventFilter.dateFrom")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void dateFromParamIsPassedToFilter() throws Exception {
            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(
                            get("/api/events")
                                    .param("dateFrom", "2026-06-01")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk());

            verify(eventManagementService).listEvents(
                    eq(EventFilter.none().withDateRange(LocalDate.of(2026, 6, 1), null)), any(), anyBoolean());
        }

        @Test
        @DisplayName("dateTo param is passed to service as EventFilter.dateTo")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void dateToParamIsPassedToFilter() throws Exception {
            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(
                            get("/api/events")
                                    .param("dateTo", "2026-08-31")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk());

            verify(eventManagementService).listEvents(
                    eq(EventFilter.none().withDateRange(null, LocalDate.of(2026, 8, 31))), any(), anyBoolean());
        }

        @Test
        @DisplayName("dateFrom and dateTo params together are passed as EventFilter date range")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void dateFromAndDateToParamsArePassedToFilter() throws Exception {
            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(
                            get("/api/events")
                                    .param("dateFrom", "2026-06-01")
                                    .param("dateTo", "2026-08-31")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk());

            verify(eventManagementService).listEvents(
                    eq(EventFilter.none().withDateRange(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31))),
                    any(), anyBoolean());
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

    @Nested
    @DisplayName("GET /api/events/{eventId}/accommodation-list (N11)")
    class AccommodationListTests {

        private static final String COORDINATOR_ID = "cccccccc-cccc-cccc-cccc-cccccccccccc";
        private static final String REGULAR_MEMBER_ID = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee";

        @Test
        @DisplayName("event coordinator gets 200 with accommodation items")
        @WithKlabisMockUser(memberId = COORDINATOR_ID)
        void coordinatorGets200WithAccommodationItems() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.randomUUID());
            MemberId coordinatorId = new MemberId(UUID.fromString(COORDINATOR_ID));

            List<EventRegistration> registrations = List.of(
                    EventRegistration.reconstruct(UUID.randomUUID(), memberId, SiCardNumber.of("1234"), null, Instant.now())
            );
            Event event = EventTestDataBuilder.anEvent()
                    .withCoordinator(coordinatorId)
                    .addRegistrations(registrations)
                    .build();
            event.publish();

            MemberAccommodationDto accommodationDto = new MemberAccommodationDto(
                    "John", "Doe", "AB123456", java.time.LocalDate.of(2028, 1, 1),
                    java.time.LocalDate.of(1985, 5, 15), "Main St 1", "Prague", "11000", "CZ");

            when(eventManagementService.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(members.findAccommodationDataByIds(any())).thenReturn(Map.of(memberId, accommodationDto));

            mockMvc.perform(
                            get("/api/events/{eventId}/accommodation-list", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.accommodationList[0].firstName").value("John"))
                    .andExpect(jsonPath("$._embedded.accommodationList[0].lastName").value("Doe"))
                    .andExpect(jsonPath("$._embedded.accommodationList[0].identityCardNumber").value("AB123456"))
                    .andExpect(jsonPath("$._embedded.accommodationList[0].dateOfBirth").value("1985-05-15"));
        }

        @Test
        @DisplayName("user with EVENTS:REGISTRATIONS authority gets 200")
        @WithKlabisMockUser(memberId = REGULAR_MEMBER_ID, authorities = {Authority.EVENTS_REGISTRATIONS})
        void eventsRegistrationsAuthorityGets200() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.randomUUID());
            MemberId coordinatorId = new MemberId(UUID.fromString(COORDINATOR_ID));

            List<EventRegistration> registrations = List.of(
                    EventRegistration.reconstruct(UUID.randomUUID(), memberId, SiCardNumber.of("1234"), null, Instant.now())
            );
            Event event = EventTestDataBuilder.anEvent()
                    .withCoordinator(coordinatorId)
                    .addRegistrations(registrations)
                    .build();
            event.publish();

            MemberAccommodationDto accommodationDto = new MemberAccommodationDto(
                    "Jane", "Smith", null, null,
                    java.time.LocalDate.of(1990, 3, 10), "Oak Ave 5", "Brno", "60200", "CZ");

            when(eventManagementService.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(members.findAccommodationDataByIds(any())).thenReturn(Map.of(memberId, accommodationDto));

            mockMvc.perform(
                            get("/api/events/{eventId}/accommodation-list", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.accommodationList[0].firstName").value("Jane"));
        }

        @Test
        @DisplayName("unauthorized member gets 403")
        @WithKlabisMockUser(memberId = REGULAR_MEMBER_ID)
        void unauthorizedMemberGets403() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId coordinatorId = new MemberId(UUID.fromString(COORDINATOR_ID));

            Event event = EventTestDataBuilder.anEvent()
                    .withCoordinator(coordinatorId)
                    .build();
            event.publish();

            when(eventManagementService.getEvent(new EventId(eventId), false)).thenReturn(event);

            mockMvc.perform(
                            get("/api/events/{eventId}/accommodation-list", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("member with null identityCard has null identityCard fields in response")
        @WithKlabisMockUser(memberId = REGULAR_MEMBER_ID, authorities = {Authority.EVENTS_REGISTRATIONS})
        void memberWithNullIdentityCardHasNullFieldsInResponse() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.randomUUID());

            List<EventRegistration> registrations = List.of(
                    EventRegistration.reconstruct(UUID.randomUUID(), memberId, SiCardNumber.of("1234"), null, Instant.now())
            );
            Event event = EventTestDataBuilder.anEvent()
                    .addRegistrations(registrations)
                    .build();
            event.publish();

            MemberAccommodationDto accommodationDto = new MemberAccommodationDto(
                    "Alice", "Brown", null, null,
                    java.time.LocalDate.of(1992, 7, 20), "Park Rd 3", "Ostrava", "70200", "CZ");

            when(eventManagementService.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(members.findAccommodationDataByIds(any())).thenReturn(Map.of(memberId, accommodationDto));

            mockMvc.perform(
                            get("/api/events/{eventId}/accommodation-list", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.accommodationList[0].identityCardNumber").doesNotExist())
                    .andExpect(jsonPath("$._embedded.accommodationList[0].identityCardValidityDate").doesNotExist())
                    .andExpect(jsonPath("$._embedded.accommodationList[0].firstName").value("Alice"));
        }

        @Test
        @DisplayName("member with null address has null address fields in response")
        @WithKlabisMockUser(memberId = REGULAR_MEMBER_ID, authorities = {Authority.EVENTS_REGISTRATIONS})
        void memberWithNullAddressHasNullAddressFieldsInResponse() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.randomUUID());

            List<EventRegistration> registrations = List.of(
                    EventRegistration.reconstruct(UUID.randomUUID(), memberId, SiCardNumber.of("1234"), null, Instant.now())
            );
            Event event = EventTestDataBuilder.anEvent()
                    .addRegistrations(registrations)
                    .build();
            event.publish();

            MemberAccommodationDto accommodationDto = new MemberAccommodationDto(
                    "Bob", "White", "XY999888", java.time.LocalDate.of(2026, 12, 31),
                    java.time.LocalDate.of(1995, 11, 5), null, null, null, null);

            when(eventManagementService.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(members.findAccommodationDataByIds(any())).thenReturn(Map.of(memberId, accommodationDto));

            mockMvc.perform(
                            get("/api/events/{eventId}/accommodation-list", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.accommodationList[0].addressStreet").doesNotExist())
                    .andExpect(jsonPath("$._embedded.accommodationList[0].firstName").value("Bob"));
        }
    }

    @Nested
    @DisplayName("GET /api/events/{eventId}/accommodation-list — CSV content negotiation (iteration 2)")
    class AccommodationListCsvTests {

        private static final String COORDINATOR_ID = "cccccccc-cccc-cccc-cccc-cccccccccccc";
        private static final String REGULAR_MEMBER_ID = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee";

        @Test
        @DisplayName("2.1: Accept: text/csv returns 200, content type text/csv, Content-Disposition attachment with event slug filename, and CSV body")
        @WithKlabisMockUser(memberId = COORDINATOR_ID, authorities = {Authority.EVENTS_REGISTRATIONS})
        void csvAcceptHeaderReturnsCsvWithContentDispositionAndBody() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.randomUUID());

            List<EventRegistration> registrations = List.of(
                    EventRegistration.reconstruct(UUID.randomUUID(), memberId, SiCardNumber.of("9876"), null, Instant.now())
            );
            Event event = EventTestDataBuilder.anEvent()
                    .withName("Zimní soustředění 2026")
                    .addRegistrations(registrations)
                    .build();
            event.publish();

            MemberAccommodationDto accommodationDto = new MemberAccommodationDto(
                    "Jan", "Novák", "AB123456", java.time.LocalDate.of(2028, 1, 1),
                    java.time.LocalDate.of(1990, 5, 10), "Hlavní 1", "Praha", "11000", "CZ");

            byte[] csvBytes = "Jan;Novák\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            when(eventManagementService.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(members.findAccommodationDataByIds(any())).thenReturn(Map.of(memberId, accommodationDto));
            when(csvRenderer.renderToBytes(any())).thenReturn(csvBytes);

            mockMvc.perform(
                            get("/api/events/{eventId}/accommodation-list", eventId)
                                    .accept("text/csv")
                    )
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("text/csv")))
                    .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                    .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("ubytovani-zimni-soustredeni-2026.csv")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Jan")));
        }

        @Test
        @DisplayName("2.2: Accept: application/prs.hal-forms+json still returns existing HAL collection unchanged (regression guard)")
        @WithKlabisMockUser(memberId = COORDINATOR_ID, authorities = {Authority.EVENTS_REGISTRATIONS})
        void halAcceptHeaderReturnsHalCollectionUnchanged() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.randomUUID());

            List<EventRegistration> registrations = List.of(
                    EventRegistration.reconstruct(UUID.randomUUID(), memberId, SiCardNumber.of("5555"), null, Instant.now())
            );
            Event event = EventTestDataBuilder.anEvent()
                    .withName("Test Event")
                    .addRegistrations(registrations)
                    .build();
            event.publish();

            MemberAccommodationDto accommodationDto = new MemberAccommodationDto(
                    "Jane", "Smith", null, null,
                    java.time.LocalDate.of(1985, 3, 20), "Oak Ave", "Brno", "60200", "CZ");

            when(eventManagementService.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(members.findAccommodationDataByIds(any())).thenReturn(Map.of(memberId, accommodationDto));

            mockMvc.perform(
                            get("/api/events/{eventId}/accommodation-list", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("application/prs.hal-forms+json")))
                    .andExpect(jsonPath("$._embedded.accommodationList[0].firstName").value("Jane"))
                    .andExpect(jsonPath("$._embedded.accommodationList[0].lastName").value("Smith"));
        }

        @Test
        @DisplayName("2.5: unauthorized user gets 403 for text/csv request — existing authorization check covers CSV path")
        @WithKlabisMockUser(memberId = REGULAR_MEMBER_ID)
        void unauthorizedUserGets403ForCsvRequest() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId coordinatorId = new MemberId(UUID.fromString(COORDINATOR_ID));

            Event event = EventTestDataBuilder.anEvent()
                    .withCoordinator(coordinatorId)
                    .build();
            event.publish();

            when(eventManagementService.getEvent(new EventId(eventId), false)).thenReturn(event);

            mockMvc.perform(
                            get("/api/events/{eventId}/accommodation-list", eventId)
                                    .accept("text/csv")
                    )
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/events/{id} — blocked member registration affordance")
    class BlockedMemberEventDetailAffordanceTests {

        @Test
        @DisplayName("blocked member does not receive registerForEvent affordance on event detail")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000099", authorities = {Authority.EVENTS_READ})
        void blockedMemberDoesNotReceiveRegisterForEventAffordance() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.fromString("00000000-0000-0000-0000-000000000099"));
            Event activeEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .buildPublished();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(activeEvent);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());
            when(memberRegistrationSanctionPort.isMemberBlocked(memberId)).thenReturn(true);

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.registerForEvent").doesNotExist())
                    .andExpect(jsonPath("$._links.newRegistration").doesNotExist());
        }

        @Test
        @DisplayName("non-blocked member receives registerForEvent affordance on event detail")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000099", authorities = {Authority.EVENTS_READ})
        void nonBlockedMemberReceivesRegisterForEventAffordance() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.fromString("00000000-0000-0000-0000-000000000099"));
            Event activeEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .buildPublished();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(activeEvent);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());
            when(memberRegistrationSanctionPort.isMemberBlocked(memberId)).thenReturn(false);

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.registerForEvent.method").value("POST"));
        }

        @Test
        @DisplayName("user without member profile receives registerForEvent affordance on ACTIVE event with open registration")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void userWithoutMemberProfileReceivesRegisterForEventAffordance() throws Exception {
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
                    .andExpect(jsonPath("$._templates.registerForEvent.method").value("POST"));

            // currentMemberId == null path must not call sanctionPort — no member to check
            verify(memberRegistrationSanctionPort, never()).isMemberBlocked(any());
        }
    }

    @Nested
    @DisplayName("GET /api/events — blocked member registration affordance on list items")
    class BlockedMemberEventListAffordanceTests {

        @Test
        @DisplayName("blocked member does not receive registerForEvent affordance on list items")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000099", authorities = {Authority.EVENTS_READ})
        void blockedMemberDoesNotReceiveRegisterForEventAffordanceOnListItems() throws Exception {
            MemberId memberId = new MemberId(UUID.fromString("00000000-0000-0000-0000-000000000099"));
            Event activeEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .buildPublished();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(activeEvent), PageRequest.of(0, 10), 1));
            when(memberRegistrationSanctionPort.isMemberBlocked(memberId)).thenReturn(true);

            mockMvc.perform(
                            get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0]._templates.registerForEvent").doesNotExist())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0]._links.newRegistration").doesNotExist());
        }

        @Test
        @DisplayName("non-blocked member receives registerForEvent affordance on list items")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000099", authorities = {Authority.EVENTS_READ})
        void nonBlockedMemberReceivesRegisterForEventAffordanceOnListItems() throws Exception {
            MemberId memberId = new MemberId(UUID.fromString("00000000-0000-0000-0000-000000000099"));
            Event activeEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .buildPublished();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(activeEvent), PageRequest.of(0, 10), 1));
            when(memberRegistrationSanctionPort.isMemberBlocked(memberId)).thenReturn(false);

            mockMvc.perform(
                            get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0]._templates.registerForEvent.method").value("POST"));
        }

        @Test
        @DisplayName("all events in list receive registerForEvent affordance when member is not blocked")
        @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "00000000-0000-0000-0000-000000000099", authorities = {Authority.EVENTS_READ})
        void allEventsReceiveRegisterForEventAffordanceWhenMemberIsNotBlocked() throws Exception {
            MemberId memberId = new MemberId(UUID.fromString("00000000-0000-0000-0000-000000000099"));
            Event event1 = EventTestDataBuilder.anEvent().withDate(LocalDate.now().plusDays(30)).buildPublished();
            Event event2 = EventTestDataBuilder.anEvent().withDate(LocalDate.now().plusDays(60)).buildPublished();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(event1, event2), PageRequest.of(0, 10), 2));
            when(memberRegistrationSanctionPort.isMemberBlocked(memberId)).thenReturn(false);

            mockMvc.perform(
                            get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0]._templates.registerForEvent.method").value("POST"))
                    .andExpect(jsonPath("$._embedded.eventSummaryDtoList[1]._templates.registerForEvent.method").value("POST"));
        }
    }

}
