package com.klabis.members.familygroup.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.usergroup.CannotRemoveLastOwnerException;
import com.klabis.common.usergroup.MemberAlreadyInGroupException;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.members.MemberId;
import com.klabis.members.familygroup.application.FamilyGroupManagementPort;
import com.klabis.members.familygroup.application.MemberAlreadyInFamilyGroupException;
import com.klabis.members.familygroup.domain.FamilyGroup;
import com.klabis.members.familygroup.domain.FamilyGroupId;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("FamilyGroupController API tests")
@WebMvcTest(controllers = {FamilyGroupController.class})
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
class FamilyGroupControllerTest {

    private static final String MEMBER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final UUID GROUP_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FamilyGroupManagementPort familyGroupManagementService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private FamilyGroup buildFamilyGroup(UUID groupUuid, String name, String ownerUuidStr) {
        MemberId owner = new MemberId(UUID.fromString(ownerUuidStr));
        return FamilyGroup.reconstruct(new FamilyGroupId(groupUuid), name, Set.of(owner), Set.of(), null);
    }

    @Nested
    @DisplayName("POST /api/family-groups")
    class CreateFamilyGroupTests {

        @Test
        @DisplayName("should return 400 when parentId is missing")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WhenParentIdIsMissing() throws Exception {
            mockMvc.perform(
                            post("/api/family-groups")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Novákovi"}
                                            """)
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 201 with Location and group has one parent no children after create")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldCreateFamilyGroupWithOneParentAndNoChildren() throws Exception {
            FamilyGroup created = buildFamilyGroup(GROUP_UUID, "Novákovi", MEMBER_ID);
            when(familyGroupManagementService.createFamilyGroup(any(FamilyGroup.CreateFamilyGroup.class)))
                    .thenReturn(created);

            mockMvc.perform(
                            post("/api/family-groups")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Novákovi", "parent": "%s"}
                                            """.formatted(MEMBER_ID))
                    )
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));
        }

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingMembersManageAuthority() throws Exception {
            mockMvc.perform(
                            post("/api/family-groups")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Novákovi", "parent": "%s"}
                                            """.formatted(MEMBER_ID))
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 201 with Location header when user has MEMBERS:MANAGE")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldCreateFamilyGroupAndReturn201() throws Exception {
            FamilyGroup created = buildFamilyGroup(GROUP_UUID, "Novákovi", MEMBER_ID);
            when(familyGroupManagementService.createFamilyGroup(any(FamilyGroup.CreateFamilyGroup.class)))
                    .thenReturn(created);

            mockMvc.perform(
                            post("/api/family-groups")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Novákovi", "parent": "%s"}
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
                                            {"name": "Novákovi", "parent": "%s"}
                                            """.formatted(MEMBER_ID))
                    )
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 400 when parent is already in a family group")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WhenMemberAlreadyInFamilyGroup() throws Exception {
            when(familyGroupManagementService.createFamilyGroup(any(FamilyGroup.CreateFamilyGroup.class)))
                    .thenThrow(new MemberAlreadyInFamilyGroupException(new MemberId(UUID.randomUUID())));

            mockMvc.perform(
                            post("/api/family-groups")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Novákovi", "parent": "%s"}
                                            """.formatted(MEMBER_ID))
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
            when(familyGroupManagementService.listFamilyGroups()).thenReturn(List.of(group));

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
        @DisplayName("should return 200 with group details including parents field when user has MEMBERS:MANAGE")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturnGroupDetailsWithParentsField() throws Exception {
            FamilyGroup group = buildFamilyGroup(GROUP_UUID, "Novákovi", MEMBER_ID);
            when(familyGroupManagementService.getFamilyGroup(any(FamilyGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/family-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Novákovi"))
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.parents").isArray())
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
    @DisplayName("POST /api/family-groups/{id}/parents")
    class AddFamilyGroupParentTests {

        @Test
        @DisplayName("should return 204 when admin adds a parent")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn204WhenAddingParent() throws Exception {
            mockMvc.perform(
                            post("/api/family-groups/{id}/parents", GROUP_UUID)
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
                            post("/api/family-groups/{id}/parents", GROUP_UUID)
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
    @DisplayName("DELETE /api/family-groups/{id}/parents/{memberId}")
    class RemoveFamilyGroupParentTests {

        @Test
        @DisplayName("should return 204 when admin removes a parent")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn204WhenRemovingParent() throws Exception {
            mockMvc.perform(
                            delete("/api/family-groups/{id}/parents/{memberId}", GROUP_UUID, UUID.fromString(MEMBER_ID))
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 422 when removing last parent")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn422WhenRemovingLastParent() throws Exception {
            UserId lastParentUserId = new UserId(UUID.fromString(MEMBER_ID));
            when(familyGroupManagementService.getFamilyGroup(any(FamilyGroupId.class)))
                    .thenReturn(buildFamilyGroup(GROUP_UUID, "Novákovi", MEMBER_ID));
            org.mockito.Mockito.doThrow(new CannotRemoveLastOwnerException(lastParentUserId))
                    .when(familyGroupManagementService).removeParent(any(FamilyGroupId.class), any(MemberId.class));

            mockMvc.perform(
                            delete("/api/family-groups/{id}/parents/{memberId}", GROUP_UUID, UUID.fromString(MEMBER_ID))
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().is(422));
        }

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(
                            delete("/api/family-groups/{id}/parents/{memberId}", GROUP_UUID, UUID.fromString(MEMBER_ID))
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/family-groups/{id}/children")
    class AddFamilyGroupChildTests {

        private static final String CHILD_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

        @Test
        @DisplayName("should return 204 when admin adds a child")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn204WhenAddingChild() throws Exception {
            mockMvc.perform(
                            post("/api/family-groups/{id}/children", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(CHILD_ID))
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 400 when child is already a parent of the same group (parent/child conflict)")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WhenChildIsAlreadyParent() throws Exception {
            doThrow(new MemberAlreadyInGroupException(new UserId(UUID.fromString(MEMBER_ID))))
                    .when(familyGroupManagementService).addChild(any(FamilyGroupId.class), any(MemberId.class));

            mockMvc.perform(
                            post("/api/family-groups/{id}/children", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(MEMBER_ID))
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(
                            post("/api/family-groups/{id}/children", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(CHILD_ID))
                    )
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/family-groups/{id}/children/{memberId}")
    class RemoveFamilyGroupChildTests {

        private static final String CHILD_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

        @Test
        @DisplayName("should return 204 when admin removes a child")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn204WhenRemovingChild() throws Exception {
            mockMvc.perform(
                            delete("/api/family-groups/{id}/children/{memberId}", GROUP_UUID, UUID.fromString(CHILD_ID))
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(
                            delete("/api/family-groups/{id}/children/{memberId}", GROUP_UUID, UUID.fromString(CHILD_ID))
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/family-groups/{id} — HAL-Forms affordances")
    class FamilyGroupDetailAffordancesTests {

        @Test
        @DisplayName("should include addParent and addChild affordances when user has MEMBERS:MANAGE")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldIncludeAddParentAndAddChildAffordancesWhenAuthorized() throws Exception {
            FamilyGroup group = buildFamilyGroup(GROUP_UUID, "Novákovi", MEMBER_ID);
            when(familyGroupManagementService.getFamilyGroup(any(FamilyGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/family-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.addFamilyGroupParent").exists())
                    .andExpect(jsonPath("$._templates.addFamilyGroupChild").exists());
        }

        @Test
        @DisplayName("should omit addParent and addChild affordances when user lacks MEMBERS:MANAGE")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldOmitAffordancesWhenNotAuthorized() throws Exception {
            mockMvc.perform(
                            get("/api/family-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }
    }
}
