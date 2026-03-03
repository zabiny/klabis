package com.klabis.events.infrastructure.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.common.HateoasTestingSupport;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.security.SecurityConfiguration;
import com.klabis.common.users.UserService;
import com.klabis.events.application.DuplicateRegistrationException;
import com.klabis.events.application.EventNotFoundException;
import com.klabis.events.application.EventRegistrationService;
import com.klabis.events.application.RegistrationNotFoundException;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.SiCardNumber;
import com.klabis.events.EventId;
import com.klabis.members.MemberDto;
import com.klabis.members.Members;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
    private EventRegistrationService registrationServiceMock;

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
            Event.RegisterCommand command = new Event.RegisterCommand("123456");

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
            Event.RegisterCommand command = new Event.RegisterCommand("123456");

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
            Event.RegisterCommand command = new Event.RegisterCommand("123456");

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
            Event.RegisterCommand command = new Event.RegisterCommand("123456");

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

            verify(registrationServiceMock).unregisterMember(new EventId(eventId), memberId, LocalDate.now());
        }

        @Test
        @DisplayName("should return 404 Not Found when service throws EventNotFoundException")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldRespond404WhenEventNotFoundException() throws Exception {
            UUID eventId = UUID.randomUUID();

            doThrow(new EventNotFoundException(new EventId(eventId))).when(registrationServiceMock)
                    .unregisterMember(any(), any(), any());

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
            when(membersMock.findById(member1Id)).thenReturn(java.util.Optional.of(new MemberDto(member1Id.value(), "John", "Doe", "john@example.com")));
            when(membersMock.findById(member2Id)).thenReturn(java.util.Optional.of(new MemberDto(member2Id.value(), "Jane", "Smith", "jane@example.com")));

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
        @DisplayName("should return full registration with SI card")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldReturnOwnRegistrationWithSiCard() throws Exception {
            final UUID eventId = UUID.randomUUID();
            final MemberId memberId = new MemberId(UUID.fromString(MEMBER_1_ID));

            EventRegistration registration = EventRegistration.reconstruct(
                    UUID.randomUUID(), memberId, SiCardNumber.of("123456"), Instant.now());

            when(registrationServiceMock.getOwnRegistration(new EventId(eventId), memberId)).thenReturn(registration);
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
                    .andExpect(jsonPath("$._templates.default.method").value("DELETE")); // UNREGISTER
        }

        @Test
        @DisplayName("should return 404 when not registered")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldReturn404WhenNotRegistered() throws Exception {
            UUID eventId = UUID.randomUUID();

            when(registrationServiceMock.getOwnRegistration(eq(new EventId(eventId)), any(MemberId.class)))
                    .thenThrow(new RegistrationNotFoundException(new MemberId(UUID.fromString(MEMBER_1_ID)), new EventId(eventId)));

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

}
