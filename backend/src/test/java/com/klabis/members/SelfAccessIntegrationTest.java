package com.klabis.members;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.users.Authority;
import com.klabis.members.domain.Member;
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
@Import({TestApplicationConfiguration.class, AccessTestFixture.class})
@CleanupTestData
@DisplayName("Access to own profile through self-grants - full-stack integration")
class SelfAccessIntegrationTest {

    private static final String PROFILE_UPDATE = "{\"email\":\"eva@example.com\",\"phone\":\"+420777888999\"}";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccessTestFixture fixture;

    private AccessTestFixture.ExtGuardian extGuardian;
    private AccessTestFixture.ExtGuardian otherExtGuardian;
    private Member minor;

    @BeforeEach
    void setUp() {
        extGuardian = fixture.extGuardian("ext");
        otherExtGuardian = fixture.extGuardian("other");
        minor = fixture.minor("Dítě", LegalGuardianPort.GuardianInput.existing(extGuardian.id()));
    }

    private RequestPostProcessor extGuardianToken() {
        return fixture.tokenFor(extGuardian, Authority.MEMBERS_READ);
    }

    private RequestPostProcessor minorToken() {
        return fixture.tokenFor(minor, Authority.MEMBERS_READ);
    }

    @Test
    @DisplayName("non-member guardian reads their own guardian profile")
    void guardianReadsOwnProfile() throws Exception {
        mockMvc.perform(get("/api/legal-guardians/{id}", extGuardian.id().uuid()).with(extGuardianToken())
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(extGuardian.id().uuid().toString()))
                .andExpect(jsonPath("$._templates.updateLegalGuardian").exists());
    }

    @Test
    @DisplayName("non-member guardian updates their own guardian profile")
    void guardianUpdatesOwnProfile() throws Exception {
        mockMvc.perform(patch("/api/legal-guardians/{id}", extGuardian.id().uuid()).with(extGuardianToken())
                        .contentType("application/json").content(PROFILE_UPDATE))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("non-member guardian is denied another guardian's profile")
    void guardianDeniedOtherProfile() throws Exception {
        mockMvc.perform(get("/api/legal-guardians/{id}", otherExtGuardian.id().uuid()).with(extGuardianToken())
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/legal-guardians/{id}", otherExtGuardian.id().uuid()).with(extGuardianToken())
                        .contentType("application/json").content(PROFILE_UPDATE))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("non-member guardian patching a member under their own user id gets 404")
    void guardianPatchingOwnIdAsMemberGetsNotFound() throws Exception {
        mockMvc.perform(patch("/api/members/{id}", extGuardian.id().uuid()).with(extGuardianToken())
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
