package com.klabis.members.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.settings.OrisClubKeyManagementPort;
import com.klabis.common.users.Authority;
import com.klabis.members.MembersWebMvcTest;
import com.klabis.members.application.MemberDiscoveryPort;
import com.klabis.members.domain.MemberFilter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.MediaTypes;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("ORIS import disabled (no MemberDiscoveryPort bean, oris profile off)")
@MembersWebMvcTest
class MemberOrisImportDisabledApiTest extends MemberApiTestBase {

    @Autowired
    private OrisClubKeyManagementPort orisClubKeyManagementPort;

    @Test
    @DisplayName("POST /api/members/oris-import -> 404 even for SYNC:MANAGE holder")
    @WithKlabisMockUser(authorities = Authority.SYNC_MANAGE)
    void importEndpointNotFound() throws Exception {
        mockMvc.perform(post("/api/members/oris-import").accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("importFromOris affordance absent even with SYNC:MANAGE and club key held")
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_READ, Authority.SYNC_MANAGE})
    void affordanceAbsent() throws Exception {
        when(managementService.listMembers(any(MemberFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(orisClubKeyManagementPort.isSet()).thenReturn(true);

        mockMvc.perform(get("/api/members").accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.importFromOris").doesNotExist());
    }
}
