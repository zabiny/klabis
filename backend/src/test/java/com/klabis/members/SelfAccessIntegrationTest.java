package com.klabis.members;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.authorization.AuthorizationSnapshotLoader;
import com.klabis.common.security.JwtParams;
import com.klabis.common.security.KlabisAuthenticationFactory;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.Address;
import com.klabis.members.domain.BirthNumber;
import com.klabis.members.domain.Country;
import com.klabis.members.domain.Gender;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.Nationality;
import com.klabis.members.domain.PersonalInformation;
import com.klabis.members.legalguardian.application.LegalGuardianPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real {@code SelfRelationshipSource} through {@link AuthorizationSnapshotLoader}: slice tests
 * only emulate its grants.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("Access to own profile through self-grants - full-stack integration")
class SelfAccessIntegrationTest {

    private static final String PROFILE_UPDATE = "{\"email\":\"eva@example.com\",\"phone\":\"+420777888999\"}";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RegistrationPort registrationPort;
    @Autowired
    private LegalGuardianPort legalGuardianPort;
    @Autowired
    private UserService userService;
    @Autowired
    private AuthorizationSnapshotLoader snapshotLoader;

    private UserId extGuardian;
    private String extLoginName;
    private UserId otherExtGuardian;
    private Member minor;

    @BeforeEach
    void setUp() {
        extGuardian = registerExtGuardian("ext");
        extLoginName = loginNameOf(extGuardian);
        otherExtGuardian = registerExtGuardian("other");
        minor = registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Dítě", "Nezletilý", LocalDate.now().minusYears(10), Nationality.of("CZ"),
                        Gender.MALE),
                Address.of("Dětská 1", "Brno", "60200", Country.of("CZ")),
                null, null, BirthNumber.of("900101/1234"), null, null,
                List.of(LegalGuardianPort.GuardianInput.existing(extGuardian)), null));
    }

    private UserId registerExtGuardian(String prefix) {
        var profile = legalGuardianPort.register(new LegalGuardianPort.NewLegalGuardian("Eva", "Externí",
                prefix + "." + UUID.randomUUID() + "@example.com", "+420777111444"));
        userService.activateWithPassword(profile.guardian().getId(), "{noop}password");
        return profile.guardian().getId();
    }

    private String loginNameOf(UserId guardian) {
        return legalGuardianPort.get(guardian).loginName();
    }

    private RequestPostProcessor tokenOf(JwtParams params, UserId userId) {
        return authentication(
                KlabisAuthenticationFactory.createAuthenticationToken(params, snapshotLoader.loadFor(userId)));
    }

    private RequestPostProcessor extGuardianToken() {
        return tokenOf(JwtParams.jwtTokenParams(extLoginName, extGuardian)
                .withAuthorities(Authority.MEMBERS_READ), extGuardian);
    }

    private RequestPostProcessor minorToken() {
        UserId userId = minor.getId().toUserId();
        return tokenOf(JwtParams.jwtTokenParams("ZBM9999", userId).withMemberId(minor.getId())
                .withAuthorities(Authority.MEMBERS_READ), userId);
    }

    @Test
    @DisplayName("non-member guardian reads their own guardian profile")
    void guardianReadsOwnProfile() throws Exception {
        mockMvc.perform(get("/api/legal-guardians/{id}", extGuardian.uuid()).with(extGuardianToken())
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(extGuardian.uuid().toString()))
                .andExpect(jsonPath("$._templates.updateLegalGuardian").exists());
    }

    @Test
    @DisplayName("non-member guardian updates their own guardian profile")
    void guardianUpdatesOwnProfile() throws Exception {
        mockMvc.perform(patch("/api/legal-guardians/{id}", extGuardian.uuid()).with(extGuardianToken())
                        .contentType("application/json").content(PROFILE_UPDATE))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("non-member guardian is denied another guardian's profile")
    void guardianDeniedOtherProfile() throws Exception {
        mockMvc.perform(get("/api/legal-guardians/{id}", otherExtGuardian.uuid()).with(extGuardianToken())
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/legal-guardians/{id}", otherExtGuardian.uuid()).with(extGuardianToken())
                        .contentType("application/json").content(PROFILE_UPDATE))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("non-member guardian patching a member under their own user id gets 404")
    void guardianPatchingOwnIdAsMemberGetsNotFound() throws Exception {
        mockMvc.perform(patch("/api/members/{id}", extGuardian.uuid()).with(extGuardianToken())
                        .contentType("application/json").content("{\"phone\": \"+420777123456\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("minor member sees all their own sensitive fields and gets no edit action")
    void minorSeesOwnDataWithoutEditAction() throws Exception {
        mockMvc.perform(get("/api/members/{id}", minor.getId().uuid()).with(minorToken())
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateOfBirth").exists())
                .andExpect(jsonPath("$.nationality").value("CZ"))
                .andExpect(jsonPath("$.gender").exists())
                .andExpect(jsonPath("$.birthNumber").value("900101/1234"))
                .andExpect(jsonPath("$._templates.updateMember").doesNotExist());
    }
}
