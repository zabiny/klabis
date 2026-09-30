package com.klabis.members.legalguardian.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.legalguardian.application.GuardianCandidate;
import com.klabis.members.legalguardian.application.GuardianCandidatesPort;
import com.klabis.members.legalguardian.application.GuardianKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("LegalGuardianOptionsController API tests")
@WebMvcTest(controllers = {LegalGuardianOptionsController.class})
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
@WithPostprocessors
class LegalGuardianOptionsControllerTest {

    private static final UUID MEMBER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GUARDIAN_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GuardianCandidatesPort guardianCandidatesService;

    @Test
    @DisplayName("returns candidates as value/prompt options")
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
    void returnsCandidates() throws Exception {
        when(guardianCandidatesService.findCandidates("nov", null)).thenReturn(List.of(
                new GuardianCandidate(new UserId(MEMBER_UUID), "Jan Novák", GuardianKind.MEMBER, "ZBM0101", "jan@example.com"),
                new GuardianCandidate(new UserId(GUARDIAN_UUID), "Petr Novotný", GuardianKind.LEGAL_GUARDIAN, null, "petr@example.com")));

        mockMvc.perform(get("/api/legal-guardian-options").param("q", "nov").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value(MEMBER_UUID.toString()))
                .andExpect(jsonPath("$[0].prompt").value("Jan Novák"))
                .andExpect(jsonPath("$[0].kind").value("MEMBER"))
                .andExpect(jsonPath("$[0].registrationNumber").value("ZBM0101"))
                .andExpect(jsonPath("$[1].value").value(GUARDIAN_UUID.toString()))
                .andExpect(jsonPath("$[1].kind").value("LEGAL_GUARDIAN"))
                .andExpect(jsonPath("$[1].email").value("petr@example.com"));
    }

    @Test
    @DisplayName("passes the kind filter to the candidates query")
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
    void passesKindFilter() throws Exception {
        when(guardianCandidatesService.findCandidates(null, GuardianKind.LEGAL_GUARDIAN)).thenReturn(List.of(
                new GuardianCandidate(new UserId(GUARDIAN_UUID), "Petr Novotný", GuardianKind.LEGAL_GUARDIAN, null, "petr@example.com")));

        mockMvc.perform(get("/api/legal-guardian-options").param("kind", "LEGAL_GUARDIAN").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].kind").value("LEGAL_GUARDIAN"));
    }

    @Test
    @DisplayName("returns 403 without MEMBERS:MANAGE")
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_READ})
    void forbiddenWithoutManage() throws Exception {
        mockMvc.perform(get("/api/legal-guardian-options").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("returns 401 when unauthenticated")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/api/legal-guardian-options").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
