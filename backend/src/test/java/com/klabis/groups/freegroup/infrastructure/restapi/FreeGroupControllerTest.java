package com.klabis.groups.freegroup.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.groups.domain.CannotRemoveLastOwnerException;
import com.klabis.common.groups.domain.GroupMembership;
import com.klabis.common.groups.domain.GroupNotFoundException;
import com.klabis.groups.GroupsWebMvcTest;
import com.klabis.groups.freegroup.FreeGroupId;
import com.klabis.groups.freegroup.application.FreeGroupManagementPort;
import com.klabis.groups.freegroup.application.PendingInvitationView;
import com.klabis.groups.freegroup.domain.*;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("FreeGroupController API tests")
@GroupsWebMvcTest
class FreeGroupControllerTest {

    private static final String MEMBER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String OTHER_MEMBER_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final UUID GROUP_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID INVITATION_UUID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final FreeGroupId GROUP_ID = new FreeGroupId(GROUP_UUID);
    private static final InvitationId INVITATION_ID = new InvitationId(INVITATION_UUID);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FreeGroupManagementPort membersGroupManagementService;

    private FreeGroup buildGroup(UUID groupUuid, String name, String ownerUuidStr) {
        MemberId owner = new MemberId(UUID.fromString(ownerUuidStr));
        return FreeGroup.reconstruct(
                new FreeGroupId(groupUuid), name, Set.of(owner), Set.of(), Set.of(), null);
    }

    private FreeGroup buildGroupWithMember(UUID groupUuid, String name, String ownerUuidStr, String memberUuidStr) {
        MemberId owner = new MemberId(UUID.fromString(ownerUuidStr));
        MemberId member = new MemberId(UUID.fromString(memberUuidStr));
        GroupMembership membership = GroupMembership.of(member);
        return FreeGroup.reconstruct(
                new FreeGroupId(groupUuid), name, Set.of(owner), Set.of(membership), Set.of(), null);
    }

    @Nested
    @DisplayName("POST /api/groups")
    class CreateGroupTests {

