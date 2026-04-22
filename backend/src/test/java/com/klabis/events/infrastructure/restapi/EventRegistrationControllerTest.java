package com.klabis.events.infrastructure.restapi;

import com.klabis.common.HateoasTestingSupport;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.encryption.EncryptionConfiguration;
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
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Event Registration Controller API Tests")
@WebMvcTest(controllers = {EventRegistrationController.class, EventsExceptionHandler.class})
@Import(EncryptionConfiguration.class)
@WithPostprocessors
class EventRegistrationControllerTest {

    private static final String MEMBER_1_ID = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestBean
    private EntityLinks entityLinksMock;

    @MockitoBean
    private EventManagementPort eventManagementServiceMock;

    @MockitoBean
    private EventRegistrationPort registrationServiceMock;

    @MockitoBean
    private Members membersMock;

    static EntityLinks entityLinksMock() {
        return HateoasTestingSupport.createModuleEntityLinks(EventRegistrationController.class);
    }

    @Nested
    @DisplayName("POST /api/events/{id}/registrations")
    class RegisterForEventTests {

        @Test
        @DisplayName("should return 201 Created with Location header and no body")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldRegisterMemberForEvent() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event.RegisterCommand command = EventRegisterCommandBuilder.builder().siCardNumber("123456").build();

            mockMvc.perform(
                            post("/api/events/{eventId}/registrations", eventId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(command))
                    )
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("2.3 Location header after registration points to /{memberId} not /me")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldReturnLocationHeaderWithMemberIdNotMe() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event.RegisterCommand command = EventRegisterCommandBuilder.builder().siCardNumber("123456").build();

