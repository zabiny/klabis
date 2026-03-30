package com.klabis.events.infrastructure.restapi;

import com.klabis.common.HateoasTestingSupport;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.security.SecurityConfiguration;
import com.klabis.common.users.UserService;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Event Registration Controller API Tests")
@WebMvcTest(controllers = {EventRegistrationController.class})
@Import({EncryptionConfiguration.class, SecurityConfiguration.class})
class EventRegistrationControllerTest {

    private static final String MEMBER_1_ID = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsServiceMock;

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
                    EventRegistration.reconstruct(UUID.randomUUID(), member1Id, SiCardNumber.of("1234"), Instant.now()),
                    EventRegistration.reconstruct(UUID.randomUUID(), member2Id, SiCardNumber.of("5678"), Instant.now())
            );

            when(registrationServiceMock.listRegistrations(new EventId(eventId))).thenReturn(registrations);
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
    }

    @Nested
    @DisplayName("GET /api/events/{id}/registrations/me")
    class GetOwnRegistrationTests {

        @Test
        @DisplayName("should return full registration with SI card and DELETE affordance when event is ACTIVE")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldReturnOwnRegistrationWithSiCard() throws Exception {
            final UUID eventId = UUID.randomUUID();
            final MemberId memberId = new MemberId(UUID.fromString(MEMBER_1_ID));

            EventRegistration registration = EventRegistration.reconstruct(
                    UUID.randomUUID(), memberId, SiCardNumber.of("123456"), Instant.now());
            Event activeEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .addRegistration(registration)
                    .build();
            activeEvent.publish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), true)).thenReturn(activeEvent);
            when(membersMock.findById(memberId)).thenReturn(java.util.Optional.of(new MemberDto(memberId.value(), "John", "Doe", "john@example.com")));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations/me", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.siCardNumber").value("123456"))
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.event.href").exists())
                    .andExpect(jsonPath("$._templates.unregisterFromEvent.method").value("DELETE")); // UNREGISTER
        }

        @Test
        @DisplayName("should return registration without DELETE affordance when event is not ACTIVE")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldReturnOwnRegistrationWithoutDeleteAffordanceWhenEventNotActive() throws Exception {
            final UUID eventId = UUID.randomUUID();
            final MemberId memberId = new MemberId(UUID.fromString(MEMBER_1_ID));

            EventRegistration registration = EventRegistration.reconstruct(
                    UUID.randomUUID(), memberId, SiCardNumber.of("123456"), Instant.now());
            Event finishedEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().minusDays(5))
                    .addRegistration(registration)
                    .build();
            finishedEvent.publish();
            finishedEvent.finish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), true)).thenReturn(finishedEvent);
            when(membersMock.findById(memberId)).thenReturn(java.util.Optional.of(new MemberDto(memberId.value(), "John", "Doe", "john@example.com")));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations/me", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.event.href").exists())
                    .andExpect(jsonPath("$._templates.unregisterFromEvent.method").doesNotExist()); // no DELETE
        }

        @Test
        @DisplayName("should return 404 when not registered")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldReturn404WhenNotRegistered() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event eventWithoutRegistration = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .build();
            eventWithoutRegistration.publish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), true)).thenReturn(eventWithoutRegistration);

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations/me", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"));
        }

        @Test
        @DisplayName("should return 401 for unauthenticated users")
        void shouldReturn401ForUnauthenticatedUserOnGetOwn() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations/me", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isUnauthorized());
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

}
