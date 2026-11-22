package com.klabis.events;

import com.klabis.TestApplicationConfiguration;
import com.klabis.CleanupTestData;
import com.klabis.common.authorization.AuthorizationSnapshotLoader;
import com.klabis.common.security.JwtParams;
import com.klabis.common.security.KlabisAuthenticationFactory;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.EventRegistrationPort;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventCreateEventBuilder;
import com.klabis.events.domain.EventRegisterCommandBuilder;
import com.klabis.events.domain.RegistrationDeadlines;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.Address;
import com.klabis.members.domain.BirthNumber;
import com.klabis.members.domain.Country;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.Gender;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.Nationality;
import com.klabis.members.domain.PersonalInformation;
import com.klabis.members.domain.PhoneNumber;
import com.klabis.members.legalguardian.application.LegalGuardianPort;
import com.klabis.members.legalguardian.application.LegalGuardianPort.GuardianInput;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("Legal guardian manages their minor's event registration - full-stack integration")
class GuardianEventRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RegistrationPort registrationPort;
    @Autowired
    private LegalGuardianPort legalGuardianPort;
    @Autowired
    private LegalGuardianGroupPort legalGuardianGroupPort;
    @Autowired
    private EventManagementPort eventManagement;
    @Autowired
    private EventRegistrationPort eventRegistration;
    @Autowired
    private UserService userService;
    @Autowired
    private AuthorizationSnapshotLoader snapshotLoader;

    private Member memberGuardian;
    private UserId extGuardian;
    private String extLoginName;
    private Member minor;
    private Member unrelatedMinor;
    private Event event;

    @BeforeEach
    void setUp() {
        memberGuardian = registerAdult();
        var extProfile = legalGuardianPort.register(
                new LegalGuardianPort.NewLegalGuardian("Eva", "Externí", unique("ext") + "@example.com",
                        "+420777111444"));
        extGuardian = extProfile.guardian().getId();
        extLoginName = extProfile.loginName();
        userService.activateWithPassword(extGuardian, "{noop}password");
        minor = registerMinor("Dítě", List.of(GuardianInput.existing(memberGuardian.getId().toUserId()),
                GuardianInput.existing(extGuardian)));
        unrelatedMinor = registerMinor("Cizí", List.of(GuardianInput.created(
                new LegalGuardianPort.NewLegalGuardian("Jiný", "Zástupce", unique("other") + "@example.com",
                        "+420777111333"))));

        event = eventManagement.createEvent(EventCreateEventBuilder.builder()
                .name("Guardian event")
                .eventDate(LocalDate.now().plusDays(30))
                .organizer("ZBM")
                .coordinators(new LinkedHashSet<>())
                .registrationDeadlines(RegistrationDeadlines.none())
                .categories(List.of())
                .sharedAccommodationEnabled(true)
                .sharedTransportEnabled(false)
                .build());
        eventManagement.publishEvent(event.getId());
        eventRegistration.registerMember(event.getId(), unrelatedMinor.getId(),
                EventRegisterCommandBuilder.builder().siCardNumber("111111").build());
    }

    private static String unique(String prefix) {
        return prefix + "." + UUID.randomUUID();
    }

    private Member registerAdult() {
        return registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Jan", "Rodič", LocalDate.of(1990, 1, 1), Nationality.of("CZ"), Gender.MALE),
                Address.of("Hlavní 1", "Praha", "11000", Country.of("CZ")),
                EmailAddress.of(unique("jan") + "@example.com"),
                PhoneNumber.of("+420777111555"),
                BirthNumber.of("900101/1234"),
                null, null, List.of(), null));
    }

    private Member registerMinor(String firstName, List<GuardianInput> guardians) {
        return registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of(firstName, "Nezletilý", LocalDate.now().minusYears(10), Nationality.of("SK"),
                        Gender.MALE),
                Address.of("Dětská 1", "Brno", "60200", Country.of("CZ")),
                null, null, null, null, null, guardians, null));
    }

    private RequestPostProcessor memberGuardianToken() {
        UserId userId = memberGuardian.getId().toUserId();
        return tokenOf(JwtParams.jwtTokenParams("ZBM8001", userId).withMemberId(memberGuardian.getId()), userId);
    }

    private RequestPostProcessor extGuardianToken() {
        return tokenOf(JwtParams.jwtTokenParams(extLoginName, extGuardian), extGuardian);
    }

    private RequestPostProcessor registrationsManagerToken() {
        return authentication(KlabisAuthenticationFactory.createAuthenticationToken(
                JwtParams.jwtTokenParams("MANAGER", new UserId(UUID.randomUUID()))
                        .withAuthorities(Authority.EVENTS_READ, Authority.EVENTS_REGISTER_MEMBER)));
    }

    private RequestPostProcessor tokenOf(JwtParams params, UserId userId) {
        return authentication(
                KlabisAuthenticationFactory.createAuthenticationToken(params, snapshotLoader.loadFor(userId)));
    }

    private static final String REGISTRATION_URL = "/api/events/{eventId}/registrations/{memberId}";

    private UUID eventId() {
        return event.getId().value();
    }

    private ResultActions getRegistration(Member of, RequestPostProcessor user, String query) throws Exception {
        return mockMvc.perform(get(REGISTRATION_URL + query, eventId(), of.getId().uuid())
                .with(user).accept(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    private ResultActions register(Member of, RequestPostProcessor user) throws Exception {
        return mockMvc.perform(post(REGISTRATION_URL, eventId(), of.getId().uuid())
                .with(user).contentType("application/json").content("{\"siCardNumber\":\"222222\"}"));
    }

    private ResultActions edit(Member of, RequestPostProcessor user) throws Exception {
        return mockMvc.perform(put(REGISTRATION_URL, eventId(), of.getId().uuid())
                .with(user).contentType("application/json").content("{\"siCardNumber\":\"333333\"}"));
    }

    private ResultActions unregister(Member of, RequestPostProcessor user) throws Exception {
        return mockMvc.perform(delete(REGISTRATION_URL, eventId(), of.getId().uuid()).with(user));
    }

    private ResultActions list(RequestPostProcessor user) throws Exception {
        return mockMvc.perform(get("/api/events/{eventId}/registrations", eventId())
                .with(user).accept(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    private ResultActions accommodationList(RequestPostProcessor user) throws Exception {
        return mockMvc.perform(get("/api/events/{eventId}/accommodation-list", eventId())
                .with(user).accept(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    private void assertManagesMinorRegistration(RequestPostProcessor user) throws Exception {
        getRegistration(minor, user, "?newRegistration=true").andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Dítě"))
                .andExpect(jsonPath("$._templates.registerForEvent").exists());

        register(minor, user).andExpect(status().isCreated());

        getRegistration(minor, user, "").andExpect(status().isOk())
                .andExpect(jsonPath("$.siCardNumber").value("222222"))
                .andExpect(jsonPath("$._templates.editRegistration").exists())
                .andExpect(jsonPath("$._templates.unregisterFromEvent").exists());

        edit(minor, user).andExpect(status().isNoContent());
        getRegistration(minor, user, "").andExpect(jsonPath("$.siCardNumber").value("333333"));

        list(user).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.registrationDtoList.length()").value(2))
                .andExpect(jsonPath("$._embedded.registrationDtoList[?(@.firstName=='Dítě')]._templates.editRegistration").exists())
                .andExpect(jsonPath("$._embedded.registrationDtoList[?(@.firstName=='Cizí')]._templates.editRegistration").doesNotExist())
                .andExpect(jsonPath("$._embedded.registrationDtoList[?(@.firstName=='Dítě')]._links.self").exists())
                .andExpect(jsonPath("$._embedded.registrationDtoList[?(@.firstName=='Cizí')]._links.self").doesNotExist())
                .andExpect(jsonPath("$._embedded.registrationDtoList[*].registrationTime").doesNotExist());

        unregister(minor, user).andExpect(status().isNoContent());
        getRegistration(minor, user, "").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("registrations manager sees the event-wide data the guardian does not")
    void managerSeesEventWideData() throws Exception {
        list(registrationsManagerToken()).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.registrationDtoList[*].registrationTime").isNotEmpty());
        accommodationList(registrationsManagerToken()).andExpect(status().isOk());
        mockMvc.perform(get("/api/events/{eventId}", eventId()).with(registrationsManagerToken())
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(jsonPath("$._links.accommodation-list").exists());
    }

    private void assertNoEventWideData(RequestPostProcessor user) throws Exception {
        accommodationList(user).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/events/{eventId}", eventId()).with(user).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.accommodation-list").doesNotExist());
    }

    private void assertDeniedForUnrelatedMinor(RequestPostProcessor user) throws Exception {
        getRegistration(unrelatedMinor, user, "").andExpect(status().isForbidden());
        getRegistration(unrelatedMinor, user, "?newRegistration=true").andExpect(status().isForbidden());
        edit(unrelatedMinor, user).andExpect(status().isForbidden());
        unregister(unrelatedMinor, user).andExpect(status().isForbidden());
        register(unrelatedMinor, user).andExpect(status().isForbidden());
    }

    @Nested
    @DisplayName("member guardian")
    class MemberGuardian {

        @Test
        @DisplayName("registers, gets, edits and unregisters their minor")
        void managesMinorRegistration() throws Exception {
            assertManagesMinorRegistration(memberGuardianToken());
        }

        @Test
        @DisplayName("sees no event-wide registration data")
        void noEventWideData() throws Exception {
            assertNoEventWideData(memberGuardianToken());
        }

        @Test
        @DisplayName("is denied for an unrelated minor")
        void deniedUnrelatedMinor() throws Exception {
            assertDeniedForUnrelatedMinor(memberGuardianToken());
        }

        @Test
        @DisplayName("loses access when the minor leaves the legal guardian group")
        void losesAccess() throws Exception {
            legalGuardianGroupPort.removeMinor(minor.getId());

            register(minor, memberGuardianToken()).andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("non-member (EXT) guardian")
    class ExtGuardian {

        @Test
        @DisplayName("registers, gets, edits and unregisters their minor")
        void managesMinorRegistration() throws Exception {
            assertManagesMinorRegistration(extGuardianToken());
        }

        @Test
        @DisplayName("sees no event-wide registration data")
        void noEventWideData() throws Exception {
            assertNoEventWideData(extGuardianToken());
        }

        @Test
        @DisplayName("is denied for an unrelated minor")
        void deniedUnrelatedMinor() throws Exception {
            assertDeniedForUnrelatedMinor(extGuardianToken());
        }

        @Test
        @DisplayName("loses access when the minor leaves the legal guardian group")
        void losesAccess() throws Exception {
            legalGuardianGroupPort.removeMinor(minor.getId());

            register(minor, extGuardianToken()).andExpect(status().isForbidden());
        }
    }
}
