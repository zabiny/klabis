package com.klabis.members.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.settings.OrisClubKeyManagementPort;
import com.klabis.common.users.Authority;
import com.klabis.members.MembersWebMvcTest;
import com.klabis.members.application.ManagementPort;
import com.klabis.members.application.MemberDiscoveryPort;
import com.klabis.members.domain.MemberFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ORIS-enabled variant: {@link MemberDiscoveryPort} exists only under the {@code oris} profile, so the mock is
 * declared here (own Spring context) while the "disabled" behaviour is covered by
 * {@code MemberControllerApiTest.OrisImportDisabledTests} in the shared context.
 */
@MembersWebMvcTest
@MockitoBean(types = MemberDiscoveryPort.class)
class MemberOrisImportEnabledApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ManagementPort managementService;

    @Autowired
    private MemberDiscoveryPort memberDiscoveryPort;

    @Autowired
    private OrisClubKeyManagementPort orisClubKeyManagementPort;

    @Nested
    @DisplayName("POST /api/members/oris-import")
    class ImportFromOrisTests {

        @Test
        @DisplayName("SYNC:MANAGE holder -> runs the same discovery job, 204")
        @WithKlabisMockUser(authorities = Authority.SYNC_MANAGE)
        void runsTheSameDiscoveryJob() throws Exception {
            mockMvc.perform(post("/api/members/oris-import").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isNoContent());

            Mockito.verify(memberDiscoveryPort).discoverNewMembers();
        }

        @Test
        @DisplayName("running twice invokes the discovery job twice, not a reimplementation")
        @WithKlabisMockUser(authorities = Authority.SYNC_MANAGE)
        void runningTwiceInvokesTheJobTwice() throws Exception {
            mockMvc.perform(post("/api/members/oris-import").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isNoContent());
            mockMvc.perform(post("/api/members/oris-import").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isNoContent());

            Mockito.verify(memberDiscoveryPort, Mockito.times(2)).discoverNewMembers();
        }

        @Test
        @DisplayName("user without SYNC:MANAGE -> 403, job never invoked")
        @WithKlabisMockUser(authorities = Authority.MEMBERS_MANAGE)
        void withoutSyncManageAuthority_returns403() throws Exception {
            mockMvc.perform(post("/api/members/oris-import").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isForbidden());

            Mockito.verify(memberDiscoveryPort, Mockito.times(0)).discoverNewMembers();
        }
    }

    @Nested
    @DisplayName("importFromOris affordance on GET /api/members (design.md D11)")
    class ImportFromOrisAffordanceTests {

        private void givenEmptyMemberList() {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));
        }

        @Test
        @DisplayName("SYNC:MANAGE held and club key held -> affordance present")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_READ, Authority.SYNC_MANAGE})
        void syncManageAndKeyHeld_affordancePresent() throws Exception {
            givenEmptyMemberList();
            when(orisClubKeyManagementPort.isSet()).thenReturn(true);

            mockMvc.perform(get("/api/members").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.importFromOris").exists())
                    .andExpect(jsonPath("$._templates.importFromOris.method").value("POST"));
        }

        @Test
        @DisplayName("SYNC:MANAGE held but no club key held -> affordance absent")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_READ, Authority.SYNC_MANAGE})
        void syncManageHeldButNoKey_affordanceAbsent() throws Exception {
            givenEmptyMemberList();
            when(orisClubKeyManagementPort.isSet()).thenReturn(false);

            mockMvc.perform(get("/api/members").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.importFromOris").doesNotExist());
        }

        @Test
        @DisplayName("club key held but no SYNC:MANAGE -> affordance absent")
        @WithKlabisMockUser(authorities = Authority.MEMBERS_READ)
        void keyHeldButNoSyncManage_affordanceAbsent() throws Exception {
            givenEmptyMemberList();
            when(orisClubKeyManagementPort.isSet()).thenReturn(true);

            mockMvc.perform(get("/api/members").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.importFromOris").doesNotExist());
        }
    }
}
