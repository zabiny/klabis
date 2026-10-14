package com.klabis.events.infrastructure.restapi;

import com.klabis.common.TargetGrant;
import com.klabis.common.authorization.TargetType;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.events.EventsWebMvcTest;
import com.klabis.common.users.Authority;
import com.klabis.events.EventCategory;
import com.klabis.events.EventCategoryId;
import com.klabis.events.EventId;
import com.klabis.events.EventTestDataBuilder;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.EventNotFoundException;
import com.klabis.events.application.EventRegistrationPort;
import com.klabis.events.application.MemberRegistrationSanctionPort;
import com.klabis.events.domain.*;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.klabis.events.EventTestDataBuilder.aRegistration;
import static com.klabis.events.EventTestDataBuilder.aRegistrationWithSharedServices;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Event Registration Controller API Tests")
@EventsWebMvcTest
class EventRegistrationControllerTest {

    private static final String MEMBER_1_ID = "11111111-1111-1111-1111-111111111111";
    private static final String COORDINATOR_ID = "cccccccc-cccc-cccc-cccc-cccccccccccc";
    private static final String COORDINATED_EVENT_ID = "00000000-0000-0000-0000-0000000000e1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventManagementPort eventManagementServiceMock;

    @Autowired
    private EventRegistrationPort registrationServiceMock;

    @Autowired
    private Members membersMock;

    @Autowired
    private MemberRegistrationSanctionPort memberRegistrationSanctionPortMock;

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

        @Test
        @DisplayName("passes wantsSharedTransport / wantsSharedAccommodation from the request into the command")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void passesSharedServiceChoicesIntoCommand() throws Exception {
            UUID eventId = UUID.randomUUID();
            String body = """
                    {"siCardNumber":"123456","categoryId":null,"wantsSharedTransport":true,"wantsSharedAccommodation":true}
                    """;

            mockMvc.perform(
                            post("/api/events/{eventId}/registrations", eventId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(body)
                    )
                    .andExpect(status().isCreated());

            ArgumentCaptor<Event.RegisterCommand> captor = ArgumentCaptor.forClass(Event.RegisterCommand.class);
            verify(registrationServiceMock).registerMember(eq(new EventId(eventId)), any(MemberId.class), captor.capture());
            assertThat(captor.getValue().wantsSharedTransport()).isTrue();
            assertThat(captor.getValue().wantsSharedAccommodation()).isTrue();
        }

        @Test
        @DisplayName("defaults both shared-service choices to false when the request omits them")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void defaultsSharedServiceChoicesToFalse() throws Exception {
            UUID eventId = UUID.randomUUID();
            String body = """
                    {"siCardNumber":"123456","categoryId":null}
                    """;

            mockMvc.perform(
                            post("/api/events/{eventId}/registrations", eventId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(body)
                    )
                    .andExpect(status().isCreated());

            ArgumentCaptor<Event.RegisterCommand> captor = ArgumentCaptor.forClass(Event.RegisterCommand.class);
            verify(registrationServiceMock).registerMember(eq(new EventId(eventId)), any(MemberId.class), captor.capture());
            assertThat(captor.getValue().wantsSharedTransport()).isFalse();
            assertThat(captor.getValue().wantsSharedAccommodation()).isFalse();
        }

        @Test
        @DisplayName("passes trimmed notes from the request into the command")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void passesNotesIntoCommand() throws Exception {
            UUID eventId = UUID.randomUUID();
            String body = """
                    {"siCardNumber":"123456","internalNote":"  club note ","orisNote":"oris note"}
                    """;

            mockMvc.perform(
                            post("/api/events/{eventId}/registrations", eventId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(body)
                    )
                    .andExpect(status().isCreated());

            ArgumentCaptor<Event.RegisterCommand> captor = ArgumentCaptor.forClass(Event.RegisterCommand.class);
            verify(registrationServiceMock).registerMember(eq(new EventId(eventId)), any(MemberId.class), captor.capture());
            assertThat(captor.getValue().internalNote().text()).isEqualTo("club note");
            assertThat(captor.getValue().orisNote().text()).isEqualTo("oris note");
        }

        @Test
        @DisplayName("rejects a note longer than 255 characters with 400")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void rejectsOverLengthNote() throws Exception {
            String body = "{\"siCardNumber\":\"123456\",\"orisNote\":\"" + "a".repeat(256) + "\"}";

            mockMvc.perform(
                            post("/api/events/{eventId}/registrations", UUID.randomUUID())
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(body)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.orisNote").exists());

            verifyNoInteractions(registrationServiceMock);
        }

        @Test
        @DisplayName("accepts a note of exactly 255 characters")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void acceptsNoteAtMaxLength() throws Exception {
            String body = "{\"siCardNumber\":\"123456\",\"orisNote\":\"" + "a".repeat(255) + "\"}";

            mockMvc.perform(
                            post("/api/events/{eventId}/registrations", UUID.randomUUID())
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(body)
                    )
                    .andExpect(status().isCreated());
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
                    aRegistration(member1Id, SiCardNumber.of("1234")),
                    aRegistration(member2Id, SiCardNumber.of("5678"))
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
        @DisplayName("self link href in registration list must not contain URI template syntax")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void selfLinkHrefMustNotContainUriTemplateSyntax() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.randomUUID());
            List<EventRegistration> registrations = List.of(
                    aRegistration(memberId, SiCardNumber.of("1234"))
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
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.self.href").value(not(containsString("{?sort}"))));
        }

        @Test
        @DisplayName("should include category in registration list when registration has category")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldIncludeCategoryInRegistrationList() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.randomUUID());
            EventCategory m21 = EventCategory.create("M21");
            List<EventRegistration> registrations = List.of(
                    aRegistration(memberId, SiCardNumber.of("1234"), m21.id())
            );
            Event closedEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().minusDays(5))
                    .withCategories(List.of(m21))
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
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0].category.name").value("M21"));
        }

