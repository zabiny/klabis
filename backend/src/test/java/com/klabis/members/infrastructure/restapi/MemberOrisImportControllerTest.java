package com.klabis.members.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.members.application.ManagementPort;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.infrastructure.orissync.MemberDiscoveryJob;
import com.klabis.members.infrastructure.orissync.OrisClubKeyPort;
import com.klabis.sync.application.SynchronizationPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /api/members/oris-import runs the exact same discovery pass
 * {@link MemberDiscoveryJob}'s own cron runs (design.md D11, tasks.md section 9.3) — it must
 * invoke {@code discoverNewMembers()} rather than reimplementing the discovery algorithm.
 * Idempotency (nobody brought in twice) is guaranteed by that method's own
 * already-tested behaviour ({@code MemberDiscoveryJobTest}); this test only confirms the
 * controller is wired to call it, including on a second run.
 */
@DisplayName("POST /api/members/oris-import")
@WebMvcTest(controllers = {MemberController.class, RegistrationController.class, MembersExceptionHandler.class})
@Import({MemberMapperImpl.class, HalFormsSupport.class})
@WithPostprocessors
class MemberOrisImportControllerTest {

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
    private MemberDiscoveryJob memberDiscoveryJob;

    @MockitoBean
    private SynchronizationPort synchronizationPort;

    @Test
    @DisplayName("SYNC:MANAGE holder -> runs the same discovery job, 204")
    @WithKlabisMockUser(authorities = Authority.SYNC_MANAGE)
    void runsTheSameDiscoveryJob() throws Exception {
        mockMvc.perform(post("/api/members/oris-import").accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isNoContent());

        verify(memberDiscoveryJob).discoverNewMembers();
    }

    @Test
    @DisplayName("running twice invokes the discovery job twice, not a reimplementation")
    @WithKlabisMockUser(authorities = Authority.SYNC_MANAGE)
    void runningTwiceInvokesTheJobTwice() throws Exception {
        mockMvc.perform(post("/api/members/oris-import").accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/members/oris-import").accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isNoContent());

        verify(memberDiscoveryJob, times(2)).discoverNewMembers();
    }

    @Test
    @DisplayName("user without SYNC:MANAGE -> 403, job never invoked")
    @WithKlabisMockUser(authorities = Authority.MEMBERS_MANAGE)
    void withoutSyncManageAuthority_returns403() throws Exception {
        mockMvc.perform(post("/api/members/oris-import").accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isForbidden());

        verify(memberDiscoveryJob, times(0)).discoverNewMembers();
    }
}
