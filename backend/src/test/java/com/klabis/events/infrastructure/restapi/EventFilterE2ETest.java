package com.klabis.events.infrastructure.restapi;

import com.klabis.E2ETest;
import com.klabis.common.security.JwtParams;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.events.application.EventManagementPort;
import com.klabis.members.familygroup.domain.FamilyGroupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static com.klabis.common.security.JwtParams.member;
import static com.klabis.common.security.KlabisMvcRequestBuilders.klabisAuthentication;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests verifying event list filter combinations.
 * <p>
 * Specifically tests the fulltext (q) and registeredBy filters in isolation and in
 * combination — these two are resolved via raw SQL (NamedParameterJdbcTemplate) instead
 * of Spring Data Criteria, so they need their own E2E coverage separate from the
 * DataJdbcTest-level {@code EventJdbcRepositoryTest} that runs in an isolated slice.
 * <p>
 * Also covers the cancelled-visibility rule: CANCELLED events reach the list only for
 * callers with EVENTS:MANAGE and for members registered for them.
 */
@E2ETest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/sql/test-members-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {"DELETE FROM calendar.calendar_items", "DELETE FROM events.events"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Event Filter E2E Tests")
class EventFilterE2ETest {

    @MockitoBean
    @SuppressWarnings("unused")
    private com.klabis.groups.traininggroup.domain.TrainingGroupRepository trainingGroupRepository;

    @MockitoBean
    @SuppressWarnings("unused")
    private FamilyGroupRepository familyGroupRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventManagementPort eventManagementPort;

    private static final UUID TEST_MEMBER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    @DisplayName("q=jihlava returns only events matching the fulltext token")
    void shouldFilterByFulltextQuery() throws Exception {
        // Given: two ACTIVE events — one matching "jihlava", one not
        createPublishedEvent("Jihlavský noční sprint", "Jihlava", LocalDate.now().plusDays(10));
        createPublishedEvent("Pražský pohár", "Praha", LocalDate.now().plusDays(11));

        // When: list events with q=jihlava
        mockMvc.perform(
                        get("/api/events")
                                .param("q", "jihlava")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(memberAuthentication())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0].name").value("Jihlavský noční sprint"));
    }

    @Test
    @DisplayName("registeredBy=me returns only events where the calling member is registered")
    void shouldFilterByRegisteredBy() throws Exception {
        // Given: two ACTIVE events, member registered only for one
        String registeredEvent = createPublishedEvent("Registered event", "Praha", LocalDate.now().plusDays(10));
        createPublishedEvent("Not registered event", "Brno", LocalDate.now().plusDays(11));

        registerMember(registeredEvent, TEST_MEMBER_ID);

        // When: list events with registeredBy=me
        mockMvc.perform(
                        get("/api/events")
                                .param("registeredBy", "me")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(memberAuthentication())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0].name").value("Registered event"));
    }

    @Test
    @DisplayName("q + registeredBy=me returns the intersection: only events matching both")
    void shouldApplyFulltextAndRegisteredByAsIntersection() throws Exception {
        // Given: three ACTIVE events
        // Event A: location "Jihlava", member IS registered → should be in result
        String jihlavRegistered = createPublishedEvent("Jihlavský pohár", "Jihlava", LocalDate.now().plusDays(10));
        // Event B: location "Jihlava", member is NOT registered → not in result
        createPublishedEvent("Jihlavský noční sprint", "Jihlava", LocalDate.now().plusDays(12));
        // Event C: location "Praha", member IS registered → not in result (fulltext mismatch)
        String prahaRegistered = createPublishedEvent("Pražský pohár", "Praha", LocalDate.now().plusDays(14));

        registerMember(jihlavRegistered, TEST_MEMBER_ID);
        registerMember(prahaRegistered, TEST_MEMBER_ID);

        // When: list events with both q=jihlava and registeredBy=me
        mockMvc.perform(
                        get("/api/events")
                                .param("q", "jihlava")
                                .param("registeredBy", "me")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(memberAuthentication())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0].name").value("Jihlavský pohár"));
    }

    @Test
    @DisplayName("q with no matches combined with registeredBy=me returns empty result")
    void shouldReturnEmptyWhenFulltextMatchesNothingCombinedWithRegisteredBy() throws Exception {
        // Given: one ACTIVE event the member is registered for
        String event = createPublishedEvent("Jihlavský pohár", "Jihlava", LocalDate.now().plusDays(10));
        registerMember(event, TEST_MEMBER_ID);

        // When: q matches nothing
        mockMvc.perform(
                        get("/api/events")
                                .param("q", "xyzxyzxyz_no_match")
                                .param("registeredBy", "me")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(memberAuthentication())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    // -------------------------------------------------------------------------
    // Cancelled event visibility
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("member without EVENTS:MANAGE sees no DRAFT and only the cancelled events they are registered for")
    void shouldShowCancelledEventsOnlyWhenRegistered() throws Exception {
        // Given: a DRAFT, an ACTIVE, a FINISHED and two CANCELLED events
        createEvent("Draft event", "Praha", LocalDate.now().plusDays(10));
        String active = createPublishedEvent("Active event", "Brno", LocalDate.now().plusDays(30));
        String finished = createPublishedEvent("Finished event", "Ostrava", LocalDate.now().plusDays(1));
        String cancelledRegistered = createPublishedEvent("Cancelled registered", "Plzen", LocalDate.now().plusDays(31));
        String cancelledUnregistered = createPublishedEvent("Cancelled unregistered", "Liberec", LocalDate.now().plusDays(32));

        registerMember(active, TEST_MEMBER_ID);
        registerMember(finished, TEST_MEMBER_ID);
        registerMember(cancelledRegistered, TEST_MEMBER_ID);

        eventManagementPort.finishExpiredActiveEvents(LocalDate.now().plusDays(2));
        cancelEvent(cancelledRegistered);
        cancelEvent(cancelledUnregistered);

        // When: the member lists all events
        mockMvc.perform(
                        get("/api/events")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(memberAuthentication())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$._embedded.eventSummaryDtoList[*].name")
                        .value(containsInAnyOrder("Active event", "Finished event", "Cancelled registered")))
                .andExpect(jsonPath("$._embedded.eventSummaryDtoList[*].name")
                        .value(not(hasItem("Draft event"))))
                .andExpect(jsonPath("$._embedded.eventSummaryDtoList[*].name")
                        .value(not(hasItem("Cancelled unregistered"))));
    }

    @Test
    @DisplayName("member without a member profile filtering status=CANCELLED gets an empty page")
    void shouldReturnEmptyPageForUserWithoutMemberProfileFilteringCancelled() throws Exception {
        // Given: one CANCELLED event
        String cancelled = createPublishedEvent("Cancelled event", "Plzen", LocalDate.now().plusDays(10));
        cancelEvent(cancelled);

        // When: a user with EVENTS:READ but no member profile filters by CANCELLED
        mockMvc.perform(
                        get("/api/events")
                                .param("status", "CANCELLED")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(userWithoutMemberProfileAuthentication())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    @DisplayName("member filtering status=CANCELLED sees only their own cancelled events")
    void shouldFilterCancelledByOwnRegistration() throws Exception {
        // Given: two CANCELLED events, the member registered for only one
        String mine = createPublishedEvent("My cancelled", "Plzen", LocalDate.now().plusDays(10));
        String someoneElses = createPublishedEvent("Someone else's cancelled", "Liberec", LocalDate.now().plusDays(11));
        registerMember(mine, TEST_MEMBER_ID);
        cancelEvent(mine);
        cancelEvent(someoneElses);

        // When
        mockMvc.perform(
                        get("/api/events")
                                .param("status", "CANCELLED")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(memberAuthentication())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0].name").value("My cancelled"));
    }

    @Test
    @DisplayName("member registered for no cancelled event filtering status=CANCELLED gets an empty page")
    void shouldReturnEmptyPageWhenMemberRegisteredForNoCancelledEvent() throws Exception {
        // Given: one CANCELLED event the member is not registered for
        String cancelled = createPublishedEvent("Cancelled event", "Plzen", LocalDate.now().plusDays(10));
        cancelEvent(cancelled);

        // When
        mockMvc.perform(
                        get("/api/events")
                                .param("status", "CANCELLED")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(memberAuthentication())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    @DisplayName("registeredBy=me still returns the member's cancelled events")
    void shouldReturnCancelledEventsWithRegisteredByMe() throws Exception {
        // Given: a cancelled event the member is registered for
        String cancelled = createPublishedEvent("Cancelled event", "Plzen", LocalDate.now().plusDays(10));
        registerMember(cancelled, TEST_MEMBER_ID);
        cancelEvent(cancelled);

        // When
        mockMvc.perform(
                        get("/api/events")
                                .param("registeredBy", "me")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(memberAuthentication())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0].name").value("Cancelled event"));
    }

    @Test
    @DisplayName("fulltext combined with cancelled visibility is ANDed — both must hold")
    void shouldAndFulltextWithCancelledVisibility() throws Exception {
        // Given: two cancelled events, only one matching the fulltext
        String matching = createPublishedEvent("Jihlavský pohár", "Jihlava", LocalDate.now().plusDays(10));
        String notMatching = createPublishedEvent("Pražský pohár", "Praha", LocalDate.now().plusDays(11));
        registerMember(matching, TEST_MEMBER_ID);
        registerMember(notMatching, TEST_MEMBER_ID);
        cancelEvent(matching);
        cancelEvent(notMatching);

        // When
        mockMvc.perform(
                        get("/api/events")
                                .param("q", "jihlava")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(memberAuthentication())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$._embedded.eventSummaryDtoList[0].name").value("Jihlavský pohár"));
    }

    @Test
    @DisplayName("fulltext and cancelled visibility both applied — a cancelled event not matching the fulltext stays hidden")
    void shouldHideCancelledEventNotMatchingFulltextEvenWhenRegistered() throws Exception {
        // Given: a cancelled event registered for, but not matching the fulltext
        String cancelled = createPublishedEvent("Pražský pohár", "Praha", LocalDate.now().plusDays(10));
        registerMember(cancelled, TEST_MEMBER_ID);
        cancelEvent(cancelled);

        // When
        mockMvc.perform(
                        get("/api/events")
                                .param("q", "jihlava")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(memberAuthentication())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    @DisplayName("caller with EVENTS:MANAGE sees all cancelled events regardless of registration")
    void shouldShowAllCancelledEventsToManager() throws Exception {
        // Given: two CANCELLED events, the member registered for only one
        String registered = createPublishedEvent("Cancelled registered", "Plzen", LocalDate.now().plusDays(10));
        String unregistered = createPublishedEvent("Cancelled unregistered", "Liberec", LocalDate.now().plusDays(11));
        registerMember(registered, TEST_MEMBER_ID);
        cancelEvent(registered);
        cancelEvent(unregistered);

        // When
        mockMvc.perform(
                        get("/api/events")
                                .param("status", "CANCELLED")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(adminAuthentication())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$._embedded.eventSummaryDtoList[*].name")
                        .value(containsInAnyOrder("Cancelled registered", "Cancelled unregistered")));
    }

    // -------------------------------------------------------------------------
    // Test helpers
    // -------------------------------------------------------------------------

    private RequestPostProcessor memberAuthentication() {
        return klabisAuthentication(
                member(TEST_MEMBER_ID).withAuthorities(Authority.EVENTS_READ)
        );
    }

    private RequestPostProcessor adminAuthentication() {
        return klabisAuthentication(
                JwtParams.jwtTokenParams("admin", UserId.fromString("14ad69f4-8fd0-4314-9cf2-d57e9e6f64a6"))
                        .withAuthorities(Authority.EVENTS_MANAGE, Authority.EVENTS_READ)
        );
    }

    /**
     * A user with EVENTS:READ but no member profile — a coordinator-style account
     * that is not a club member and therefore has no {@code memberId} claim.
     */
    private RequestPostProcessor userWithoutMemberProfileAuthentication() {
        return klabisAuthentication(
                JwtParams.jwtTokenParams("coordinator", UserId.fromString("7c3f9b21-4d18-4f7a-9c2e-5a6b1d8e3f40"))
                        .withAuthorities(Authority.EVENTS_READ)
        );
    }

    private String createEvent(String name, String location, LocalDate date) throws Exception {
        Map<String, Object> event = Map.of(
                "name", name,
                "eventDate", date.toString(),
                "location", location,
                "organizer", "TEST"
        );

        MvcResult createResult = mockMvc.perform(
                        post("/api/events")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(event))
                                .with(adminAuthentication())
                )
                .andExpect(status().isCreated())
                .andReturn();

        String location2 = createResult.getResponse().getHeader("Location");
        return location2.substring(location2.lastIndexOf('/') + 1);
    }

    private String createPublishedEvent(String name, String location, LocalDate date) throws Exception {
        String eventId = createEvent(name, location, date);

        mockMvc.perform(
                        post("/api/events/{id}/publish", eventId)
                                .with(adminAuthentication())
                )
                .andExpect(status().isNoContent());

        return eventId;
    }

    private void cancelEvent(String eventId) throws Exception {
        mockMvc.perform(
                        post("/api/events/{id}/cancel", eventId)
                                .contentType("application/json")
                                .content("{\"cancellationReason\":\"Zrušeno kvůli dešti\"}")
                                .with(adminAuthentication())
                )
                .andExpect(status().isNoContent());
    }

    private void registerMember(String eventId, UUID memberId) throws Exception {
        mockMvc.perform(
                        post("/api/events/{id}/registrations", eventId)
                                .contentType("application/json")
                                .content("{\"siCardNumber\":\"123456\"}")
                                .with(klabisAuthentication(member(memberId).withAuthorities(Authority.EVENTS_READ, Authority.EVENTS_REGISTRATIONS)))
                )
                .andExpect(status().isCreated());
    }
}
