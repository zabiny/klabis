package com.klabis.events.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.common.HateoasTestingSupport;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.security.SecurityConfiguration;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.LinkBuilder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
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
    private UserDetailsService userDetailsServiceMock;

    @TestBean
    private EntityLinks entityLinksMock;

    @MockitoBean
    private EventRegistrationService registrationServiceMock;

    static EntityLinks entityLinksMock() {
        return HateoasTestingSupport.createModuleEntityLinks(EventRegistrationController.class);
    }

    @Nested
    @DisplayName("POST /api/events/{id}/registrations")
    class RegisterForEventTests {

        @Test
        @DisplayName("should return 201 Created")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldRegisterMemberForEvent() throws Exception {
            UUID eventId = UUID.randomUUID();
            RegisterForEventCommand command = new RegisterForEventCommand("123456");

            OwnRegistrationDto registration = new OwnRegistrationDto(
                    "John",
                    "Doe",
                    "123456",
                    Instant.now()
            );

            when(registrationServiceMock.getOwnRegistration(eq(eventId), any(UserId.class))).thenReturn(registration);

            mockMvc.perform(
                            post("/api/events/{eventId}/registrations", eventId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(command))
                    )
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.siCardNumber").value("123456"))
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.event.href").exists())
                    .andExpect(jsonPath("$._templates.default.method").value("DELETE"));    // UNREGISTER
        }

        private LinkBuilder eventLinkBuilder(UUID eventId) {
            return new SimpleLinkBuilder("/api/events/" + eventId);
        }

        @Test
        @DisplayName("should return 409 Conflict for duplicate registration")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldReturn409ForDuplicateRegistration() throws Exception {
            UUID eventId = UUID.randomUUID();
            RegisterForEventCommand command = new RegisterForEventCommand("123456");

            doThrow(new DuplicateRegistrationException(UUID.fromString(MEMBER_1_ID), eventId))
                    .when(registrationServiceMock)
                    .registerMember(eq(eventId), any(MemberId.class), any(RegisterForEventCommand.class));

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
            RegisterForEventCommand command = new RegisterForEventCommand("123456");

            mockMvc.perform(
                            post("/api/events/{eventId}/registrations", eventId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(command))
                    )
                    .andExpect(status().isUnauthorized());
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
            final UserId userId = UserId.fromString(MEMBER_1_ID);

            mockMvc.perform(delete("/api/events/{eventId}/registrations", eventId))
                    .andExpect(status().isNoContent());

            verify(registrationServiceMock).unregisterMember(eventId, userId, LocalDate.now());
        }

        @Test
        @DisplayName("should return 404 Not Found when service throws EventNotFoundException")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldRespond404WhenEventNotFoundException() throws Exception {
            UUID eventId = UUID.randomUUID();

            doThrow(new EventNotFoundException(eventId)).when(registrationServiceMock)
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
            List<RegistrationDto> registrations = List.of(
                    new RegistrationDto("John", "Doe", Instant.now()),
                    new RegistrationDto("Jane", "Smith", Instant.now())
            );

            when(registrationServiceMock.listRegistrations(eventId)).thenReturn(registrations);

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.registrationDtoList").isArray())
                    .andExpect(jsonPath("$._embedded.registrationDtoList.length()")
                            .value(registrations.size()))
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0].siCardNumber").doesNotExist())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[1].siCardNumber").doesNotExist());
        }
    }

    @Nested
    @DisplayName("GET /api/events/{id}/registrations/me")
    class GetOwnRegistrationTests {

        @Test
        @DisplayName("should return full registration with SI card")
        @WithKlabisMockUser(userId = MEMBER_1_ID)
        void shouldReturnOwnRegistrationWithSiCard() throws Exception {
            final UUID eventId = UUID.randomUUID();
            final UserId  userId = UserId.fromString(MEMBER_1_ID);

            OwnRegistrationDto registration = new OwnRegistrationDto(
                    "John",
                    "Doe",
                    "123456",
                    Instant.now()
            );

            when(registrationServiceMock.getOwnRegistration(eventId, userId)).thenReturn(registration);

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
        @WithKlabisMockUser(userId = MEMBER_1_ID)
        void shouldReturn404WhenNotRegistered() throws Exception {
            UUID eventId = UUID.randomUUID();

            when(registrationServiceMock.getOwnRegistration(eq(eventId), any(UserId.class)))
                    .thenThrow(new RegistrationNotFoundException(UUID.fromString(MEMBER_1_ID), eventId));

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

    private LinkBuilder eventLinkBuilder(UUID eventId) {
        return new SimpleLinkBuilder("/api/events/" + eventId);
    }

    private static final class SimpleLinkBuilder implements LinkBuilder {
        private final String href;

        private SimpleLinkBuilder(String href) {
            this.href = href;
        }

        @Override
        public LinkBuilder slash(Object object) {
            return new SimpleLinkBuilder(href.endsWith("/") ? href + object : href + "/" + object);
        }

        @Override
        public URI toUri() {
            return URI.create(href);
        }

        @Override
        public Link withRel(LinkRelation rel) {
            return Link.of(href, rel);
        }

        @Override
        public Link withSelfRel() {
            return Link.of(href, "self");
        }
    }
}
