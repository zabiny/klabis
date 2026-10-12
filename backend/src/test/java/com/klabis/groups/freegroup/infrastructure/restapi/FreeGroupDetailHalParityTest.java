package com.klabis.groups.freegroup.infrastructure.restapi;

import com.klabis.common.HalShape;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.groups.domain.GroupMembership;
import com.klabis.common.users.Authority;
import com.klabis.groups.GroupsWebMvcTest;
import com.klabis.groups.freegroup.FreeGroupId;
import com.klabis.groups.freegroup.application.FreeGroupManagementPort;
import com.klabis.groups.freegroup.domain.FreeGroup;
import com.klabis.groups.freegroup.domain.Invitation;
import com.klabis.groups.freegroup.domain.InvitationId;
import com.klabis.groups.freegroup.domain.InvitationStatus;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Free group detail HAL parity (owners, members, pendingInvitations)")
@GroupsWebMvcTest
class FreeGroupDetailHalParityTest {

    private static final String OWNER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String SECOND_OWNER_ID = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee";
    private static final String MEMBER_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final String INVITEE_ID = "ffffffff-ffff-ffff-ffff-ffffffffffff";
    private static final UUID GROUP_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID INVITATION_UUID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    private static final String GROUP = "/api/groups/" + GROUP_UUID;
    private static final String INVITATION = GROUP + "/invitations/" + INVITATION_UUID;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FreeGroupManagementPort groupManagement;

    private static MemberId member(String id) {
        return new MemberId(UUID.fromString(id));
    }

    private FreeGroup groupWithTwoOwnersMemberAndInvitation() {
        Invitation invitation = Invitation.reconstruct(new InvitationId(INVITATION_UUID), member(INVITEE_ID),
                member(OWNER_ID), InvitationStatus.PENDING, Instant.now(), null, null, null);
        return FreeGroup.reconstruct(new FreeGroupId(GROUP_UUID), "Sprint Team",
                Set.of(member(OWNER_ID), member(SECOND_OWNER_ID)),
                Set.of(GroupMembership.of(member(MEMBER_ID))), Set.of(invitation), Set.of(), null);
    }

    private String getGroupBody() throws Exception {
        return getGroupBody(groupWithTwoOwnersMemberAndInvitation());
    }

    private String getGroupBody(FreeGroup group) throws Exception {
        when(groupManagement.getGroup(any(FreeGroupId.class))).thenReturn(group);
        return mockMvc.perform(get("/api/groups/{id}", GROUP_UUID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("owner sees a removal affordance on every owner while more than one owner exists")
    @WithKlabisMockUser(memberId = OWNER_ID, authorities = Authority.MEMBERS_READ)
    void ownersForOwner() throws Exception {
        String body = getGroupBody();

        assertThat(itemsByMember(body, "owners", SECOND_OWNER_ID)).isEqualTo(List.of("link member -> /api/members/" + SECOND_OWNER_ID, "link self -> " + GROUP + "/owners/" + SECOND_OWNER_ID, "template removeGroupOwner DELETE []"));
    }

    @Test
    @DisplayName("owner sees no removal affordance on the only owner")
    @WithKlabisMockUser(memberId = OWNER_ID, authorities = Authority.MEMBERS_READ)
    void singleOwnerHasNoRemoval() throws Exception {
        FreeGroup group = FreeGroup.reconstruct(new FreeGroupId(GROUP_UUID), "Sprint Team",
                Set.of(member(OWNER_ID)), Set.of(GroupMembership.of(member(MEMBER_ID))), Set.of(), Set.of(), null);

        String body = getGroupBody(group);

        assertThat(itemsByMember(body, "owners", OWNER_ID)).isEqualTo(List.of("link member -> /api/members/" + OWNER_ID));
    }

    @Test
    @DisplayName("owner sees a removal affordance on every member")
    @WithKlabisMockUser(memberId = OWNER_ID, authorities = Authority.MEMBERS_READ)
    void membersForOwner() throws Exception {
        String body = getGroupBody();

        assertThat(itemsByMember(body, "members", MEMBER_ID)).isEqualTo(List.of("link member -> /api/members/" + MEMBER_ID, "link self -> " + GROUP + "/members/" + MEMBER_ID, "template removeGroupMember DELETE []"));
    }

    @Test
    @DisplayName("owner sees accept, reject and cancel affordances on pending invitations")
    @WithKlabisMockUser(memberId = OWNER_ID, authorities = Authority.MEMBERS_READ)
    void pendingInvitationsForOwner() throws Exception {
        String body = getGroupBody();

        assertThat(HalShape.of(body, "$.pendingInvitations[0]")).isEqualTo(List.of("link accept -> " + INVITATION + "/accept", "link invitedMember -> /api/members/" + INVITEE_ID, "link reject -> " + INVITATION + "/reject", "link self -> " + INVITATION, "template acceptInvitation POST " + INVITATION + "/accept []", "template cancelInvitation DELETE []", "template rejectInvitation POST " + INVITATION + "/reject []"));
    }

    @Test
    @DisplayName("plain member sees only member links on owners and members and no pending invitations")
    @WithKlabisMockUser(memberId = MEMBER_ID, authorities = Authority.MEMBERS_READ)
    void itemsForPlainMember() throws Exception {
        String body = getGroupBody();

        assertThat(itemsByMember(body, "owners", SECOND_OWNER_ID)).isEqualTo(List.of("link member -> /api/members/" + SECOND_OWNER_ID));
        assertThat(itemsByMember(body, "members", MEMBER_ID)).isEqualTo(List.of("link member -> /api/members/" + MEMBER_ID));
        assertThat(com.jayway.jsonpath.JsonPath.<List<Object>>read(body, "$.pendingInvitations")).isEmpty();
    }

    @Test
    @DisplayName("without MEMBERS:READ the member links disappear but owner affordances stay")
    @WithKlabisMockUser(memberId = OWNER_ID)
    void ownerWithoutMembersRead() throws Exception {
        String body = getGroupBody();

        assertThat(itemsByMember(body, "owners", SECOND_OWNER_ID)).isEqualTo(List.of("link self -> " + GROUP + "/owners/" + SECOND_OWNER_ID, "template removeGroupOwner DELETE []"));
        assertThat(itemsByMember(body, "members", MEMBER_ID)).isEqualTo(List.of("link self -> " + GROUP + "/members/" + MEMBER_ID, "template removeGroupMember DELETE []"));
        assertThat(HalShape.of(body, "$.pendingInvitations[0]")).isEqualTo(List.of("link accept -> " + INVITATION + "/accept", "link reject -> " + INVITATION + "/reject", "link self -> " + INVITATION, "template acceptInvitation POST " + INVITATION + "/accept []", "template cancelInvitation DELETE []", "template rejectInvitation POST " + INVITATION + "/reject []"));
    }

    private static List<String> itemsByMember(String body, String property, String memberId) {
        int index = com.jayway.jsonpath.JsonPath.<List<String>>read(body, "$." + property + "[*].memberId")
                .indexOf(memberId);
        return HalShape.of(body, "$." + property + "[" + index + "]");
    }
}
