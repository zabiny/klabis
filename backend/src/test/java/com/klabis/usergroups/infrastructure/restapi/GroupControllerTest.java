package com.klabis.usergroups.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.UserService;
import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.application.GroupManagementPort;
import com.klabis.usergroups.application.GroupNotFoundException;
import com.klabis.usergroups.application.NotGroupOwnerException;
import com.klabis.usergroups.domain.FreeGroup;
import com.klabis.usergroups.domain.GroupMembership;
import com.klabis.usergroups.domain.UserGroup;
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


import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("GroupController API tests")
@WebMvcTest(controllers = {GroupController.class, GroupsRootPostprocessor.class})
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
class GroupControllerTest {

    private static final String MEMBER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String OTHER_MEMBER_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final UUID GROUP_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UserGroupId GROUP_ID = new UserGroupId(GROUP_UUID);

    @Autowired
    private MockMvc mockMvc;


    @MockitoBean
    private GroupManagementPort groupManagementService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UserGroup buildGroup(UUID groupUuid, String name, String ownerUuidStr) {
        MemberId owner = new MemberId(UUID.fromString(ownerUuidStr));
        return FreeGroup.reconstruct(new UserGroupId(groupUuid), name, Set.of(owner), Set.of(), null);
    }

    @Nested
    @DisplayName("POST /api/groups")
    class CreateGroupTests {