        @Test
        @DisplayName("should return category as null when registration's categoryId no longer exists on the event (orphaned)")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldReturnNullCategoryWhenOrphaned() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.randomUUID());
            EventCategoryId removedCategoryId = EventCategoryId.generate();
            List<EventRegistration> registrations = List.of(
                    aRegistration(memberId, SiCardNumber.of("1234"), removedCategoryId)
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
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0].category").value(nullValue()));
        }
    }

    @Nested
    @DisplayName("GET /api/events/{id}/registrations — registrationTime privacy (N9)")
    class RegistrationTimePrivacyTests {

        private static final String REGULAR_MEMBER_ID = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee";

        @Test
        @DisplayName("coordinator rows carry the registration link, other members' rows of a regular member do not")
        @WithKlabisMockUser(memberId = REGULAR_MEMBER_ID)
        void regularMemberRowsOfOthersCarryNoRegistrationLink() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId ownId = new MemberId(UUID.fromString(REGULAR_MEMBER_ID));
            MemberId otherId = new MemberId(UUID.randomUUID());

            Event event = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .addRegistrations(List.of(
                            aRegistration(ownId, SiCardNumber.of("1234")),
                            aRegistration(otherId, SiCardNumber.of("5678"))))
                    .build();
            event.publish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(membersMock.findByIds(any())).thenReturn(Map.of(
                    ownId, new MemberDto(ownId.value(), "John", "Doe", "john@example.com"),
                    otherId, new MemberDto(otherId.value(), "Jane", "Smith", "jane@example.com")
            ));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0]._links.self.href").exists())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[1]._links.self").doesNotExist());
        }

        @Test
        @DisplayName("event coordinator rows carry the registration link")
        @WithKlabisMockUser(memberId = COORDINATOR_ID, targetGrants = @TargetGrant(authority = Authority.EVENTS_REGISTRATIONS_READ, type = TargetType.EVENT, ids = COORDINATED_EVENT_ID))
        void coordinatorRowsCarryRegistrationLink() throws Exception {
            UUID eventId = UUID.fromString(COORDINATED_EVENT_ID);
            MemberId memberId = new MemberId(UUID.randomUUID());

            Event event = EventTestDataBuilder.anEventWithId(new EventId(eventId))
                    .withDate(LocalDate.now().plusDays(30))
                    .addRegistrations(List.of(
                            aRegistration(memberId, SiCardNumber.of("1234"))))
                    .build();
            event.publish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(membersMock.findByIds(any())).thenReturn(Map.of(
                    memberId, new MemberDto(memberId.value(), "John", "Doe", "john@example.com")
            ));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0]._links.self.href",
                            containsString("/registrations/" + memberId.value())))
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0]._templates").doesNotExist());
        }

        @Test
        @DisplayName("regular member does not see registrationTime in registration list")
        @WithKlabisMockUser(memberId = REGULAR_MEMBER_ID)
        void regularMemberDoesNotSeeRegistrationTime() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.randomUUID());
            MemberId coordinatorId = new MemberId(UUID.fromString(COORDINATOR_ID));

            List<EventRegistration> registrations = List.of(
                    aRegistration(memberId, SiCardNumber.of("1234"))
            );
            Event event = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .withCoordinator(coordinatorId)
                    .addRegistrations(registrations)
                    .build();
            event.publish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(membersMock.findByIds(any())).thenReturn(Map.of(
                    memberId, new MemberDto(memberId.value(), "John", "Doe", "john@example.com")
            ));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0].registrationTime").doesNotExist());
        }

        @Test
        @DisplayName("event coordinator sees registrationTime in registration list")
        @WithKlabisMockUser(memberId = COORDINATOR_ID, targetGrants = @TargetGrant(authority = Authority.EVENTS_REGISTRATIONS_READ, type = TargetType.EVENT, ids = COORDINATED_EVENT_ID))
        void eventCoordinatorSeesRegistrationTime() throws Exception {
            UUID eventId = UUID.fromString(COORDINATED_EVENT_ID);
            MemberId memberId = new MemberId(UUID.randomUUID());

            List<EventRegistration> registrations = List.of(
                    aRegistration(memberId, SiCardNumber.of("1234"))
            );
            Event event = EventTestDataBuilder.anEventWithId(new EventId(eventId))
                    .withDate(LocalDate.now().plusDays(30))
                    .addRegistrations(registrations)
                    .build();
            event.publish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(membersMock.findByIds(any())).thenReturn(Map.of(
                    memberId, new MemberDto(memberId.value(), "John", "Doe", "john@example.com")
            ));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0].registrationTime").exists());
        }

        @Test
        @DisplayName("user with EVENTS:REGISTRATIONS sees registrationTime in registration list")
        @WithKlabisMockUser(memberId = REGULAR_MEMBER_ID, authorities = {Authority.EVENTS_REGISTRATIONS})
        void eventsRegistrationsAuthoritySeesRegistrationTime() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.randomUUID());
            MemberId coordinatorId = new MemberId(UUID.fromString(COORDINATOR_ID));

            List<EventRegistration> registrations = List.of(
                    aRegistration(memberId, SiCardNumber.of("1234"))
            );
            Event event = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .withCoordinator(coordinatorId)
                    .addRegistrations(registrations)
                    .build();
            event.publish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(membersMock.findByIds(any())).thenReturn(Map.of(
                    memberId, new MemberDto(memberId.value(), "John", "Doe", "john@example.com")
            ));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0].registrationTime").exists());
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
                    {"siCardNumber":"123456","categoryId":null}
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
                    {"siCardNumber":"123456","categoryId":null}
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
                    {"siCardNumber":"abc","categoryId":null}
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
            String unknownCategoryId = UUID.randomUUID().toString();
            String body = """
                    {"siCardNumber":"123456","categoryId":"%s"}
                    """.formatted(unknownCategoryId);

            doThrow(new com.klabis.common.exceptions.BusinessRuleViolationException("Category '" + unknownCategoryId + "' is not available for this event") {})
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

        @Test
        @DisplayName("3.1 user with EVENTS:REGISTRATIONS can PUT another member's registration and it is updated")
        @WithKlabisMockUser(memberId = "22222222-2222-2222-2222-222222222222", authorities = {Authority.EVENTS_REGISTRATIONS})
        void shouldAllowEditByEventsRegistrationsAuthorityForNonOwner() throws Exception {
            UUID eventId = UUID.randomUUID();
            String body = """
                    {"siCardNumber":"999888","categoryId":null}
                    """;

            mockMvc.perform(
                            put("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(body)
                    )
                    .andExpect(status().isNoContent());

            verify(registrationServiceMock).editRegistration(
                    eq(new EventId(eventId)),
                    eq(new MemberId(UUID.fromString(MEMBER_1_ID))),
                    any(Event.EditRegistrationCommand.class));
        }

        @Test
        @DisplayName("3.2 user without EVENTS:REGISTRATIONS (and not the owner) receives 403 on PUT")
        @WithKlabisMockUser(memberId = "22222222-2222-2222-2222-222222222222")
        void shouldReturn403ForNonOwnerWithoutEventsRegistrations() throws Exception {
            UUID eventId = UUID.randomUUID();
            String body = """
                    {"siCardNumber":"999888","categoryId":null}
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
        @DisplayName("3.3 member without special authority can PUT own registration (owner flow preserved)")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldAllowOwnerToEditOwnRegistrationWithoutSpecialAuthority() throws Exception {
            UUID eventId = UUID.randomUUID();
            String body = """
                    {"siCardNumber":"111222","categoryId":null}
                    """;

            mockMvc.perform(
                            put("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(body)
                    )
                    .andExpect(status().isNoContent());

            verify(registrationServiceMock).editRegistration(
                    eq(new EventId(eventId)),
                    eq(new MemberId(UUID.fromString(MEMBER_1_ID))),
                    any(Event.EditRegistrationCommand.class));
        }

        @Test
        @DisplayName("passes wantsSharedTransport / wantsSharedAccommodation from the request into the command")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void passesSharedServiceChoicesIntoEditCommand() throws Exception {
            UUID eventId = UUID.randomUUID();
            String body = """
                    {"siCardNumber":"123456","categoryId":null,"wantsSharedTransport":true,"wantsSharedAccommodation":false}
                    """;

            mockMvc.perform(
                            put("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(body)
                    )
                    .andExpect(status().isNoContent());

            ArgumentCaptor<Event.EditRegistrationCommand> captor =
                    ArgumentCaptor.forClass(Event.EditRegistrationCommand.class);
            verify(registrationServiceMock).editRegistration(eq(new EventId(eventId)),
                    eq(new MemberId(UUID.fromString(MEMBER_1_ID))), captor.capture());
            assertThat(captor.getValue().wantsSharedTransport()).isTrue();
            assertThat(captor.getValue().wantsSharedAccommodation()).isFalse();
        }

        @Test
        @DisplayName("passes trimmed notes from the request into the edit command")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void passesNotesIntoEditCommand() throws Exception {
            UUID eventId = UUID.randomUUID();
            String body = """
                    {"siCardNumber":"123456","internalNote":"  club note ","orisNote":"oris note"}
                    """;

            mockMvc.perform(put("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content(body))
                    .andExpect(status().isNoContent());

            ArgumentCaptor<Event.EditRegistrationCommand> captor =
                    ArgumentCaptor.forClass(Event.EditRegistrationCommand.class);
            verify(registrationServiceMock).editRegistration(eq(new EventId(eventId)),
                    eq(new MemberId(UUID.fromString(MEMBER_1_ID))), captor.capture());
            assertThat(captor.getValue().internalNote().text()).isEqualTo("club note");
            assertThat(captor.getValue().orisNote().text()).isEqualTo("oris note");
        }

        @Test
        @DisplayName("absent and blank notes reach the command as no note")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void absentAndBlankNotesMeanNoNote() throws Exception {
            UUID eventId = UUID.randomUUID();
            String body = """
                    {"siCardNumber":"123456","orisNote":"   "}
                    """;

            mockMvc.perform(put("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content(body))
                    .andExpect(status().isNoContent());

            ArgumentCaptor<Event.EditRegistrationCommand> captor =
                    ArgumentCaptor.forClass(Event.EditRegistrationCommand.class);
            verify(registrationServiceMock).editRegistration(eq(new EventId(eventId)),
                    eq(new MemberId(UUID.fromString(MEMBER_1_ID))), captor.capture());
            assertThat(captor.getValue().internalNote()).isNull();
            assertThat(captor.getValue().orisNote()).isNull();
        }

        @Test
        @DisplayName("user with EVENTS:REGISTRATIONS edits another member's notes")
        @WithKlabisMockUser(memberId = "22222222-2222-2222-2222-222222222222", authorities = {Authority.EVENTS_REGISTRATIONS})
        void registrationsManagerEditsNotesOfAnotherMember() throws Exception {
            UUID eventId = UUID.randomUUID();
            String body = """
                    {"siCardNumber":"123456","internalNote":"by manager"}
                    """;

            mockMvc.perform(put("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content(body))
                    .andExpect(status().isNoContent());

            ArgumentCaptor<Event.EditRegistrationCommand> captor =
                    ArgumentCaptor.forClass(Event.EditRegistrationCommand.class);
            verify(registrationServiceMock).editRegistration(eq(new EventId(eventId)),
                    eq(new MemberId(UUID.fromString(MEMBER_1_ID))), captor.capture());
            assertThat(captor.getValue().internalNote().text()).isEqualTo("by manager");
        }

        @Test
        @DisplayName("rejects a note longer than 255 characters on edit with 400")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void rejectsTooLongNoteOnEdit() throws Exception {
            String body = """
                    {"siCardNumber":"123456","internalNote":"%s"}
                    """.formatted("x".repeat(256));

            mockMvc.perform(put("/api/events/{eventId}/registrations/{memberId}", UUID.randomUUID(), MEMBER_1_ID)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.internalNote").exists());
            verifyNoInteractions(registrationServiceMock);
        }

        @Test
        @DisplayName("accepts a note of exactly 255 characters on edit")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void acceptsNoteAtMaxLengthOnEdit() throws Exception {
            String body = """
                    {"siCardNumber":"123456","internalNote":"%s"}
                    """.formatted("x".repeat(255));

            mockMvc.perform(put("/api/events/{eventId}/registrations/{memberId}", UUID.randomUUID(), MEMBER_1_ID)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content(body))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("GET /api/events/{id}/registrations — sort parameter (N10)")
    class RegistrationSortTests {

        private static final String COORDINATOR_ID = "cccccccc-cccc-cccc-cccc-cccccccccccc";
        private static final String REGULAR_MEMBER_ID = "aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

        private Event buildEventWithRegistrations(MemberId coordinatorId, List<EventRegistration> registrations) {
            return buildEventWithRegistrations(coordinatorId, registrations, List.of());
        }

        private Event buildEventWithRegistrations(MemberId coordinatorId, List<EventRegistration> registrations, List<EventCategory> categories) {
            Event event = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().minusDays(5))
                    .withCoordinator(coordinatorId)
                    .withCategories(categories)
                    .addRegistrations(registrations)
                    .build();
            event.publish();
            event.finish();
            return event;
        }

        @Test
        @DisplayName("no sort parameter → default registrationTime ASC (first registered appears first)")
        @WithKlabisMockUser(memberId = COORDINATOR_ID, authorities = {Authority.EVENTS_REGISTRATIONS})
        void defaultSortIsRegistrationTimeAsc() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId member1Id = new MemberId(UUID.randomUUID());
            MemberId member2Id = new MemberId(UUID.randomUUID());
            Instant earlier = Instant.now().minus(10, ChronoUnit.MINUTES);
            Instant later = Instant.now();

            List<EventRegistration> registrations = List.of(
                    aRegistration(member2Id, SiCardNumber.of("5678"), later),
                    aRegistration(member1Id, SiCardNumber.of("1234"), earlier)
            );
            Event event = buildEventWithRegistrations(new MemberId(UUID.fromString(COORDINATOR_ID)), registrations);

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(membersMock.findByIds(any())).thenReturn(Map.of(
                    member1Id, new MemberDto(member1Id.value(), "Alice", "Zeta", "alice@example.com"),
                    member2Id, new MemberDto(member2Id.value(), "Bob", "Alpha", "bob@example.com")
            ));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0].firstName").value("Alice"))
                    .andExpect(jsonPath("$._embedded.registrationDtoList[1].firstName").value("Bob"));
        }

        @Test
        @DisplayName("sort=firstName → results ordered by first name ascending")
        @WithKlabisMockUser(memberId = REGULAR_MEMBER_ID)
        void sortByFirstName() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId member1Id = new MemberId(UUID.randomUUID());
            MemberId member2Id = new MemberId(UUID.randomUUID());

            List<EventRegistration> registrations = List.of(
                    aRegistration(member1Id, SiCardNumber.of("1234"), Instant.now().minus(5, ChronoUnit.MINUTES)),
                    aRegistration(member2Id, SiCardNumber.of("5678"))
            );
            Event event = buildEventWithRegistrations(new MemberId(UUID.fromString(COORDINATOR_ID)), registrations);

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(membersMock.findByIds(any())).thenReturn(Map.of(
                    member1Id, new MemberDto(member1Id.value(), "Zdeněk", "Smith", "z@example.com"),
                    member2Id, new MemberDto(member2Id.value(), "Adam", "Jones", "a@example.com")
            ));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations", eventId)
                                    .param("sort", "firstName")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0].firstName").value("Adam"))
                    .andExpect(jsonPath("$._embedded.registrationDtoList[1].firstName").value("Zdeněk"));
        }

        @Test
        @DisplayName("sort=lastName → results ordered by last name ascending")
        @WithKlabisMockUser(memberId = REGULAR_MEMBER_ID)
        void sortByLastName() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId member1Id = new MemberId(UUID.randomUUID());
            MemberId member2Id = new MemberId(UUID.randomUUID());

            List<EventRegistration> registrations = List.of(
                    aRegistration(member1Id, SiCardNumber.of("1234"), Instant.now().minus(5, ChronoUnit.MINUTES)),
                    aRegistration(member2Id, SiCardNumber.of("5678"))
            );
            Event event = buildEventWithRegistrations(new MemberId(UUID.fromString(COORDINATOR_ID)), registrations);

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(membersMock.findByIds(any())).thenReturn(Map.of(
                    member1Id, new MemberDto(member1Id.value(), "John", "Žák", "j@example.com"),
                    member2Id, new MemberDto(member2Id.value(), "Jane", "Beneš", "ja@example.com")
            ));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations", eventId)
                                    .param("sort", "lastName")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0].lastName").value("Beneš"))
                    .andExpect(jsonPath("$._embedded.registrationDtoList[1].lastName").value("Žák"));
        }

        @Test
        @DisplayName("sort=category → results ordered by category ascending")
        @WithKlabisMockUser(memberId = REGULAR_MEMBER_ID)
        void sortByCategory() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId member1Id = new MemberId(UUID.randomUUID());
            MemberId member2Id = new MemberId(UUID.randomUUID());
            EventCategory w21 = EventCategory.create("W21");
            EventCategory m21 = EventCategory.create("M21");

            List<EventRegistration> registrations = List.of(
                    aRegistration(member1Id, SiCardNumber.of("1234"), w21.id(), Instant.now().minus(5, ChronoUnit.MINUTES)),
                    aRegistration(member2Id, SiCardNumber.of("5678"), m21.id())
            );
            Event event = buildEventWithRegistrations(new MemberId(UUID.fromString(COORDINATOR_ID)), registrations, List.of(w21, m21));

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(membersMock.findByIds(any())).thenReturn(Map.of(
                    member1Id, new MemberDto(member1Id.value(), "Alice", "Smith", "a@example.com"),
                    member2Id, new MemberDto(member2Id.value(), "Bob", "Jones", "b@example.com")
            ));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations", eventId)
                                    .param("sort", "category")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0].category.name").value("M21"))
                    .andExpect(jsonPath("$._embedded.registrationDtoList[1].category.name").value("W21"));
        }

        @Test
        @DisplayName("sort=registrationTime as authorized user (EVENTS:REGISTRATIONS) → ordered by registrationTime ascending")
        @WithKlabisMockUser(memberId = REGULAR_MEMBER_ID, authorities = {Authority.EVENTS_REGISTRATIONS})
        void sortByRegistrationTimeAsAuthorizedUser() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId member1Id = new MemberId(UUID.randomUUID());
            MemberId member2Id = new MemberId(UUID.randomUUID());
            Instant earlier = Instant.now().minus(10, ChronoUnit.MINUTES);
            Instant later = Instant.now();

            List<EventRegistration> registrations = List.of(
                    aRegistration(member2Id, SiCardNumber.of("5678"), later),
                    aRegistration(member1Id, SiCardNumber.of("1234"), earlier)
            );
            Event event = buildEventWithRegistrations(new MemberId(UUID.fromString(COORDINATOR_ID)), registrations);

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(membersMock.findByIds(any())).thenReturn(Map.of(
                    member1Id, new MemberDto(member1Id.value(), "Alice", "Smith", "a@example.com"),
                    member2Id, new MemberDto(member2Id.value(), "Bob", "Jones", "b@example.com")
            ));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations", eventId)
                                    .param("sort", "registrationTime")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0].firstName").value("Alice"))
                    .andExpect(jsonPath("$._embedded.registrationDtoList[1].firstName").value("Bob"));
        }

        @Test
        @DisplayName("sort=registrationTime as unauthorized member → silently falls back to default sort (registrationTime ASC)")
        @WithKlabisMockUser(memberId = REGULAR_MEMBER_ID)
        void sortByRegistrationTimeAsUnauthorizedMemberFallsBackToDefault() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId member1Id = new MemberId(UUID.randomUUID());
            MemberId member2Id = new MemberId(UUID.randomUUID());
            Instant earlier = Instant.now().minus(10, ChronoUnit.MINUTES);
            Instant later = Instant.now();

            List<EventRegistration> registrations = List.of(
                    aRegistration(member2Id, SiCardNumber.of("5678"), later),
                    aRegistration(member1Id, SiCardNumber.of("1234"), earlier)
            );
            Event event = buildEventWithRegistrations(new MemberId(UUID.fromString(COORDINATOR_ID)), registrations);

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(membersMock.findByIds(any())).thenReturn(Map.of(
                    member1Id, new MemberDto(member1Id.value(), "Alice", "Smith", "a@example.com"),
                    member2Id, new MemberDto(member2Id.value(), "Bob", "Jones", "b@example.com")
            ));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations", eventId)
                                    .param("sort", "registrationTime")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    // falls back to default registrationTime ASC — Alice registered earlier appears first
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0].firstName").value("Alice"))
                    .andExpect(jsonPath("$._embedded.registrationDtoList[1].firstName").value("Bob"));
        }

        @Test
        @DisplayName("unknown sort field → silently falls back to default sort (registrationTime ASC), no 400/403")
        @WithKlabisMockUser(memberId = REGULAR_MEMBER_ID)
        void unknownSortFieldFallsBackToDefault() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId member1Id = new MemberId(UUID.randomUUID());
            MemberId member2Id = new MemberId(UUID.randomUUID());
            Instant earlier = Instant.now().minus(10, ChronoUnit.MINUTES);
            Instant later = Instant.now();

            List<EventRegistration> registrations = List.of(
                    aRegistration(member2Id, SiCardNumber.of("5678"), later),
                    aRegistration(member1Id, SiCardNumber.of("1234"), earlier)
            );
            Event event = buildEventWithRegistrations(new MemberId(UUID.fromString(COORDINATOR_ID)), registrations);

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(membersMock.findByIds(any())).thenReturn(Map.of(
                    member1Id, new MemberDto(member1Id.value(), "Alice", "Smith", "a@example.com"),
                    member2Id, new MemberDto(member2Id.value(), "Bob", "Jones", "b@example.com")
            ));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations", eventId)
                                    .param("sort", "unknownField")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0].firstName").value("Alice"))
                    .andExpect(jsonPath("$._embedded.registrationDtoList[1].firstName").value("Bob"));
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
                    aRegistration(member1Id, SiCardNumber.of("1234")),
                    aRegistration(member2Id, SiCardNumber.of("5678"))
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
                    aRegistration(member1Id, SiCardNumber.of("1234"))
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

        @Test
        @DisplayName("4.1 holder of EVENTS:REGISTRATIONS sees editRegistration affordance and self-link on EVERY row when registrations are open")
        @WithKlabisMockUser(memberId = "22222222-2222-2222-2222-222222222222", authorities = {Authority.EVENTS_REGISTRATIONS})
        void shouldExposeEditAffordanceOnEveryRowForEventsRegistrationsHolder() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId member1Id = new MemberId(UUID.fromString(MEMBER_1_ID));
            MemberId member2Id = new MemberId(UUID.fromString("33333333-3333-3333-3333-333333333333"));

            List<EventRegistration> registrations = List.of(
                    aRegistration(member1Id, SiCardNumber.of("1234")),
                    aRegistration(member2Id, SiCardNumber.of("5678"))
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
                    .andExpect(jsonPath("$._embedded.registrationDtoList[1]._links.self.href").exists())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[1]._templates.editRegistration.method").value("PUT"));
        }

        @Test
        @DisplayName("4.3 holder of EVENTS:REGISTRATIONS sees no editRegistration affordance when registrations are closed")
        @WithKlabisMockUser(memberId = "22222222-2222-2222-2222-222222222222", authorities = {Authority.EVENTS_REGISTRATIONS})
        void shouldNotExposeEditAffordanceForEventsRegistrationsHolderWhenRegistrationsClosed() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId member1Id = new MemberId(UUID.fromString(MEMBER_1_ID));
            MemberId member2Id = new MemberId(UUID.fromString("33333333-3333-3333-3333-333333333333"));

            List<EventRegistration> registrations = List.of(
                    aRegistration(member1Id, SiCardNumber.of("1234")),
                    aRegistration(member2Id, SiCardNumber.of("5678"))
            );

            Event finishedEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().minusDays(5))
                    .addRegistrations(registrations)
                    .build();
            finishedEvent.publish();
            finishedEvent.finish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(finishedEvent);
            when(membersMock.findByIds(any())).thenReturn(Map.of(
                    member1Id, new MemberDto(member1Id.value(), "John", "Doe", "john@example.com"),
                    member2Id, new MemberDto(member2Id.value(), "Jane", "Smith", "jane@example.com")
            ));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0]._templates").doesNotExist())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[1]._templates").doesNotExist());
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

            EventRegistration registration = aRegistration(memberId, SiCardNumber.of("123456"));
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
                    .andExpect(jsonPath("$._links.self.href", not(containsString("/me"))))
                    .andExpect(jsonPath("$._templates.editRegistration.method").value("PUT"))
                    .andExpect(jsonPath("$._templates.unregisterFromEvent.method").value("DELETE"))
                    .andExpect(jsonPath("$._templates.registerForEvent").doesNotExist());
        }

        @Test
        @DisplayName("2.1 user with EVENTS:REGISTRATIONS gets 200 with SI card number (non-owner)")
        @WithKlabisMockUser(memberId = "22222222-2222-2222-2222-222222222222", authorities = {Authority.EVENTS_REGISTRATIONS})
        void shouldReturn200WithSiCardForEventsRegistrationsAuthority() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId targetMemberId = new MemberId(UUID.fromString(MEMBER_1_ID));

            EventRegistration registration = aRegistration(targetMemberId, SiCardNumber.of("654321"));
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
        @DisplayName("2.2 user with only EVENTS:MANAGE (not owner, not EVENTS:REGISTRATIONS) gets 403")
        @WithKlabisMockUser(memberId = "22222222-2222-2222-2222-222222222222", authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn403ForNonOwnerWithEventsManageOnly() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("event coordinator gets 200 with SI card number and neither edit nor unregister template")
        @WithKlabisMockUser(memberId = COORDINATOR_ID, targetGrants = @TargetGrant(authority = Authority.EVENTS_REGISTRATIONS_READ, type = TargetType.EVENT, ids = COORDINATED_EVENT_ID))
        void shouldReturn200ForCoordinatorWithoutEditTemplates() throws Exception {
            UUID eventId = UUID.fromString(COORDINATED_EVENT_ID);
            MemberId targetMemberId = new MemberId(UUID.fromString(MEMBER_1_ID));

            EventRegistration registration = aRegistration(targetMemberId, SiCardNumber.of("654321"));
            Event activeEvent = EventTestDataBuilder.anEventWithId(new EventId(eventId))
                    .withDate(LocalDate.now().plusDays(30))
                    .addRegistration(registration)
                    .build();
            activeEvent.publish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(activeEvent);
            when(membersMock.findById(targetMemberId)).thenReturn(Optional.of(
                    new MemberDto(targetMemberId.value(), "Jane", "Smith", "jane@example.com")));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.siCardNumber").value("654321"))
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._templates.editRegistration").doesNotExist())
                    .andExpect(jsonPath("$._templates.unregisterFromEvent").doesNotExist());
        }

        @Test
        @DisplayName("owner reads both notes back")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void ownerReadsNotes() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.fromString(MEMBER_1_ID));
            Event event = eventWithNotedRegistration(null, memberId);
            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(membersMock.findById(memberId)).thenReturn(Optional.of(
                    new MemberDto(memberId.value(), "John", "Doe", "john@example.com")));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.internalNote").value("club note"))
                    .andExpect(jsonPath("$.orisNote").value("oris note"));
        }

        @Test
        @DisplayName("editRegistration template offers both note inputs limited to 255 characters")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void editTemplateOffersLimitedNoteInputs() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.fromString(MEMBER_1_ID));
            Event event = eventWithNotedRegistration(null, memberId);
            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(membersMock.findById(memberId)).thenReturn(Optional.of(
                    new MemberDto(memberId.value(), "John", "Doe", "john@example.com")));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.editRegistration.properties[?(@.name=='internalNote')].max").value(255))
                    .andExpect(jsonPath("$._templates.editRegistration.properties[?(@.name=='orisNote')].max").value(255));
        }

        @Test
        @DisplayName("event coordinator sees both notes")
        @WithKlabisMockUser(memberId = COORDINATOR_ID, targetGrants = @TargetGrant(authority = Authority.EVENTS_REGISTRATIONS_READ, type = TargetType.EVENT, ids = COORDINATED_EVENT_ID))
        void coordinatorReadsNotes() throws Exception {
            UUID eventId = UUID.fromString(COORDINATED_EVENT_ID);
            MemberId memberId = new MemberId(UUID.fromString(MEMBER_1_ID));
            Event event = eventWithNotedRegistration(new EventId(eventId), memberId);
            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(membersMock.findById(memberId)).thenReturn(Optional.of(
                    new MemberDto(memberId.value(), "Jane", "Smith", "jane@example.com")));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.internalNote").value("club note"))
                    .andExpect(jsonPath("$.orisNote").value("oris note"));
        }

        private Event eventWithNotedRegistration(EventId eventId, MemberId memberId) {
            EventRegistration registration = EventRegistration.reconstruct(UUID.randomUUID(), Instant.now(), EventRegistrationCreateEventRegistrationBuilder.builder().memberId(memberId).siCardNumber(SiCardNumber.of("123456")).internalNote(RegistrationNote.ofNullable("club note")).orisNote(RegistrationNote.ofNullable("oris note")).build());
            var builder = eventId != null ? EventTestDataBuilder.anEventWithId(eventId) : EventTestDataBuilder.anEvent();
            Event event = builder.withDate(LocalDate.now().plusDays(30)).addRegistration(registration).build();
            event.publish();
            return event;
        }

        @Test
        @DisplayName("coordinator of another event gets 403")
        @WithKlabisMockUser(memberId = COORDINATOR_ID, targetGrants = @TargetGrant(authority = Authority.EVENTS_REGISTRATIONS_READ, type = TargetType.EVENT, ids = COORDINATED_EVENT_ID))
        void shouldReturn403ForCoordinatorOfAnotherEvent() throws Exception {
            mockMvc.perform(
                            get("/api/events/{eventId}/registrations/{memberId}", UUID.randomUUID(), MEMBER_1_ID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("1.3 non-owner without any special authority gets 403")
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
        @DisplayName("RegistrationDto carries the member's stored shared-service choices when the event offers them")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void registrationDtoCarriesStoredChoicesWhenOffered() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.fromString(MEMBER_1_ID));

            EventRegistration registration = aRegistrationWithSharedServices(memberId, SiCardNumber.of("123456"), true, false);
            Event activeEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .withSharedTransportEnabled(true)
                    .withSharedAccommodationEnabled(true)
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
                    .andExpect(jsonPath("$.wantsSharedTransport").value(true))
                    .andExpect(jsonPath("$.wantsSharedAccommodation").value(false));
        }

        @Test
        @DisplayName("RegistrationDto omits a shared-service choice when the event does not offer it")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void registrationDtoOmitsChoiceWhenNotOffered() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.fromString(MEMBER_1_ID));

            EventRegistration registration = aRegistrationWithSharedServices(memberId, SiCardNumber.of("123456"), true, true);
            Event activeEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .withSharedTransportEnabled(true)
                    .withSharedAccommodationEnabled(false)
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
                    .andExpect(jsonPath("$.wantsSharedTransport").value(true))
                    .andExpect(jsonPath("$.wantsSharedAccommodation").doesNotExist());
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

    @Nested
    @DisplayName("GET /api/events/{eventId}/registrations/{memberId}?newRegistration=true — SI prefill defaults (N2)")
    class NewRegistrationDefaultsTests {

        @Test
        @DisplayName("new=true for memberId == principal returns 200 with prefill from profile and registerForEvent affordance")
        @WithKlabisMockUser(memberId = MEMBER_1_ID, authorities = {Authority.EVENTS_READ})
        void shouldReturn200WithPrefillWhenPrincipalMatchesMemberId() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.fromString(MEMBER_1_ID));

            Event activeEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .withCategoryNames("M21", "W21")
                    .build();
            activeEvent.publish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(activeEvent);
            when(membersMock.findById(memberId)).thenReturn(Optional.of(
                    new MemberDto(memberId.value(), "John", "Doe", "john@example.com", null, null, "123456")));

            List<EventCategory> categories = activeEvent.getCategories();

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .param("newRegistration", "true")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.siCardNumber").value("123456"))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$._templates.registerForEvent.method").value("POST"))
                    .andExpect(jsonPath("$._templates.registerForEvent.target", containsString("/registrations")))
                    .andExpect(jsonPath("$._templates.registerForEvent.properties[?(@.name=='categoryId')].options.inline").isArray())
                    .andExpect(jsonPath("$._templates.registerForEvent.properties[?(@.name=='categoryId')].options.inline[0].value")
                            .value(categories.get(0).id().toString()))
                    .andExpect(jsonPath("$._templates.registerForEvent.properties[?(@.name=='categoryId')].options.inline[0].prompt")
                            .value("M21"))
                    .andExpect(jsonPath("$._templates.editRegistration").doesNotExist())
                    .andExpect(jsonPath("$._templates.unregisterFromEvent").doesNotExist())
                    .andExpect(jsonPath("$._links.event.href").exists());
        }

        @Test
        @DisplayName("new=true on event with closed registrations returns 200 without any registration affordance")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldNotOfferRegisterForEventWhenRegistrationsClosed() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.fromString(MEMBER_1_ID));

            Event closedEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .withRegistrationDeadline(LocalDate.now().minusDays(1))
                    .build();
            closedEvent.publish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(closedEvent);
            when(membersMock.findById(memberId)).thenReturn(Optional.of(
                    new MemberDto(memberId.value(), "John", "Doe", "john@example.com", null, null, "123456")));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .param("newRegistration", "true")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates").doesNotExist());
        }

        @Test
        @DisplayName("new=true for member blocked by sanction returns 200 without registerForEvent affordance")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldNotOfferRegisterForEventWhenMemberIsBlocked() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.fromString(MEMBER_1_ID));

            Event activeEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .build();
            activeEvent.publish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(activeEvent);
            when(membersMock.findById(memberId)).thenReturn(Optional.of(
                    new MemberDto(memberId.value(), "John", "Doe", "john@example.com", null, null, "123456")));
            when(memberRegistrationSanctionPortMock.isMemberBlocked(memberId)).thenReturn(true);

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .param("newRegistration", "true")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates").doesNotExist());
        }

        @Test
        @DisplayName("new=true for different memberId (not the principal) returns 403")
        @WithKlabisMockUser(memberId = "22222222-2222-2222-2222-222222222222")
        void shouldReturn403WhenPrincipalDiffersFromMemberId() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .param("newRegistration", "true")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("new=true for admin with EVENTS:REGISTRATIONS authority but different memberId returns 403 — authority must not bypass principal==memberId check")
        @WithKlabisMockUser(memberId = "22222222-2222-2222-2222-222222222222", authorities = {Authority.EVENTS_REGISTRATIONS})
        void shouldReturn403WhenAdminWithEventsRegistrationsAuthorityCallsNewRegistrationForDifferentMember() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .param("newRegistration", "true")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("new=true unauthenticated returns 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .param("newRegistration", "true")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("new=false on non-existing registration still returns 404 (regression)")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldReturn404WhenNewFalseAndNoRegistration() throws Exception {
            UUID eventId = UUID.randomUUID();

            Event eventWithoutRegistration = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .build();
            eventWithoutRegistration.publish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(eventWithoutRegistration);

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .param("newRegistration", "false")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("new=true for member without siCardNumber in profile returns 200 with empty siCardNumber value (no error)")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldReturn200WithNullSiCardWhenMemberHasNoSiCard() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.fromString(MEMBER_1_ID));

            Event activeEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .build();
            activeEvent.publish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(activeEvent);
            when(membersMock.findById(memberId)).thenReturn(Optional.of(
                    new MemberDto(memberId.value(), "John", "Doe", "john@example.com")));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                                    .param("newRegistration", "true")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.siCardNumber").doesNotExist());
        }

    }

    @Nested
    @DisplayName("GET /api/events/{id}/registrations — recordTransaction affordance (finance)")
    class RecordTransactionAffordanceTests {

        @Test
        @DisplayName("1.1 user with FINANCE:MANAGE sees recordTransaction link on every registration row")
        @WithKlabisMockUser(memberId = MEMBER_1_ID, authorities = {Authority.FINANCE_MANAGE})
        void shouldExposeRecordTransactionLinkForFinanceManager() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId member1Id = new MemberId(UUID.randomUUID());
            MemberId member2Id = new MemberId(UUID.randomUUID());

            List<EventRegistration> registrations = List.of(
                    aRegistration(member1Id, SiCardNumber.of("1234")),
                    aRegistration(member2Id, SiCardNumber.of("5678"))
            );
            Event publishedEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .addRegistrations(registrations)
                    .build();
            publishedEvent.publish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(publishedEvent);
            when(membersMock.findByIds(any())).thenReturn(Map.of(
                    member1Id, new MemberDto(member1Id.value(), "John", "Doe", "john@example.com"),
                    member2Id, new MemberDto(member2Id.value(), "Jane", "Smith", "jane@example.com")
            ));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0]._links.recordTransaction.href")
                            .value(containsString("/api/members/" + member1Id.value() + "/account")))
                    .andExpect(jsonPath("$._embedded.registrationDtoList[1]._links.recordTransaction.href")
                            .value(containsString("/api/members/" + member2Id.value() + "/account")));
        }

        @Test
        @DisplayName("1.2 user WITHOUT FINANCE:MANAGE does not see recordTransaction link on registration rows")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void shouldNotExposeRecordTransactionLinkWithoutFinanceManage() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId member1Id = new MemberId(UUID.randomUUID());

            List<EventRegistration> registrations = List.of(
                    aRegistration(member1Id, SiCardNumber.of("1234"))
            );
            Event publishedEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .addRegistrations(registrations)
                    .build();
            publishedEvent.publish();

            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(publishedEvent);
            when(membersMock.findByIds(any())).thenReturn(Map.of(
                    member1Id, new MemberDto(member1Id.value(), "John", "Doe", "john@example.com")
            ));

            mockMvc.perform(
                            get("/api/events/{eventId}/registrations", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.registrationDtoList[0]._links.recordTransaction").doesNotExist());
        }
    }

    @Nested
    @DisplayName("SI card number and rental request")
    class SiPolicyTests {

        @Test
        @DisplayName("POST without siCardNumber and with wantsToRentSi passes both to the service")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void registerPassesRentalWithoutSiNumber() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(post("/api/events/{eventId}/registrations", eventId)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content("{\"wantsToRentSi\":true}"))
                    .andExpect(status().isCreated());

            ArgumentCaptor<Event.RegisterCommand> captor = ArgumentCaptor.forClass(Event.RegisterCommand.class);
            verify(registrationServiceMock).registerMember(eq(new EventId(eventId)), any(MemberId.class), captor.capture());
            assertThat(captor.getValue().siCardNumber()).isNull();
            assertThat(captor.getValue().wantsToRentSi()).isTrue();
        }

        @Test
        @DisplayName("POST returns the domain's 'required' error for REQUIRED without number or rental")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void registerReturnsRequiredError() throws Exception {
            doThrow(new com.klabis.common.exceptions.BusinessRuleViolationException("SI card number is required") {})
                    .when(registrationServiceMock)
                    .registerMember(any(), any(), any());

            mockMvc.perform(post("/api/events/{eventId}/registrations", UUID.randomUUID())
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail", containsString("SI card number is required")));
        }

        @Test
        @DisplayName("POST rejects an SI card number outside 4-8 digits")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void registerRejectsMalformedSiNumber() throws Exception {
            mockMvc.perform(post("/api/events/{eventId}/registrations", UUID.randomUUID())
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content("{\"siCardNumber\":\"123\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PUT without siCardNumber and with wantsToRentSi passes both to the service")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void editPassesRentalWithoutSiNumber() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(put("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content("{\"wantsToRentSi\":true}"))
                    .andExpect(status().isNoContent());

            ArgumentCaptor<Event.EditRegistrationCommand> captor = ArgumentCaptor.forClass(Event.EditRegistrationCommand.class);
            verify(registrationServiceMock).editRegistration(eq(new EventId(eventId)),
                    eq(new MemberId(UUID.fromString(MEMBER_1_ID))), captor.capture());
            assertThat(captor.getValue().siCardNumber()).isNull();
            assertThat(captor.getValue().wantsToRentSi()).isTrue();
        }

        @Test
        @DisplayName("PUT returns the domain's 'required' error when SI values are cleared")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void editReturnsRequiredError() throws Exception {
            doThrow(new com.klabis.common.exceptions.BusinessRuleViolationException("SI card number is required") {})
                    .when(registrationServiceMock)
                    .editRegistration(any(), any(), any());

            mockMvc.perform(put("/api/events/{eventId}/registrations/{memberId}", UUID.randomUUID(), MEMBER_1_ID)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail", containsString("SI card number is required")));
        }

        @Test
        @DisplayName("GET registration without SI number shows the rental request and both templates carry optional SI properties")
        @WithKlabisMockUser(memberId = MEMBER_1_ID)
        void getRegistrationWithoutSiNumber() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.fromString(MEMBER_1_ID));
            EventRegistration registration = EventRegistration.reconstruct(UUID.randomUUID(), Instant.now(),
                    EventRegistrationCreateEventRegistrationBuilder.builder()
                            .memberId(memberId).wantsToRentSi(true).build());
            Event event = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(30))
                    .addRegistration(registration)
                    .build();
            event.publish();
            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(membersMock.findById(memberId)).thenReturn(Optional.of(
                    new MemberDto(memberId.value(), "John", "Doe", "john@example.com")));

            mockMvc.perform(get("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.siCardNumber").doesNotExist())
                    .andExpect(jsonPath("$.wantsToRentSi").value(true))
                    .andExpect(jsonPath("$._templates.editRegistration.properties[?(@.name=='siCardNumber')]").exists())
                    .andExpect(jsonPath("$._templates.editRegistration.properties[?(@.name=='siCardNumber')].required").isEmpty())
                    .andExpect(jsonPath("$._templates.editRegistration.properties[?(@.name=='wantsToRentSi')]").exists());
        }

        @Test
        @DisplayName("registration defaults template carries both SI properties with siCardNumber not required")
        @WithKlabisMockUser(memberId = MEMBER_1_ID, authorities = {Authority.EVENTS_READ})
        void registerTemplateCarriesSiProperties() throws Exception {
            UUID eventId = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.fromString(MEMBER_1_ID));
            Event event = EventTestDataBuilder.anEvent().withDate(LocalDate.now().plusDays(30)).build();
            event.publish();
            when(eventManagementServiceMock.getEvent(new EventId(eventId), false)).thenReturn(event);
            when(membersMock.findById(memberId)).thenReturn(Optional.of(
                    new MemberDto(memberId.value(), "John", "Doe", "john@example.com", null, null, "123456")));

            mockMvc.perform(get("/api/events/{eventId}/registrations/{memberId}", eventId, MEMBER_1_ID)
                            .param("newRegistration", "true")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.registerForEvent.properties[?(@.name=='siCardNumber')]").exists())
                    .andExpect(jsonPath("$._templates.registerForEvent.properties[?(@.name=='siCardNumber')].required").isEmpty())
                    .andExpect(jsonPath("$._templates.registerForEvent.properties[?(@.name=='wantsToRentSi')]").exists());
        }
    }

}
