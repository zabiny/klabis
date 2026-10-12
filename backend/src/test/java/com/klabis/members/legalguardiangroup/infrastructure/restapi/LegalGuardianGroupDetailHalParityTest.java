package com.klabis.members.legalguardiangroup.infrastructure.restapi;

import com.klabis.common.HalShape;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.groups.domain.GroupMembership;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import com.klabis.members.MembersWebMvcTest;
import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Legal guardian group detail HAL parity (minors)")
@MembersWebMvcTest
class LegalGuardianGroupDetailHalParityTest {

    private static final String ADMIN_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String GUARDIAN_ID = "11111111-1111-1111-1111-111111111111";
    private static final String MINOR_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final UUID GROUP_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LegalGuardianGroupPort legalGuardianGroupService;

    @Autowired
    private Members members;

    private String getGroupBody() throws Exception {
        when(members.findByIds(anyCollection())).thenReturn(Map.of(new MemberId(UUID.fromString(MINOR_ID)),
                new MemberDto(UUID.fromString(MINOR_ID), "Tomáš", "Novák", null, "ZBM1501", LocalDateTime.now())));
        when(legalGuardianGroupService.getGroup(new LegalGuardianGroupId(GROUP_UUID)))
                .thenReturn(LegalGuardianGroup.reconstruct(new LegalGuardianGroupId(GROUP_UUID), "Novák",
                        Set.of(new UserId(UUID.fromString(GUARDIAN_ID))),
                        Set.of(GroupMembership.of(new UserId(UUID.fromString(MINOR_ID)))), null));
        return mockMvc.perform(get("/api/legal-guardian-groups/{id}", GROUP_UUID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("admin with MEMBERS:READ: minor rows carry only the member link")
    @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE, Authority.MEMBERS_READ})
    void minorRowsWithMembersRead() throws Exception {
        String body = getGroupBody();

        assertThat(HalShape.of(body, "$.minors[0]")).isEqualTo(List.of("link member -> /api/members/" + MINOR_ID));
    }

    @Test
    @DisplayName("admin without MEMBERS:READ: minor rows carry no links")
    @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
    void minorRowsWithoutMembersRead() throws Exception {
        String body = getGroupBody();

        assertThat(HalShape.of(body, "$.minors[0]")).isEmpty();
    }
}