        @Test
        @DisplayName("should return 201 with Location header when member creates group")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldCreateGroupAndReturn201() throws Exception {
            UserGroup created = buildGroup(GROUP_UUID, "Trail Runners", MEMBER_ID);
            when(groupManagementService.createFreeGroup(any(FreeGroup.CreateFreeGroup.class))).thenReturn(created);

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
            UserGroup group = buildGroup(GROUP_UUID, "Trail Runners", MEMBER_ID);
            when(groupManagementService.listGroupsForMember(any(MemberId.class))).thenReturn(List.of(group));

            mockMvc.perform(
                            get("/api/groups")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.groupSummaryResponseList").isArray());
        }

        @Test
        @DisplayName("should return 200 with empty collection when member has no groups")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturnEmptyCollectionWhenNoGroups() throws Exception {
            when(groupManagementService.listGroupsForMember(any(MemberId.class))).thenReturn(List.of());

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
        @DisplayName("should return 200 with group details")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturnGroupDetails() throws Exception {
            UserGroup group = buildGroup(GROUP_UUID, "Sprint Team", MEMBER_ID);
            when(groupManagementService.getGroup(any(UserGroupId.class))).thenReturn(group);

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
            when(groupManagementService.getGroup(any(UserGroupId.class)))
                    .thenThrow(new GroupNotFoundException(GROUP_ID));

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
            UserGroup renamedGroup = buildGroup(GROUP_UUID, "New Name", MEMBER_ID);
            when(groupManagementService.renameGroup(any(UserGroupId.class), any(String.class), any(MemberId.class)))
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
        @DisplayName("should return 400 when non-owner tries to rename group")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldReturn400WhenNonOwnerRenamesGroup() throws Exception {
            MemberId nonOwner = new MemberId(UUID.fromString(OTHER_MEMBER_ID));
            when(groupManagementService.renameGroup(any(UserGroupId.class), any(String.class), eq(nonOwner)))
                    .thenThrow(new NotGroupOwnerException(nonOwner, GROUP_ID));

            mockMvc.perform(
                            patch("/api/groups/{id}", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "New Name"}
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
            when(groupManagementService.renameGroup(any(UserGroupId.class), any(String.class), any(MemberId.class)))
                    .thenThrow(new GroupNotFoundException(GROUP_ID));

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
        @DisplayName("should return 400 when non-owner tries to delete group")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldReturn400WhenNonOwnerDeletesGroup() throws Exception {
            MemberId nonOwner = new MemberId(UUID.fromString(OTHER_MEMBER_ID));
            doThrow(new NotGroupOwnerException(nonOwner, GROUP_ID))
                    .when(groupManagementService).deleteGroup(any(UserGroupId.class), eq(nonOwner));

            mockMvc.perform(
                            delete("/api/groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isBadRequest());
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
            doThrow(new GroupNotFoundException(GROUP_ID))
                    .when(groupManagementService).deleteGroup(any(UserGroupId.class), any(MemberId.class));

            mockMvc.perform(
                            delete("/api/groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/groups/{id}/members")
    class AddMemberTests {

        @Test
        @DisplayName("should return 204 when owner adds member")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldAddMemberAndReturn204() throws Exception {
            UUID newMemberUuid = UUID.fromString(OTHER_MEMBER_ID);
            when(groupManagementService.addMemberToGroup(any(UserGroupId.class), any(MemberId.class), any(MemberId.class)))
                    .thenReturn(buildGroup(GROUP_UUID, "Sprint Team", MEMBER_ID));

            mockMvc.perform(
                            post("/api/groups/{id}/members", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(newMemberUuid))
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 400 when memberId is null")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn400WhenMemberIdIsNull() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{id}/members", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {}
                                            """)
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when non-owner tries to add member")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldReturn400WhenNonOwnerAddsMember() throws Exception {
            MemberId nonOwner = new MemberId(UUID.fromString(OTHER_MEMBER_ID));
            when(groupManagementService.addMemberToGroup(any(UserGroupId.class), any(MemberId.class), eq(nonOwner)))
                    .thenThrow(new NotGroupOwnerException(nonOwner, GROUP_ID));

            mockMvc.perform(
                            post("/api/groups/{id}/members", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(UUID.randomUUID()))
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when user has no member profile")
        @WithKlabisMockUser
        void shouldReturn403WhenNoMemberProfile() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{id}/members", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(UUID.randomUUID()))
                    )
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/groups/{id}/members/{memberId}")
    class RemoveMemberTests {

        @Test
        @DisplayName("should return 204 when owner removes member")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldRemoveMemberAndReturn204() throws Exception {
            UUID memberToRemove = UUID.fromString(OTHER_MEMBER_ID);
            when(groupManagementService.removeMemberFromGroup(any(UserGroupId.class), any(MemberId.class), any(MemberId.class)))
                    .thenReturn(buildGroup(GROUP_UUID, "Sprint Team", MEMBER_ID));

            mockMvc.perform(
                            delete("/api/groups/{id}/members/{memberId}", GROUP_UUID, memberToRemove)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 400 when non-owner tries to remove member")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldReturn400WhenNonOwnerRemovesMember() throws Exception {
            MemberId nonOwner = new MemberId(UUID.fromString(OTHER_MEMBER_ID));
            UUID memberToRemove = UUID.randomUUID();
            when(groupManagementService.removeMemberFromGroup(any(UserGroupId.class), any(MemberId.class), eq(nonOwner)))
                    .thenThrow(new NotGroupOwnerException(nonOwner, GROUP_ID));

            mockMvc.perform(
                            delete("/api/groups/{id}/members/{memberId}", GROUP_UUID, memberToRemove)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when domain rejects member removal (e.g. business rule violated)")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn400WhenDomainRejectsMemberRemoval() throws Exception {
            UUID targetMember = UUID.randomUUID();
            MemberId requestingMember = new MemberId(UUID.fromString(MEMBER_ID));
            when(groupManagementService.removeMemberFromGroup(any(UserGroupId.class), any(MemberId.class), any(MemberId.class)))
                    .thenThrow(new NotGroupOwnerException(requestingMember, GROUP_ID));

            mockMvc.perform(
                            delete("/api/groups/{id}/members/{memberId}", GROUP_UUID, targetMember)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isBadRequest());
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
            when(groupManagementService.removeMemberFromGroup(any(UserGroupId.class), any(MemberId.class), any(MemberId.class)))
                    .thenThrow(new GroupNotFoundException(GROUP_ID));

            mockMvc.perform(
                            delete("/api/groups/{id}/members/{memberId}", GROUP_UUID, UUID.randomUUID())
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNotFound());
        }
    }
}
