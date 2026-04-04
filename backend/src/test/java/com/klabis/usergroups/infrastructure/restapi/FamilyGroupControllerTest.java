package com.klabis.usergroups.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserService;
import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.application.GroupManagementPort;
import com.klabis.usergroups.application.MemberAlreadyInFamilyGroupException;
import com.klabis.usergroups.domain.FamilyGroup;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("FamilyGroupController API tests")
@WebMvcTest(controllers = {FamilyGroupController.class, GroupsExceptionHandler.class})
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
class FamilyGroupControllerTest {

    private static final String MEMBER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final UUID GROUP_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GroupManagementPort groupManagementService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private FamilyGroup buildFamilyGroup(UUID groupUuid, String name, String ownerUuidStr) {
        MemberId owner = new MemberId(UUID.fromString(ownerUuidStr));
        return FamilyGroup.reconstruct(new UserGroupId(groupUuid), name, Set.of(owner), Set.of(), null);
    }

    @Nested
    @DisplayName("POST /api/family-groups")
    class CreateFamilyGroupTests {

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingMembersManageAuthority() throws Exception {
            mockMvc.perform(
                            post("/api/family-groups")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Novákovi", "parentIds": ["%s"], "memberIds": []}
                                            """.formatted(MEMBER_ID))
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 201 with Location header when user has MEMBERS:MANAGE")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldCreateFamilyGroupAndReturn201() throws Exception {
            FamilyGroup created = buildFamilyGroup(GROUP_UUID, "Novákovi", MEMBER_ID);
            when(groupManagementService.createFamilyGroup(any(FamilyGroup.CreateFamilyGroup.class)))
                    .thenReturn(created);

            mockMvc.perform(
                            post("/api/family-groups")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Novákovi", "parentIds": ["%s"], "memberIds": []}
                                            """.formatted(MEMBER_ID))
                    )
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(
                            post("/api/family-groups")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Novákovi", "parentIds": ["%s"], "memberIds": []}
                                            """.formatted(MEMBER_ID))
                    )
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 400 when a member is already in a family group")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WhenMemberAlreadyInFamilyGroup() throws Exception {
            when(groupManagementService.createFamilyGroup(any(FamilyGroup.CreateFamilyGroup.class)))
                    .thenThrow(new MemberAlreadyInFamilyGroupException(new MemberId(UUID.randomUUID())));

            mockMvc.perform(
                            post("/api/family-groups")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Novákovi", "parentIds": ["%s"], "memberIds": ["%s"]}
                                            """.formatted(MEMBER_ID, UUID.randomUUID()))
                    )
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/family-groups")
    class ListFamilyGroupsTests {

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingMembersManageAuthority() throws Exception {
            mockMvc.perform(
                            get("/api/family-groups")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 200 with collection when user has MEMBERS:MANAGE")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturnCollectionWhenAuthorized() throws Exception {
            FamilyGroup group = buildFamilyGroup(GROUP_UUID, "Novákovi", MEMBER_ID);
            when(groupManagementService.listFamilyGroups()).thenReturn(List.of(group));

            mockMvc.perform(
                            get("/api/family-groups")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(
                            get("/api/family-groups")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/family-groups/{id}")
    class GetFamilyGroupTests {

        @Test
        @DisplayName("should return 200 with group details when user has MEMBERS:MANAGE")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturnGroupDetails() throws Exception {
            FamilyGroup group = buildFamilyGroup(GROUP_UUID, "Novákovi", MEMBER_ID);
            when(groupManagementService.getFamilyGroup(any(UserGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/family-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Novákovi"))
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.owners").isArray())
                    .andExpect(jsonPath("$.members").isArray());
        }

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingMembersManageAuthority() throws Exception {
            mockMvc.perform(
                            get("/api/family-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(
                            get("/api/family-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /api/family-groups/{id}")
    class DeleteFamilyGroupTests {

        @Test
        @DisplayName("should return 204 when user has MEMBERS:MANAGE")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldDeleteGroupAndReturn204() throws Exception {
            mockMvc.perform(
                            delete("/api/family-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingMembersManageAuthority() throws Exception {
            mockMvc.perform(
                            delete("/api/family-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(
                            delete("/api/family-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/family-groups/{id}/owners")
    class AddFamilyGroupOwnerTests {

        @Test
        @DisplayName("should return 204 when admin adds an owner")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn204WhenAddingOwner() throws Exception {
            FamilyGroup group = buildFamilyGroup(GROUP_UUID, "Novákovi", MEMBER_ID);
            when(groupManagementService.addOwnerToGroup(any(UserGroupId.class), any(MemberId.class), any(MemberId.class)))
                    .thenReturn(group);

            mockMvc.perform(
                            post("/api/family-groups/{id}/owners", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(UUID.randomUUID()))
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(
                            post("/api/family-groups/{id}/owners", GROUP_UUID)
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
    @DisplayName("DELETE /api/family-groups/{id}/owners/{memberId}")
    class RemoveFamilyGroupOwnerTests {

        @Test
        @DisplayName("should return 422 when removing last owner")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn422WhenRemovingLastOwner() throws Exception {
            MemberId lastOwner = new MemberId(UUID.fromString(MEMBER_ID));
            when(groupManagementService.removeOwnerFromGroup(any(UserGroupId.class), any(MemberId.class), any(MemberId.class)))
                    .thenThrow(new UserGroup.CannotRemoveLastOwnerException(lastOwner));

            mockMvc.perform(
                            delete("/api/family-groups/{id}/owners/{memberId}", GROUP_UUID, UUID.fromString(MEMBER_ID))
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().is(422));
        }

        @Test
        @DisplayName("should return 400 when requesting user is not an owner of the group")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WhenNonOwnerAttemptsRemoveOwner() throws Exception {
            UUID otherOwnerUuid = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
            when(groupManagementService.removeOwnerFromGroup(any(UserGroupId.class), any(MemberId.class), any(MemberId.class)))
                    .thenThrow(new com.klabis.usergroups.domain.NotGroupOwnerException(
                            new MemberId(UUID.fromString(MEMBER_ID)), new UserGroupId(GROUP_UUID)));

            mockMvc.perform(
                            delete("/api/family-groups/{id}/owners/{memberId}", GROUP_UUID, otherOwnerUuid)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isBadRequest());
        }
    }
}