        @Test
        @DisplayName("should return 201 with Location header when member creates group")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldCreateGroupAndReturn201() throws Exception {
            FreeGroup created = buildGroup(GROUP_UUID, "Trail Runners", MEMBER_ID);
            when(membersGroupManagementService.createGroup(any(String.class), any(MemberId.class)))
                    .thenReturn(created);

            mockMvc.perform(
                            post("/api/groups")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Trail Runners"}
                                            """)
                    )
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));
        }

        @Test
        @DisplayName("should return 403 when user has no member profile")
        @WithKlabisMockUser
        void shouldReturn403WhenNoMemberProfile() throws Exception {
            mockMvc.perform(
                            post("/api/groups")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Trail Runners"}
                                            """)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn400WhenNameIsBlank() throws Exception {
            mockMvc.perform(
                            post("/api/groups")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": ""}
                                            """)
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(
                            post("/api/groups")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Trail Runners"}
                                            """)
                    )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/groups")
    class ListGroupsTests {

        @Test
        @DisplayName("should return 200 with list of groups for member")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturnGroupsForMember() throws Exception {
            FreeGroup group = buildGroup(GROUP_UUID, "Trail Runners", MEMBER_ID);
            when(membersGroupManagementService.listGroupsForMember(any(MemberId.class))).thenReturn(List.of(group));

            mockMvc.perform(
                            get("/api/groups")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 200 with empty collection when member has no groups")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturnEmptyCollectionWhenNoGroups() throws Exception {
            when(membersGroupManagementService.listGroupsForMember(any(MemberId.class))).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/groups")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 403 when user has no member profile")
        @WithKlabisMockUser
        void shouldReturn403WhenNoMemberProfile() throws Exception {
            mockMvc.perform(
                            get("/api/groups")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(
                            get("/api/groups")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/groups/{id}")
    class GetGroupTests {

        @Test
        @DisplayName("should return 200 with group details including owners and members")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturnGroupDetails() throws Exception {
            FreeGroup group = buildGroup(GROUP_UUID, "Sprint Team", MEMBER_ID);
            when(membersGroupManagementService.getGroup(any(FreeGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Sprint Team"))
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.owners").isArray())
                    .andExpect(jsonPath("$.members").isArray());
        }

        @Test
        @DisplayName("should return 200 when non-owner group member views group detail")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldReturnGroupDetailsForNonOwnerMember() throws Exception {
            FreeGroup group = buildGroupWithMember(GROUP_UUID, "Sprint Team", MEMBER_ID, OTHER_MEMBER_ID);
            when(membersGroupManagementService.getGroup(any(FreeGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Sprint Team"))
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.owners").isArray())
                    .andExpect(jsonPath("$.members").isArray());
        }

        @Test
        @DisplayName("should not list the owner among the members")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldNotListOwnerAmongMembers() throws Exception {
            MemberId owner = new MemberId(UUID.fromString(MEMBER_ID));
            MemberId member = new MemberId(UUID.fromString(OTHER_MEMBER_ID));
            FreeGroup group = FreeGroup.reconstruct(GROUP_ID, "Sprint Team", Set.of(owner),
                    Set.of(GroupMembership.of(member)), Set.of(), null);
            when(membersGroupManagementService.getGroup(any(FreeGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.owners.length()").value(1))
                    .andExpect(jsonPath("$.owners[0].memberId").value(MEMBER_ID))
                    .andExpect(jsonPath("$.members.length()").value(1))
                    .andExpect(jsonPath("$.members[0].memberId").value(OTHER_MEMBER_ID));
        }

        @Test
        @DisplayName("should return no members for a freshly created group — the creator is its owner only")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturnNoMembersForFreshlyCreatedGroup() throws Exception {
            MemberId creator = new MemberId(UUID.fromString(MEMBER_ID));
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Sprint Team", creator));
            when(membersGroupManagementService.getGroup(any(FreeGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.owners[0].memberId").value(MEMBER_ID))
                    .andExpect(jsonPath("$.members.length()").value(0));
        }

        @Test
        @DisplayName("should return owner-only affordances on self link when acting member is owner")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturnOwnerAffordancesForGroupOwner() throws Exception {
            FreeGroup group = buildGroup(GROUP_UUID, "Sprint Team", MEMBER_ID);
            when(membersGroupManagementService.getGroup(any(FreeGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.updateGroup.method").exists())
                    .andExpect(jsonPath("$._templates.deleteGroup.method").exists())
                    .andExpect(jsonPath("$._templates.addGroupOwner.method").exists())
                    .andExpect(jsonPath("$._templates.inviteMember.method").exists());
        }

        @Test
        @DisplayName("addGroupOwner and inviteMember templates should expose memberId property with an options.link pointing at the member options endpoint")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldExposeMemberIdOptionsLinkOnOwnerAndInviteTemplates() throws Exception {
            FreeGroup group = buildGroup(GROUP_UUID, "Sprint Team", MEMBER_ID);
            when(membersGroupManagementService.getGroup(any(FreeGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.addGroupOwner.properties[?(@.name=='memberId')].options.link.href")
                            .value("http://localhost/api/members/options"))
                    .andExpect(jsonPath("$._templates.addGroupOwner.properties[?(@.name=='memberId')].type")
                            .value("MemberId"))
                    .andExpect(jsonPath("$._templates.inviteMember.properties[?(@.name=='memberId')].options.link.href")
                            .value("http://localhost/api/members/options"))
                    .andExpect(jsonPath("$._templates.inviteMember.properties[?(@.name=='memberId')].type")
                            .value("MemberId"));
        }

        @Test
        @DisplayName("should NOT return owner-only affordances on self link when acting member is not owner")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldNotReturnOwnerAffordancesForNonOwnerMember() throws Exception {
            FreeGroup group = buildGroupWithMember(GROUP_UUID, "Sprint Team", MEMBER_ID, OTHER_MEMBER_ID);
            when(membersGroupManagementService.getGroup(any(FreeGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._templates.updateGroup").doesNotExist())
                    .andExpect(jsonPath("$._templates.deleteGroup").doesNotExist())
                    .andExpect(jsonPath("$._templates.addGroupOwner").doesNotExist())
                    .andExpect(jsonPath("$._templates.inviteMember").doesNotExist());
        }

        @Test
        @DisplayName("should expose a leaveGroup affordance targeting the caller's own memberId for a member")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldExposeLeaveGroupAffordanceForMember() throws Exception {
            FreeGroup group = buildGroupWithMember(GROUP_UUID, "Sprint Team", MEMBER_ID, OTHER_MEMBER_ID);
            when(membersGroupManagementService.getGroup(any(FreeGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.leaveGroup.method").value("DELETE"))
                    .andExpect(jsonPath("$._templates.leaveGroup.target")
                            .value("http://localhost/api/groups/" + GROUP_UUID + "/members/" + OTHER_MEMBER_ID));
        }

        @Test
        @DisplayName("should not expose a leaveGroup affordance to an owner")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldNotExposeLeaveGroupAffordanceForOwner() throws Exception {
            FreeGroup group = buildGroupWithMember(GROUP_UUID, "Sprint Team", MEMBER_ID, OTHER_MEMBER_ID);
            when(membersGroupManagementService.getGroup(any(FreeGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.leaveGroup").doesNotExist());
        }

        @Test
        @DisplayName("should return 403 when user is neither owner nor member of the group")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldReturn403WhenNotOwnerNorMember() throws Exception {
            FreeGroup group = buildGroup(GROUP_UUID, "Sprint Team", MEMBER_ID);
            when(membersGroupManagementService.getGroup(any(FreeGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 403 when user has no member profile")
        @WithKlabisMockUser
        void shouldReturn403WhenNoMemberProfile() throws Exception {
            mockMvc.perform(
                            get("/api/groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when group not found")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn404WhenGroupNotFound() throws Exception {
            when(membersGroupManagementService.getGroup(any(FreeGroupId.class)))
                    .thenThrow(new GroupNotFoundException("Members", GROUP_ID));

            mockMvc.perform(
                            get("/api/groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(
                            get("/api/groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should include cancel affordance on each pending invitation row when acting member is owner")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldIncludeCancelAffordanceOnPendingInvitationsForOwner() throws Exception {
            MemberId owner = new MemberId(UUID.fromString(MEMBER_ID));
            MemberId invitee = new MemberId(UUID.fromString(OTHER_MEMBER_ID));
            com.klabis.groups.freegroup.domain.Invitation invitation = com.klabis.groups.freegroup.domain.Invitation.reconstruct(
                    INVITATION_ID, invitee, owner, InvitationStatus.PENDING, Instant.now(), null, null, null);
            FreeGroup group = FreeGroup.reconstruct(
                    GROUP_ID, "Sprint Team", Set.of(owner), Set.of(), Set.of(invitation), null);
            when(membersGroupManagementService.getGroup(any(FreeGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pendingInvitations[0]._templates.cancelInvitation").exists());
        }

        @Test
        @DisplayName("should NOT include cancel affordance on pending invitations when acting member is not owner")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldNotIncludeCancelAffordanceForNonOwner() throws Exception {
            MemberId owner = new MemberId(UUID.fromString(MEMBER_ID));
            MemberId nonOwner = new MemberId(UUID.fromString(OTHER_MEMBER_ID));
            GroupMembership membership = GroupMembership.of(nonOwner);
            FreeGroup group = FreeGroup.reconstruct(
                    GROUP_ID, "Sprint Team", Set.of(owner), Set.of(membership), Set.of(), null);
            when(membersGroupManagementService.getGroup(any(FreeGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pendingInvitations").isArray())
                    .andExpect(jsonPath("$.pendingInvitations").isEmpty());
        }
    }

    @Nested
    @DisplayName("PATCH /api/groups/{id}")
    class RenameGroupTests {

        @Test
        @DisplayName("should return 204 when owner renames group")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldRenameGroupAndReturn204() throws Exception {
            FreeGroup renamedGroup = buildGroup(GROUP_UUID, "New Name", MEMBER_ID);
            when(membersGroupManagementService.renameGroup(any(FreeGroupId.class), any(String.class), any(MemberId.class)))
                    .thenReturn(renamedGroup);

            mockMvc.perform(
                            patch("/api/groups/{id}", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "New Name"}
                                            """)
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 403 when acting member is not owner")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldReturn403WhenNotOwner() throws Exception {
            MemberId notOwner = new MemberId(UUID.fromString(OTHER_MEMBER_ID));
            doThrow(new GroupOwnershipRequiredException(notOwner, GROUP_ID))
                    .when(membersGroupManagementService).renameGroup(any(FreeGroupId.class), any(String.class), any(MemberId.class));

            mockMvc.perform(
                            patch("/api/groups/{id}", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "New Name"}
                                            """)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn400WhenNameIsBlank() throws Exception {
            mockMvc.perform(
                            patch("/api/groups/{id}", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": ""}
                                            """)
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when user has no member profile")
        @WithKlabisMockUser
        void shouldReturn403WhenNoMemberProfile() throws Exception {
            mockMvc.perform(
                            patch("/api/groups/{id}", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "New Name"}
                                            """)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when group not found")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn404WhenGroupNotFound() throws Exception {
            doThrow(new GroupNotFoundException("Members", GROUP_ID))
                    .when(membersGroupManagementService).renameGroup(any(FreeGroupId.class), any(String.class), any(MemberId.class));

            mockMvc.perform(
                            patch("/api/groups/{id}", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "New Name"}
                                            """)
                    )
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/groups/{id}")
    class DeleteGroupTests {

        @Test
        @DisplayName("should return 204 when owner deletes group")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldDeleteGroupAndReturn204() throws Exception {
            mockMvc.perform(
                            delete("/api/groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 403 when acting member is not owner")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldReturn403WhenNotOwner() throws Exception {
            MemberId notOwner = new MemberId(UUID.fromString(OTHER_MEMBER_ID));
            doThrow(new GroupOwnershipRequiredException(notOwner, GROUP_ID))
                    .when(membersGroupManagementService).deleteGroup(any(FreeGroupId.class), any(MemberId.class));

            mockMvc.perform(
                            delete("/api/groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 403 when user has no member profile")
        @WithKlabisMockUser
        void shouldReturn403WhenNoMemberProfile() throws Exception {
            mockMvc.perform(
                            delete("/api/groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when group not found")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn404WhenGroupNotFound() throws Exception {
            doThrow(new GroupNotFoundException("Members", GROUP_ID))
                    .when(membersGroupManagementService).deleteGroup(any(FreeGroupId.class), any(MemberId.class));

            mockMvc.perform(
                            delete("/api/groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/groups/{id}/members (removed endpoint)")
    class AddMemberTests {

        @Test
        @DisplayName("should return 404 — endpoint was removed, membership is invitation-only")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn404BecauseEndpointWasRemoved() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{id}/members", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(UUID.randomUUID()))
                    )
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/groups/{id}/members/{memberId}")
    class RemoveMemberTests {

        @Test
        @DisplayName("should return 204 when owner removes member")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldRemoveMemberAndReturn204() throws Exception {
            mockMvc.perform(
                            delete("/api/groups/{id}/members/{memberId}", GROUP_UUID, UUID.fromString(OTHER_MEMBER_ID))
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should pass the caller through as both the target and the acting member when leaving")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldReturn204WhenMemberLeavesGroup() throws Exception {
            MemberId self = new MemberId(UUID.fromString(OTHER_MEMBER_ID));

            mockMvc.perform(
                            delete("/api/groups/{id}/members/{memberId}", GROUP_UUID, UUID.fromString(OTHER_MEMBER_ID))
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());
            verify(membersGroupManagementService).removeMember(GROUP_ID, self, self);
        }

        @Test
        @DisplayName("should return 403 when acting member is not owner")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldReturn403WhenNotOwner() throws Exception {
            MemberId notOwner = new MemberId(UUID.fromString(OTHER_MEMBER_ID));
            doThrow(new GroupOwnershipRequiredException(notOwner, GROUP_ID))
                    .when(membersGroupManagementService).removeMember(any(FreeGroupId.class), any(MemberId.class), any(MemberId.class));

            mockMvc.perform(
                            delete("/api/groups/{id}/members/{memberId}", GROUP_UUID, UUID.fromString(MEMBER_ID))
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 403 when user has no member profile")
        @WithKlabisMockUser
        void shouldReturn403WhenNoMemberProfile() throws Exception {
            mockMvc.perform(
                            delete("/api/groups/{id}/members/{memberId}", GROUP_UUID, UUID.randomUUID())
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when group not found")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn404WhenGroupNotFound() throws Exception {
            doThrow(new GroupNotFoundException("Members", GROUP_ID))
                    .when(membersGroupManagementService).removeMember(any(FreeGroupId.class), any(MemberId.class), any(MemberId.class));

            mockMvc.perform(
                            delete("/api/groups/{id}/members/{memberId}", GROUP_UUID, UUID.randomUUID())
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/groups/{id}/owners")
    class AddOwnerTests {

        @Test
        @DisplayName("should return 204 when owner adds another owner")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn204WhenAddingOwner() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{id}/owners", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(OTHER_MEMBER_ID))
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 403 when acting member is not owner")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldReturn403WhenNotOwner() throws Exception {
            MemberId notOwner = new MemberId(UUID.fromString(OTHER_MEMBER_ID));
            doThrow(new GroupOwnershipRequiredException(notOwner, GROUP_ID))
                    .when(membersGroupManagementService).addOwner(any(FreeGroupId.class), any(MemberId.class), any(MemberId.class));

            mockMvc.perform(
                            post("/api/groups/{id}/owners", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(OTHER_MEMBER_ID))
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 400 when body is missing memberId")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn400WhenMissingMemberId() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{id}/owners", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{}")
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when user has no member profile")
        @WithKlabisMockUser
        void shouldReturn403WhenNoMemberProfile() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{id}/owners", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(OTHER_MEMBER_ID))
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 409 when promoting a non-member to owner")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn409WhenPromotingNonMemberToOwner() throws Exception {
            MemberId nonMemberId = new MemberId(java.util.UUID.fromString(OTHER_MEMBER_ID));
            doThrow(new CannotPromoteNonMemberToOwnerException(nonMemberId))
                    .when(membersGroupManagementService).addOwner(any(FreeGroupId.class), any(MemberId.class), any(MemberId.class));

            mockMvc.perform(
                            post("/api/groups/{id}/owners", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(OTHER_MEMBER_ID))
                    )
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Cannot Promote Non-Member to Owner"));
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{id}/owners", GROUP_UUID)
                                    .contentType("application/json")
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(OTHER_MEMBER_ID))
                    )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /api/groups/{id}/owners/{memberId}")
    class RemoveOwnerTests {

        @Test
        @DisplayName("should return 204 when owner removes another owner")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn204WhenRemovingOwner() throws Exception {
            mockMvc.perform(
                            delete("/api/groups/{id}/owners/{memberId}", GROUP_UUID, UUID.fromString(OTHER_MEMBER_ID))
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 403 when acting member is not owner")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldReturn403WhenNotOwner() throws Exception {
            MemberId notOwner = new MemberId(UUID.fromString(OTHER_MEMBER_ID));
            doThrow(new GroupOwnershipRequiredException(notOwner, GROUP_ID))
                    .when(membersGroupManagementService).removeOwner(any(FreeGroupId.class), any(MemberId.class), any(MemberId.class));

            mockMvc.perform(
                            delete("/api/groups/{id}/owners/{memberId}", GROUP_UUID, UUID.fromString(OTHER_MEMBER_ID))
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 422 when attempting to remove last owner")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn422WhenRemovingLastOwner() throws Exception {
            MemberId lastOwnerMemberId = new MemberId(UUID.fromString(MEMBER_ID));
            doThrow(new CannotRemoveLastOwnerException(lastOwnerMemberId))
                    .when(membersGroupManagementService).removeOwner(any(FreeGroupId.class), any(MemberId.class), any(MemberId.class));

            mockMvc.perform(
                            delete("/api/groups/{id}/owners/{memberId}", GROUP_UUID, UUID.fromString(MEMBER_ID))
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().is(422));
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(
                            delete("/api/groups/{id}/owners/{memberId}", GROUP_UUID, UUID.randomUUID())
                    )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/groups/{id}/invitations")
    class InviteMemberTests {

        @Test
        @DisplayName("should return 204 when owner invites a member")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldInviteMemberAndReturn204() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{id}/invitations", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(OTHER_MEMBER_ID))
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 403 when acting member is not owner")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldReturn403WhenNotOwner() throws Exception {
            MemberId notOwner = new MemberId(UUID.fromString(OTHER_MEMBER_ID));
            doThrow(new GroupOwnershipRequiredException(notOwner, GROUP_ID))
                    .when(membersGroupManagementService).inviteMember(any(FreeGroupId.class), any(MemberId.class), any(MemberId.class));

            mockMvc.perform(
                            post("/api/groups/{id}/invitations", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(OTHER_MEMBER_ID))
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 400 when memberId is missing")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn400WhenMemberIdIsNull() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{id}/invitations", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{}")
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when user has no member profile")
        @WithKlabisMockUser
        void shouldReturn403WhenNoMemberProfile() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{id}/invitations", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(OTHER_MEMBER_ID))
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{id}/invitations", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(OTHER_MEMBER_ID))
                    )
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 404 when group not found")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn404WhenGroupNotFound() throws Exception {
            doThrow(new GroupNotFoundException("Members", GROUP_ID))
                    .when(membersGroupManagementService).inviteMember(any(FreeGroupId.class), any(MemberId.class), any(MemberId.class));

            mockMvc.perform(
                            post("/api/groups/{id}/invitations", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(OTHER_MEMBER_ID))
                    )
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/groups/{id}/invitations/{invitationId}/accept")
    class AcceptInvitationTests {

        @Test
        @DisplayName("should return 204 when invited member accepts invitation")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldAcceptInvitationAndReturn204() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{id}/invitations/{invitationId}/accept",
                                    GROUP_UUID, INVITATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 400 when non-invited member tries to accept")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn400WhenNonInvitedMemberAccepts() throws Exception {
            MemberId notInvited = new MemberId(UUID.fromString(MEMBER_ID));
            doThrow(new NotInvitedMemberException(notInvited, INVITATION_ID))
                    .when(membersGroupManagementService).acceptInvitation(any(), any(), eq(notInvited));

            mockMvc.perform(
                            post("/api/groups/{id}/invitations/{invitationId}/accept",
                                    GROUP_UUID, INVITATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when user has no member profile")
        @WithKlabisMockUser
        void shouldReturn403WhenNoMemberProfile() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{id}/invitations/{invitationId}/accept",
                                    GROUP_UUID, INVITATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{id}/invitations/{invitationId}/accept",
                                    GROUP_UUID, INVITATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/groups/{id}/invitations/{invitationId}/reject")
    class RejectInvitationTests {

        @Test
        @DisplayName("should return 204 when invited member rejects invitation")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldRejectInvitationAndReturn204() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{id}/invitations/{invitationId}/reject",
                                    GROUP_UUID, INVITATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 400 when non-invited member tries to reject")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn400WhenNonInvitedMemberRejects() throws Exception {
            MemberId notInvited = new MemberId(UUID.fromString(MEMBER_ID));
            doThrow(new NotInvitedMemberException(notInvited, INVITATION_ID))
                    .when(membersGroupManagementService).rejectInvitation(any(), any(), eq(notInvited));

            mockMvc.perform(
                            post("/api/groups/{id}/invitations/{invitationId}/reject",
                                    GROUP_UUID, INVITATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when user has no member profile")
        @WithKlabisMockUser
        void shouldReturn403WhenNoMemberProfile() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{id}/invitations/{invitationId}/reject",
                                    GROUP_UUID, INVITATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{id}/invitations/{invitationId}/reject",
                                    GROUP_UUID, INVITATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /api/groups/{id}/invitations/{invitationId}")
    class CancelInvitationTests {

        @Test
        @DisplayName("should return 204 when owner cancels invitation without reason body")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldCancelInvitationWithoutReasonAndReturn204() throws Exception {
            mockMvc.perform(
                            delete("/api/groups/{id}/invitations/{invitationId}", GROUP_UUID, INVITATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 204 when owner cancels invitation with reason body")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldCancelInvitationWithReasonAndReturn204() throws Exception {
            mockMvc.perform(
                            delete("/api/groups/{id}/invitations/{invitationId}", GROUP_UUID, INVITATION_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"reason": "No longer needed"}
                                            """)
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 409 when invitation is not in PENDING state")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn409WhenInvitationNotCancellable() throws Exception {
            doThrow(new InvitationNotCancellableException(INVITATION_ID, InvitationStatus.ACCEPTED))
                    .when(membersGroupManagementService).cancelInvitation(
                            any(FreeGroupId.class), any(InvitationId.class), any(MemberId.class), isNull());

            mockMvc.perform(
                            delete("/api/groups/{id}/invitations/{invitationId}", GROUP_UUID, INVITATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 403 when caller is not a current owner")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldReturn403WhenCallerIsNotOwner() throws Exception {
            MemberId notOwner = new MemberId(UUID.fromString(OTHER_MEMBER_ID));
            doThrow(new GroupOwnershipRequiredException(notOwner, GROUP_ID))
                    .when(membersGroupManagementService).cancelInvitation(
                            any(FreeGroupId.class), any(InvitationId.class), any(MemberId.class), any());

            mockMvc.perform(
                            delete("/api/groups/{id}/invitations/{invitationId}", GROUP_UUID, INVITATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 403 when user has no member profile")
        @WithKlabisMockUser
        void shouldReturn403WhenNoMemberProfile() throws Exception {
            mockMvc.perform(
                            delete("/api/groups/{id}/invitations/{invitationId}", GROUP_UUID, INVITATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(
                            delete("/api/groups/{id}/invitations/{invitationId}", GROUP_UUID, INVITATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/invitations/pending")
    class GetPendingInvitationsTests {

        @Test
        @DisplayName("should return 200 with list of pending invitations")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldReturnPendingInvitations() throws Exception {
            MemberId owner = new MemberId(UUID.fromString(MEMBER_ID));
            MemberId invitedMember = new MemberId(UUID.fromString(OTHER_MEMBER_ID));
            com.klabis.groups.freegroup.domain.Invitation invitation = com.klabis.groups.freegroup.domain.Invitation.reconstruct(
                    INVITATION_ID, invitedMember, owner, InvitationStatus.PENDING, Instant.now(), null, null, null);
            PendingInvitationView view = new PendingInvitationView(GROUP_ID, "Trail Runners", invitation);
            when(membersGroupManagementService.getPendingInvitationsForMember(any(MemberId.class)))
                    .thenReturn(List.of(view));

            mockMvc.perform(
                            get("/api/invitations/pending")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.pendingInvitationResponseList").isArray());
        }

        @Test
        @DisplayName("should return 200 with empty list when no pending invitations")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturnEmptyListWhenNoPendingInvitations() throws Exception {
            when(membersGroupManagementService.getPendingInvitationsForMember(any(MemberId.class)))
                    .thenReturn(List.of());

            mockMvc.perform(
                            get("/api/invitations/pending")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 403 when user has no member profile")
        @WithKlabisMockUser
        void shouldReturn403WhenNoMemberProfile() throws Exception {
            mockMvc.perform(
                            get("/api/invitations/pending")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(
                            get("/api/invitations/pending")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isUnauthorized());
        }
    }
}
