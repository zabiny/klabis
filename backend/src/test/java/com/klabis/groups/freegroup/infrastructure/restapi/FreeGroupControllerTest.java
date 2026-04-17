package com.klabis.groups.freegroup.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.usergroup.CannotPromoteNonMemberToOwnerException;
import com.klabis.common.usergroup.CannotRemoveLastOwnerException;
import com.klabis.common.usergroup.InvitationId;
import com.klabis.common.usergroup.InvitationStatus;
import com.klabis.common.usergroup.NotInvitedMemberException;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.members.MemberId;
import com.klabis.common.usergroup.GroupNotFoundException;
import com.klabis.groups.freegroup.application.FreeGroupManagementPort;
import com.klabis.groups.freegroup.application.PendingInvitationView;
import com.klabis.groups.freegroup.domain.GroupOwnershipRequiredException;
import com.klabis.groups.freegroup.domain.FreeGroup;
import com.klabis.groups.freegroup.FreeGroupId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("FreeGroupController API tests")
@WebMvcTest(controllers = {FreeGroupController.class, PendingInvitationsController.class, FreeGroupExceptionHandler.class})
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
class FreeGroupControllerTest {

    private static final String MEMBER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String OTHER_MEMBER_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final UUID GROUP_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID INVITATION_UUID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final FreeGroupId GROUP_ID = new FreeGroupId(GROUP_UUID);
    private static final InvitationId INVITATION_ID = new InvitationId(INVITATION_UUID);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FreeGroupManagementPort membersGroupManagementService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private FreeGroup buildGroup(UUID groupUuid, String name, String ownerUuidStr) {
        MemberId owner = new MemberId(UUID.fromString(ownerUuidStr));
        return FreeGroup.reconstruct(
                new FreeGroupId(groupUuid), name, Set.of(owner), Set.of(), Set.of(), null);
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
        @DisplayName("should return 403 when acting member is not owner")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldReturn403WhenNotOwner() throws Exception {
            MemberId notOwner = new MemberId(UUID.fromString(OTHER_MEMBER_ID));
            doThrow(new GroupOwnershipRequiredException(notOwner, GROUP_ID))
                    .when(membersGroupManagementService).removeMember(any(FreeGroupId.class), any(MemberId.class), any(MemberId.class));

            mockMvc.perform(
                            delete("/api/groups/{id}/members/{memberId}", GROUP_UUID, UUID.fromString(OTHER_MEMBER_ID))
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
            UserId nonMemberUserId = new UserId(java.util.UUID.fromString(OTHER_MEMBER_ID));
            doThrow(new CannotPromoteNonMemberToOwnerException(nonMemberUserId))
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
            UserId lastOwnerUserId = new UserId(UUID.fromString(MEMBER_ID));
            doThrow(new CannotRemoveLastOwnerException(lastOwnerUserId))
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
            doThrow(new NotInvitedMemberException(notInvited.toUserId(), INVITATION_ID))
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
            doThrow(new NotInvitedMemberException(notInvited.toUserId(), INVITATION_ID))
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
    @DisplayName("GET /api/invitations/pending")
    class GetPendingInvitationsTests {

        @Test
        @DisplayName("should return 200 with list of pending invitations")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldReturnPendingInvitations() throws Exception {
            MemberId owner = new MemberId(UUID.fromString(MEMBER_ID));
            MemberId invitedMember = new MemberId(UUID.fromString(OTHER_MEMBER_ID));
            com.klabis.common.usergroup.Invitation invitation = com.klabis.common.usergroup.Invitation.reconstruct(
                    INVITATION_ID, invitedMember.toUserId(), owner.toUserId(), InvitationStatus.PENDING, Instant.now());
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
