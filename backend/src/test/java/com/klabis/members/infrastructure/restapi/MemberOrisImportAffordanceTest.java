package com.klabis.members.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.members.application.ManagementPort;
import com.klabis.members.application.MemberDiscoveryPort;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.MemberFilter;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.infrastructure.orissync.MemberOrisImportAffordancePostprocessor;
import com.klabis.members.infrastructure.orissync.OrisClubKeyPort;
import com.klabis.sync.application.SynchronizationPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Design.md D11: the importFromOris affordance on GET /api/members is offered only to a caller
 * holding SYNC:MANAGE and only while a club key is held — without a key the action could not do
 * anything, so it must not be offered even to an authorized caller.
 */
@DisplayName("importFromOris affordance on GET /api/members")
@WebMvcTest(controllers = {MemberController.class, RegistrationController.class, MembersExceptionHandler.class})
@Import({MemberMapperImpl.class, HalFormsSupport.class, MemberOrisImportAffordancePostprocessor.class})
@WithPostprocessors
class MemberOrisImportAffordanceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ManagementPort managementService;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private RegistrationPort registrationService;

    @MockitoBean
    private OrisClubKeyPort orisClubKeyPort;

    @MockitoBean
    private MemberDiscoveryPort memberDiscoveryJob;

    @MockitoBean
    private SynchronizationPort synchronizationPort;

    @Test
    @DisplayName("SYNC:MANAGE held and club key held -> affordance present")
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_READ, Authority.SYNC_MANAGE})
    void syncManageAndKeyHeld_affordancePresent() throws Exception {
        when(memberRepository.findAll(any(MemberFilter.class), any())).thenReturn(new PageImpl<>(List.of()));
        when(orisClubKeyPort.isSet()).thenReturn(true);

        mockMvc.perform(get("/api/members").accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.importFromOris").exists())
                .andExpect(jsonPath("$._templates.importFromOris.method").value("POST"));
    }

    @Test
    @DisplayName("SYNC:MANAGE held but no club key held -> affordance absent")
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_READ, Authority.SYNC_MANAGE})
    void syncManageHeldButNoKey_affordanceAbsent() throws Exception {
        when(memberRepository.findAll(any(MemberFilter.class), any())).thenReturn(new PageImpl<>(List.of()));
        when(orisClubKeyPort.isSet()).thenReturn(false);

        mockMvc.perform(get("/api/members").accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.importFromOris").doesNotExist());
    }

    @Test
    @DisplayName("club key held but no SYNC:MANAGE -> affordance absent")
    @WithKlabisMockUser(authorities = Authority.MEMBERS_READ)
    void keyHeldButNoSyncManage_affordanceAbsent() throws Exception {
        when(memberRepository.findAll(any(MemberFilter.class), any())).thenReturn(new PageImpl<>(List.of()));
        when(orisClubKeyPort.isSet()).thenReturn(true);

        mockMvc.perform(get("/api/members").accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.importFromOris").doesNotExist());
    }
}