            mockMvc.perform(
                            post("/api/events/{eventId}/registrations", eventId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(command))
                    )
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString(MEMBER_1_ID)))
                    .andExpect(header().string("Location", not(containsString("/me"))));
        }

        @Test
        @DisplayName("should return 409 Conflict for duplicate registration")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldReturn409ForDuplicateRegistration() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event.RegisterCommand command = EventRegisterCommandBuilder.builder().siCardNumber("123456").build();

            doThrow(new DuplicateRegistrationException(new MemberId(UUID.fromString(MEMBER_1_ID)), new EventId(eventId)))
                    .when(registrationServiceMock)
                    .registerMember(eq(new EventId(eventId)), any(MemberId.class), any(Event.RegisterCommand.class));

            mockMvc.perform(
                            post("/api/events/{eventId}/registrations", eventId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(command))
                    )
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Registration Conflict"));
        }

        @Test
        @DisplayName("should return 401 for unauthenticated users")
        void shouldReturn401ForUnauthenticatedUser() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event.RegisterCommand command = EventRegisterCommandBuilder.builder().siCardNumber("123456").build();

            mockMvc.perform(
                            post("/api/events/{eventId}/registrations", eventId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(command))
                    )
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 403 Forbidden when user has no member profile")
        @WithKlabisMockUser
        void shouldReturn403WhenUserHasNoMemberProfile() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event.RegisterCommand command = EventRegisterCommandBuilder.builder().siCardNumber("123456").build();

            mockMvc.perform(
                            post("/api/events/{eventId}/registrations", eventId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(command))
                    )
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.title").value("Authorization Failed"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/events/{id}/registrations")
    class UnregisterFromEventTests {

        @Test
        @DisplayName("should return 204 No Content when service doesn't throw an exception")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldUnregisterMemberFromEvent() throws Exception {
            UUID eventId = UUID.randomUUID();
            final MemberId memberId = new MemberId(UUID.fromString(MEMBER_1_ID));

            mockMvc.perform(delete("/api/events/{eventId}/registrations", eventId))
                    .andExpect(status().isNoContent());

            verify(registrationServiceMock).unregisterMember(new EventId(eventId), memberId);
        }

        @Test
        @DisplayName("should return 404 Not Found when service throws EventNotFoundException")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldRespond404WhenEventNotFoundException() throws Exception {
            UUID eventId = UUID.randomUUID();

            doThrow(new EventNotFoundException(new EventId(eventId))).when(registrationServiceMock)
                    .unregisterMember(any(), any());

            mockMvc.perform(delete("/api/events/{eventId}/registrations", eventId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 401 for unauthenticated users")
        void shouldReturn401ForUnauthenticatedUserOnDelete() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(
                            delete("/api/events/{eventId}/registrations", eventId)
                    )
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 403 Forbidden when user has no member profile")
        @WithKlabisMockUser
        void shouldReturn403WhenUserHasNoMemberProfile() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(
                            delete("/api/events/{eventId}/registrations", eventId)
                    )
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.title").value("Authorization Failed"));
        }
    }

    @Nested
    @DisplayName("GET /api/events/{id}/registrations")
    class ListRegistrationsTests {

        @Test
        @DisplayName("should return list without SI card numbers")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldListRegistrationsWithoutSiCardNumbers() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId member1Id = new MemberId(UUID.randomUUID());
            MemberId member2Id = new MemberId(UUID.randomUUID());
            List<EventRegistration> registrations = List.of(
                    EventRegistration.reconstruct(UUID.randomUUID(), member1Id, SiCardNumber.of("1234"), null, Instant.now()),
                    EventRegistration.reconstruct(UUID.randomUUID(), member2Id, SiCardNumber.of("5678"), null, Instant.now())
            );
            Event closedEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().minusDays(5))
                    .addRegistrations(registrations)
                    .build();
            closedEvent.publish();
            closedEvent.finish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(closedEvent);
            when(membersMock.findByIds(any())).thenReturn(Map.of(
                    member1Id, new MemberDto(member1Id.value(), "John", "Doe", "john@example.com"),
                    member2Id, new MemberDto(member2Id.value(), "Jane", "Smith", "jane@example.com")
            ));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.registrationDtoList").isArray())
                    .andExpect(jsonPath("$._embedded.registrationDtoList.length()").value(registrations.size()))
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0].firstName").value("John"))
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0].siCardNumber").doesNotExist())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[1].siCardNumber").doesNotExist());
        }

        @Test
        @DisplayName("should include category in registration list when registration has category")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldIncludeCategoryInRegistrationList() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.randomUUID());
            List<EventRegistration> registrations = List.of(
                    EventRegistration.reconstruct(UUID.randomUUID(), memberId, SiCardNumber.of("1234"), "M21", Instant.now())
            );
            Event closedEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().minusDays(5))
                    .addRegistrations(registrations)
                    .build();
            closedEvent.publish();
            closedEvent.finish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(closedEvent);
            when(membersMock.findByIds(any())).thenReturn(Map.of(
                    memberId, new MemberDto(memberId.value(), "John", "Doe", "john@example.com")
            ));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0].category").value("M21"));
        }
    }

    @Nested
    @DisplayName("Registration Deadline Enforcement (task 4.3)")
    class RegistrationDeadlineEnforcementTests {

        @Test
        @DisplayName("POST /registrations returns 400 when service throws BusinessRuleViolationException for expired deadline")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldReturn400WhenRegistrationDeadlinePassed() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event.RegisterCommand command = EventRegisterCommandBuilder.builder().siCardNumber("123456").build();

            doThrow(new com.klabis.common.exceptions.BusinessRuleViolationException("Registration deadline has passed") {})
                    .when(registrationServiceMock)
                    .registerMember(any(), any(), any());

            mockMvc.perform(
                            post("/api/events/{eventId}/registrations", eventId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(command))
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("DELETE /registrations returns 400 when service throws BusinessRuleViolationException for expired deadline")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldReturn400WhenUnregistrationDeadlinePassed() throws Exception {
            UUID eventId = UUID.randomUUID();

            doThrow(new com.klabis.common.exceptions.BusinessRuleViolationException("Registration deadline has passed") {})
                    .when(registrationServiceMock)
                    .unregisterMember(any(), any());

            mockMvc.perform(
                            delete("/api/events/{eventId}/registrations", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/events/{eventId}/registrations/{memberId}")
    class EditRegistrationTests {

        @Test
        @DisplayName("4.1 acting user == memberId updates registration and returns 204")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldEditRegistrationAndReturn204WhenActingUserIsOwner() throws Exception {
            UUID eventId = UUID.randomUUID();
            String body = """
                    {"siCardNumber":"123456","category":null}
                    """;

            mockMvc.perform(
                            put("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(body)
                    )
                    .andExpect(status().isNoContent());

            verify(registrationServiceMock).editRegistration(eq(new EventId(eventId)), eq(new MemberId(UUID.fromString(MEMBER_1_ID))), any(Event.EditRegistrationCommand.class));
        }

        @Test
        @DisplayName("4.2 PUT by different member returns 403")
        @WithKlabisMockUser(memberId = "22222222-2222-2222-2222-222222222222")
        void shouldReturn403WhenActingUserIsNotOwner() throws Exception {
            UUID eventId = UUID.randomUUID();
            String body = """
                    {"siCardNumber":"123456","category":null}
                    """;

            mockMvc.perform(
                            put("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(body)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("4.3 PUT with invalid SI card number returns 400 with field feedback")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldReturn400WithFieldFeedbackForInvalidSiCardNumber() throws Exception {
            UUID eventId = UUID.randomUUID();
            String body = """
                    {"siCardNumber":"abc","category":null}
                    """;

            mockMvc.perform(
                            put("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(body)
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("4.4 PUT with category not in event category list returns 400")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldReturn400WhenCategoryIsNotInEventList() throws Exception {
            UUID eventId = UUID.randomUUID();
            String body = """
                    {"siCardNumber":"123456","category":"INVALID_CATEGORY"}
                    """;

            doThrow(new com.klabis.common.exceptions.BusinessRuleViolationException("Category 'INVALID_CATEGORY' is not available for this event") {})
                    .when(registrationServiceMock)
                    .editRegistration(any(), any(), any());

            mockMvc.perform(
                            put("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(body)
                    )
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/events/{id}/registrations — edit affordance on acting member row")
    class ListRegistrationsEditAffordanceTests {

        @Test
        @DisplayName("4.8 edit affordance present on acting member row and absent on other rows")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldExposeEditAffordanceOnlyOnActingMemberRow() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId member1Id = new MemberId(UUID.fromString(MEMBER_1_ID));
            MemberId member2Id = new MemberId(UUID.randomUUID());

            List<EventRegistration> registrations = List.of(
                    EventRegistration.reconstruct(UUID.randomUUID(), member1Id, SiCardNumber.of("1234"), null, Instant.now()),
                    EventRegistration.reconstruct(UUID.randomUUID(), member2Id, SiCardNumber.of("5678"), null, Instant.now())
            );

            Event activeEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .addRegistrations(registrations)
                    .build();
            activeEvent.publish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(activeEvent);
            when(membersMock.findByIds(any())).thenReturn(Map.of(
                    member1Id, new MemberDto(member1Id.value(), "John", "Doe", "john@example.com"),
                    member2Id, new MemberDto(member2Id.value(), "Jane", "Smith", "jane@example.com")
            ));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0]._links.self.href").exists())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0]._templates.editRegistration.method").value("PUT"))
                    .andExpect(jsonPath("$._embedded.registrationDtoList[1]._templates").doesNotExist());
        }

        @Test
        @DisplayName("edit affordance absent on all rows when registrations are closed")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldNotExposeEditAffordanceWhenRegistrationsClosed() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId member1Id = new MemberId(UUID.fromString(MEMBER_1_ID));

            List<EventRegistration> registrations = List.of(
                    EventRegistration.reconstruct(UUID.randomUUID(), member1Id, SiCardNumber.of("1234"), null, Instant.now())
            );

            Event finishedEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().minusDays(5))
                    .addRegistrations(registrations)
                    .build();
            finishedEvent.publish();
            finishedEvent.finish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(finishedEvent);
            when(membersMock.findByIds(any())).thenReturn(Map.of(
                    member1Id, new MemberDto(member1Id.value(), "John", "Doe", "john@example.com")
            ));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0]._templates").doesNotExist());
        }
    }

    @Nested
    @DisplayName("GET /api/events/{eventId}/registrations/{memberId}")
    class GetRegistrationByMemberIdTests {

        @Test
        @DisplayName("1.1 owner gets 200 with SI card number and self link pointing to /{memberId}")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldReturn200WithSiCardForOwner() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.fromString(MEMBER_1_ID));

            EventRegistration registration = EventRegistration.reconstruct(
                    UUID.randomUUID(), memberId, SiCardNumber.of("123456"), null, Instant.now());
            Event activeEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .addRegistration(registration)
                    .build();
            activeEvent.publish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(activeEvent);
            when(membersMock.findById(memberId)).thenReturn(Optional.of(
                    new MemberDto(memberId.value(), "John", "Doe", "john@example.com")));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.siCardNumber").value("123456"))
                    .andExpect(jsonPath("$._links.self.href", containsString(MEMBER_1_ID)))
                    .andExpect(jsonPath("$._links.self.href", not(containsString("/me"))));
        }

        @Test
        @DisplayName("1.2 user with EVENTS:MANAGE gets 200 with SI card number (non-owner coordinator)")
        @WithKlabisMockUser(memberId = "22222222-2222-2222-2222-222222222222", authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn200WithSiCardForEventsManageAuthority() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId targetMemberId = new MemberId(UUID.fromString(MEMBER_1_ID));

            EventRegistration registration = EventRegistration.reconstruct(
                    UUID.randomUUID(), targetMemberId, SiCardNumber.of("654321"), null, Instant.now());
            Event activeEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .addRegistration(registration)
                    .build();
            activeEvent.publish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), true)).thenReturn(activeEvent);
            when(membersMock.findById(targetMemberId)).thenReturn(Optional.of(
                    new MemberDto(targetMemberId.value(), "Jane", "Smith", "jane@example.com")));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Jane"))
                    .andExpect(jsonPath("$.siCardNumber").value("654321"));
        }

        @Test
        @DisplayName("1.3 non-owner without EVENTS:MANAGE gets 403")
        @WithKlabisMockUser(memberId = "22222222-2222-2222-2222-222222222222")
        void shouldReturn403ForNonOwnerWithoutEventsManage() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("1.4 returns 404 when member is not registered for the event")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldReturn404WhenMemberNotRegistered() throws Exception {
            UUID eventId = UUID.randomUUID();

            Event eventWithoutRegistration = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .build();
            eventWithoutRegistration.publish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(eventWithoutRegistration);

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"));
        }

    }

}
