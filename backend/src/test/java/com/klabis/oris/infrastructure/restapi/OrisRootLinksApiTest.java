package com.klabis.oris.infrastructure.restapi;

import com.dpolach.api.orisclient.OrisApiClient;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.oris.OrisWebMvcTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@OrisWebMvcTest
@ActiveProfiles({"oris", "test"})
@DisplayName("Links contributed by oris to the API root")
class OrisRootLinksApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrisApiClient orisApiClient;

    @Test
    @WithKlabisMockUser(authorities = Authority.SYNC_MANAGE)
    @DisplayName("adds the club-key link for a user with SYNC:MANAGE")
    void addsClubKeyLinkForUserWithSyncManage() throws Exception {
        mockMvc.perform(get("/api").accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.orisClubKey.href", endsWith("/api/oris/club-key")));
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
