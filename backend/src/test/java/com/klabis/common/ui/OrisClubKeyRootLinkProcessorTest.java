package com.klabis.common.ui;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.users.Authority;
import com.klabis.common.settings.OrisClubKeyPort;
import com.klabis.members.infrastructure.orissync.OrisClubKeyRootLinkProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Design.md D10: the club-key resource hangs off the API root via a postprocessor, mirroring
 * {@link RootAdminLinkProcessor} — only visible to a user holding SYNC:MANAGE. Lives alongside
 * {@link RootControllerTest} because {@link RootController} is package-private.
 */
@DisplayName("OrisClubKeyRootLinkProcessor")
@WebMvcTest(controllers = RootController.class)
@Import({HalFormsSupport.class, OrisClubKeyRootLinkProcessor.class})
@WithPostprocessors
class OrisClubKeyRootLinkProcessorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrisClubKeyPort orisClubKeyPort;

    @Test
    @WithKlabisMockUser(authorities = Authority.SYNC_MANAGE)
    @DisplayName("adds the club-key link for a user with SYNC:MANAGE")
    void addsClubKeyLinkForUserWithSyncManage() throws Exception {
        mockMvc.perform(get("/api").accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.orisClubKey.href", org.hamcrest.Matchers.endsWith("/api/oris/club-key")));
    }

    @Test
    @WithKlabisMockUser(authorities = Authority.MEMBERS_MANAGE)
    @DisplayName("omits the club-key link for a user without SYNC:MANAGE")
    void omitsClubKeyLinkWithoutSyncManage() throws Exception {
        mockMvc.perform(get("/api").accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.orisClubKey").doesNotExist());
    }
}
