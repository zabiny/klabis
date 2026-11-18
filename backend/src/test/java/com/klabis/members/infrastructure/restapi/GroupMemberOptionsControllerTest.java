package com.klabis.members.infrastructure.restapi;

import com.klabis.common.TargetGrant;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.authorization.TargetType;
import com.klabis.common.groups.domain.GroupNotFoundException;
import com.klabis.common.users.Authority;
import com.klabis.members.MembersWebMvcTest;
import com.klabis.members.application.GroupMemberOptionsPort;
import com.klabis.members.application.GroupParticipantOption;
import com.klabis.members.application.GroupParticipantRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GroupMemberOptionsController API tests")
@MembersWebMvcTest
class GroupMemberOptionsControllerTest {

    private static final String GROUP = "33333333-3333-3333-3333-333333333333";
    private static final String OTHER_GROUP = "44444444-4444-4444-4444-444444444444";
    private static final UUID PERSON = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GroupMemberOptionsPort groupMemberOptions;

    @Test
    @DisplayName("manager reads the options as value/prompt pairs")
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
    void managerReadsOptions() throws Exception {
        when(groupMemberOptions.findOptions(UUID.fromString(GROUP), null))
                .thenReturn(List.of(new GroupParticipantOption(PERSON, "Jan Novák (ZBM0101)")));

        mockMvc.perform(get("/api/groups/{id}/member-options", GROUP).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].value").value(PERSON.toString()))
                .andExpect(jsonPath("$[0].prompt").value("Jan Novák (ZBM0101)"));
    }

    @Test
    @DisplayName("passes the role filter to the query")
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
    void passesRole() throws Exception {
        when(groupMemberOptions.findOptions(UUID.fromString(GROUP), GroupParticipantRole.MEMBER))
                .thenReturn(List.of(new GroupParticipantOption(PERSON, "Jan Novák (ZBM0101)")));

        mockMvc.perform(get("/api/groups/{id}/member-options", GROUP).param("role", "MEMBER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value(PERSON.toString()));
    }

    @Test
    @DisplayName("owner of the group reads its options")
    @WithKlabisMockUser(targetGrants = @TargetGrant(authority = Authority.GROUPS_OWNER, type = TargetType.GROUP, ids = GROUP))
    void ownerReadsOptions() throws Exception {
        when(groupMemberOptions.findOptions(UUID.fromString(GROUP), null)).thenReturn(List.of());

        mockMvc.perform(get("/api/groups/{id}/member-options", GROUP).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("owner of another group gets 403")
    @WithKlabisMockUser(targetGrants = @TargetGrant(authority = Authority.GROUPS_OWNER, type = TargetType.GROUP, ids = OTHER_GROUP))
    void ownerOfOtherGroupForbidden() throws Exception {
        mockMvc.perform(get("/api/groups/{id}/member-options", GROUP).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("plain member or outsider gets 403")
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_READ})
    void outsiderForbidden() throws Exception {
        mockMvc.perform(get("/api/groups/{id}/member-options", GROUP).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("unknown group id gets 404")
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
    void unknownGroup() throws Exception {
        when(groupMemberOptions.findOptions(UUID.fromString(GROUP), null))
                .thenThrow(new GroupNotFoundException("Group", GROUP));

        mockMvc.perform(get("/api/groups/{id}/member-options", GROUP).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
