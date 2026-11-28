package com.klabis.members;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.users.Authority;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.legalguardian.application.LegalGuardianPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real {@code SelfRelationshipSource} and the legal guardian group grants through
 * {@link AuthorizationSnapshotLoader}: slice tests only emulate those grants.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestApplicationConfiguration.class, AccessTestFixture.class})
@CleanupTestData
@DisplayName("Access through self- and guardian grants - full-stack integration")
class SelfAccessIntegrationTest {

    private static final String PROFILE_UPDATE = "{\"email\":\"eva@example.com\",\"phone\":\"+420777888999\"}";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccessTestFixture fixture;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private AccessTestFixture.ExtGuardian extGuardian;
    private Member minor;

    @BeforeEach
    void setUp() {
        extGuardian = fixture.extGuardian("ext");
        minor = fixture.minor("Dítě", LegalGuardianPort.GuardianInput.existing(extGuardian.id()));
    }

    private RequestPostProcessor extGuardianToken() {
        return fixture.tokenFor(extGuardian, Authority.MEMBERS_READ);
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
    @DisplayName("non-member guardian patching a member under their own user id gets 404")
    void guardianPatchingOwnIdAsMemberGetsNotFound() throws Exception {
        mockMvc.perform(patch("/api/members/{id}", extGuardian.id().uuid()).with(extGuardianToken())
                        .contentType("application/json").content("{\"phone\": \"+420777123456\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("non-member guardian opens the child's detail with all data and the edit action, and a saved change shows on the detail")
    void guardianOpensAndEditsChild() throws Exception {
        UUID childId = minor.getId().uuid();

        mockMvc.perform(get("/api/members/{id}", childId).with(extGuardianToken()).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.birthNumber").value("900101/1234"))
                .andExpect(jsonPath("$.dateOfBirth").exists())
                .andExpect(jsonPath("$._links.legalGuardians.href").exists())
                .andExpect(jsonPath("$._templates.updateMember.method").value("PATCH"))
                .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='birthNumber')].readOnly")
                        .value(true))
                .andExpect(jsonPath("$._templates.suspendMember").doesNotExist());

        mockMvc.perform(patch("/api/members/{id}", childId).with(extGuardianToken())
                        .contentType("application/json").content("{\"phone\": \"+420777123456\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/members/{id}", childId).with(extGuardianToken()).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+420777123456"));
        assertThat(memberRepository.findById(minor.getId()).orElseThrow().getPhone().value())
                .isEqualTo("+420777123456");
    }

    @Test
    @DisplayName("guardian's view of the child's birth number is audited")
    void guardianViewOfBirthNumberIsAudited() throws Exception {
        mockMvc.perform(get("/api/members/{id}", minor.getId().uuid()).with(extGuardianToken())
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk());

        await().untilAsserted(() -> assertThat(jdbcTemplate.queryForList(
                "SELECT action FROM members.birth_number_audit_log WHERE user_id = ? AND member_id = ?",
                extGuardian.id().uuid(), minor.getId().uuid())).extracting(row -> row.get("action"))
                .contains("VIEW_BIRTH_NUMBER"));
    }
}
