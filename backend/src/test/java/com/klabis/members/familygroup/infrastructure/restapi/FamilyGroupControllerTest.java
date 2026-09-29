package com.klabis.members.familygroup.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.groups.domain.CannotRemoveLastOwnerException;
import com.klabis.common.groups.domain.GroupMembership;
import com.klabis.common.groups.domain.MemberAlreadyInGroupException;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.familygroup.FamilyGroupId;
import com.klabis.members.familygroup.application.FamilyGroupManagementPort;
import com.klabis.members.familygroup.application.MemberAlreadyInFamilyGroupException;
import com.klabis.members.familygroup.domain.FamilyGroup;
import com.klabis.members.infrastructure.restapi.FamilyGroupsApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("FamilyGroupController API tests")
@WebMvcTest(controllers = {FamilyGroupController.class})
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
@WithPostprocessors
class FamilyGroupControllerTest {

    private static final String MEMBER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String CHILD_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final String NON_MEMBER_USER_ID = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee";
    private static final UUID GROUP_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FamilyGroupManagementPort familyGroupManagementService;

    private FamilyGroup buildFamilyGroup(UUID groupUuid, String name, String ownerUuidStr) {
        UserId owner = new UserId(UUID.fromString(ownerUuidStr));
        return FamilyGroup.reconstruct(new FamilyGroupId(groupUuid), name, Set.of(owner), Set.of(), null);
    }

    private FamilyGroup buildFamilyGroupWithChild(UUID groupUuid, String name, String ownerUuidStr, String childUuidStr) {
        UserId owner = new UserId(UUID.fromString(ownerUuidStr));
        MemberId child = new MemberId(UUID.fromString(childUuidStr));
        GroupMembership<UserId> childMembership = GroupMembership.of(child.toUserId());
        return FamilyGroup.reconstruct(new FamilyGroupId(groupUuid), name, Set.of(owner), Set.of(childMembership), null);
    }

    // A parent is always also a member of the group, which is what lets a single
    // group.hasMember(currentUser.userId()) check cover both parents and children.
    private FamilyGroup buildFamilyGroupWithParent(UUID groupUuid, String name, String parentUuidStr) {
        UserId parent = new UserId(UUID.fromString(parentUuidStr));
        GroupMembership<UserId> parentMembership = GroupMembership.of(parent);
        return FamilyGroup.reconstruct(new FamilyGroupId(groupUuid), name, Set.of(parent), Set.of(parentMembership), null);
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

            ArgumentCaptor<FamilyGroup.CreateFamilyGroup> command = ArgumentCaptor.forClass(FamilyGroup.CreateFamilyGroup.class);
            verify(familyGroupManagementService).createFamilyGroup(command.capture());
            assertThat(command.getValue().name()).isEqualTo("Novákovi");
            assertThat(command.getValue().parent()).isEqualTo(new UserId(UUID.fromString(MEMBER_ID)));
        }

        @Test
        @DisplayName("should create family group when parent is a user without a member profile")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        void shouldCreateFamilyGroupWithParentThatIsNotAMember() throws Exception {
            UserId parentWithoutProfile = new UserId(UUID.fromString(NON_MEMBER_USER_ID));
            when(familyGroupManagementService.createFamilyGroup(any(FamilyGroup.CreateFamilyGroup.class)))
                    .thenReturn(buildFamilyGroup(GROUP_UUID, "Novákovi", NON_MEMBER_USER_ID));

            mockMvc.perform(
                            post("/api/family-groups")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Novákovi", "parent": "%s"}
                                            """.formatted(NON_MEMBER_USER_ID))
                    )
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/family-groups/" + GROUP_UUID)));

            ArgumentCaptor<FamilyGroup.CreateFamilyGroup> command = ArgumentCaptor.forClass(FamilyGroup.CreateFamilyGroup.class);
            verify(familyGroupManagementService).createFamilyGroup(command.capture());
            assertThat(command.getValue().parent()).isEqualTo(parentWithoutProfile);
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
        @DisplayName("should return 409 when the designated parent user already belongs to a family group")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn409WhenParentUserAlreadyInFamilyGroup() throws Exception {
            when(familyGroupManagementService.createFamilyGroup(any(FamilyGroup.CreateFamilyGroup.class)))
                    .thenThrow(new MemberAlreadyInFamilyGroupException(new UserId(UUID.fromString(MEMBER_ID))));

            mockMvc.perform(
                            post("/api/family-groups")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Novákovi", "parent": "%s"}
                                            """.formatted(MEMBER_ID))
                    )
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Member Already In Family Group"));
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
        @DisplayName("createFamilyGroup template should expose parent property with an options.link pointing at the member options endpoint")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldExposeParentOptionsLinkOnCreateTemplate() throws Exception {
            when(familyGroupManagementService.listFamilyGroups()).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/family-groups")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.createFamilyGroup.properties[?(@.name=='parent')].options.link.href")
                            .value("http://localhost/api/members/options"))
                    .andExpect(jsonPath("$._templates.createFamilyGroup.properties[?(@.name=='parent')].type")
                            .value("UserId"));
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

        private static final String PARENT_ID = "dddddddd-dddd-dddd-dddd-dddddddddddd";

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
        @DisplayName("should return 200 when user is a child member of the family group")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_READ})
        void shouldReturnGroupDetailsForFamilyGroupMember() throws Exception {
            FamilyGroup group = buildFamilyGroupWithChild(GROUP_UUID, "Novákovi", PARENT_ID, MEMBER_ID);
            when(familyGroupManagementService.getFamilyGroup(any(FamilyGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/family-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Novákovi"))
                    .andExpect(jsonPath("$.id").exists());
        }

        @Test
        @DisplayName("should return 403 when user is not a member of the family group and lacks MEMBERS:MANAGE")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_READ})
        void shouldReturn403WhenNonMemberLacksMembersManageAuthority() throws Exception {
            FamilyGroup group = buildFamilyGroup(GROUP_UUID, "Novákovi", PARENT_ID);
            when(familyGroupManagementService.getFamilyGroup(any(FamilyGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/family-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 200 when the caller is a parent without a member profile")
        @WithKlabisMockUser(userId = PARENT_ID, authorities = {Authority.MEMBERS_READ})
        void shouldReturnGroupDetailsForParentWithoutMemberProfile() throws Exception {
            FamilyGroup group = buildFamilyGroupWithParent(GROUP_UUID, "Novákovi", PARENT_ID);
            when(familyGroupManagementService.getFamilyGroup(any(FamilyGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/family-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Novákovi"));
        }

        @Test
        @DisplayName("should return 200 when the caller is a parent with a member profile")
        @WithKlabisMockUser(memberId = PARENT_ID, authorities = {Authority.MEMBERS_READ})
        void shouldReturnGroupDetailsForParentWithMemberProfile() throws Exception {
            FamilyGroup group = buildFamilyGroupWithParent(GROUP_UUID, "Novákovi", PARENT_ID);
            when(familyGroupManagementService.getFamilyGroup(any(FamilyGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/family-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Novákovi"));
        }

        @Test
        @DisplayName("should return 403 when a user without a member profile is not a parent of the group")
        @WithKlabisMockUser(userId = NON_MEMBER_USER_ID, authorities = {Authority.MEMBERS_READ})
        void shouldReturn403ForNonParticipantWithoutMemberProfile() throws Exception {
            FamilyGroup group = buildFamilyGroupWithParent(GROUP_UUID, "Novákovi", PARENT_ID);
            when(familyGroupManagementService.getFamilyGroup(any(FamilyGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/family-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 200 with MEMBERS:MANAGE for a user who is not a participant in the group")
        @WithKlabisMockUser(userId = NON_MEMBER_USER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn200ForManagerWithoutMemberProfileWhoIsNotAParticipant() throws Exception {
            FamilyGroup group = buildFamilyGroupWithParent(GROUP_UUID, "Novákovi", PARENT_ID);
            when(familyGroupManagementService.getFamilyGroup(any(FamilyGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/family-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Novákovi"));
        }

        @Test
        @DisplayName("should list a parent by userId with no member link, children keep memberId with a member link")
        @WithKlabisMockUser(userId = NON_MEMBER_USER_ID, authorities = {Authority.MEMBERS_MANAGE, Authority.MEMBERS_READ})
        void shouldListParentsByUserIdAndChildrenByMemberId() throws Exception {
            UserId parent = new UserId(UUID.fromString(PARENT_ID));
            MemberId child = new MemberId(UUID.fromString(MEMBER_ID));
            FamilyGroup group = FamilyGroup.reconstruct(new FamilyGroupId(GROUP_UUID), "Novákovi",
                    Set.of(parent), Set.of(GroupMembership.of(parent), GroupMembership.of(child.toUserId())), null);
            when(familyGroupManagementService.getFamilyGroup(any(FamilyGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/family-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.parents[0].userId").value(PARENT_ID))
                    .andExpect(jsonPath("$.parents[0].memberId").doesNotExist())
                    .andExpect(jsonPath("$.parents[0]._links.member").doesNotExist())
                    .andExpect(jsonPath("$.members[0].memberId").value(MEMBER_ID))
                    .andExpect(jsonPath("$.members[0]._links.member.href")
                            .value("http://localhost/api/members/" + MEMBER_ID));
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
        @DisplayName("should return 204 and pass the userId to the service when admin adds a parent")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn204WhenAddingParent() throws Exception {
            mockMvc.perform(
                            post("/api/family-groups/{id}/parents", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"userId": "%s"}
                                            """.formatted(MEMBER_ID))
                    )
                    .andExpect(status().isNoContent());

            verify(familyGroupManagementService)
                    .addParent(new FamilyGroupId(GROUP_UUID), new UserId(UUID.fromString(MEMBER_ID)));
        }

        @Test
        @DisplayName("should return 204 and pass the userId to the service when admin adds a parent without a member profile")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn204WhenAddingParentWithoutMemberProfile() throws Exception {
            mockMvc.perform(
                            post("/api/family-groups/{id}/parents", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"userId": "%s"}
                                            """.formatted(NON_MEMBER_USER_ID))
                    )
                    .andExpect(status().isNoContent());

            verify(familyGroupManagementService)
                    .addParent(new FamilyGroupId(GROUP_UUID), new UserId(UUID.fromString(NON_MEMBER_USER_ID)));
        }

        @Test
        @DisplayName("should pass the userId of an existing child to the service so it can be promoted in place")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldPassUserIdOfExistingChildWhenPromotingToParent() throws Exception {
            mockMvc.perform(
                            post("/api/family-groups/{id}/parents", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"userId": "%s"}
                                            """.formatted(CHILD_ID))
                    )
                    .andExpect(status().isNoContent());

            verify(familyGroupManagementService)
                    .addParent(new FamilyGroupId(GROUP_UUID), new UserId(UUID.fromString(CHILD_ID)));
        }

        @Test
        @DisplayName("should return 400 when userId is missing")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WhenUserIdIsMissing() throws Exception {
            mockMvc.perform(
                            post("/api/family-groups/{id}/parents", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{}")
                    )
                    .andExpect(status().isBadRequest());
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
                                            {"userId": "%s"}
                                            """.formatted(UUID.randomUUID()))
                    )
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/family-groups/{id}/parents/{userId}")
    class RemoveFamilyGroupParentTests {

        @Test
        @DisplayName("should expose the remove-parent path with a userId parameter")
        void shouldExposeRemoveParentPathWithUserIdParameter() {
            assertThat(FamilyGroupsApi.PATH_REMOVE_FAMILY_GROUP_PARENT)
                    .isEqualTo("/api/family-groups/{id}/parents/{userId}");
        }

        @Test
        @DisplayName("should return 204 and pass the path userId to the service when admin removes a parent")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn204WhenRemovingParent() throws Exception {
            mockMvc.perform(
                            delete("/api/family-groups/{id}/parents/{userId}", GROUP_UUID, UUID.fromString(MEMBER_ID))
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());

            verify(familyGroupManagementService)
                    .removeParent(new FamilyGroupId(GROUP_UUID), new UserId(UUID.fromString(MEMBER_ID)));
        }

        @Test
        @DisplayName("should return 204 when admin removes a parent that has no member profile")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn204WhenRemovingParentWithoutMemberProfile() throws Exception {
            mockMvc.perform(
                            delete("/api/family-groups/{id}/parents/{userId}", GROUP_UUID, UUID.fromString(NON_MEMBER_USER_ID))
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());

            verify(familyGroupManagementService)
                    .removeParent(new FamilyGroupId(GROUP_UUID), new UserId(UUID.fromString(NON_MEMBER_USER_ID)));
        }

        @Test
        @DisplayName("should return 422 when removing last parent")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn422WhenRemovingLastParent() throws Exception {
            MemberId lastParentMemberId = new MemberId(UUID.fromString(MEMBER_ID));
            doThrow(new CannotRemoveLastOwnerException(lastParentMemberId))
                    .when(familyGroupManagementService).removeParent(any(FamilyGroupId.class), any(UserId.class));

            mockMvc.perform(
                            delete("/api/family-groups/{id}/parents/{userId}", GROUP_UUID, UUID.fromString(MEMBER_ID))
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().is(422));
        }

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(
                            delete("/api/family-groups/{id}/parents/{userId}", GROUP_UUID, UUID.fromString(MEMBER_ID))
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/family-groups/{id}/children")
    class AddFamilyGroupChildTests {

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
            doThrow(new MemberAlreadyInGroupException(new MemberId(UUID.fromString(MEMBER_ID))))
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
                    .andExpect(jsonPath("$._templates.deleteFamilyGroup").exists())
                    .andExpect(jsonPath("$._templates.addFamilyGroupParent").exists())
                    .andExpect(jsonPath("$._templates.addFamilyGroupChild").exists());
        }

        @Test
        @DisplayName("addFamilyGroupParent template should expose userId property with an options.link pointing at the member options endpoint, addFamilyGroupChild should keep memberId")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldExposeUserIdOptionsLinkOnParentTemplateAndMemberIdOnChildTemplate() throws Exception {
            FamilyGroup group = buildFamilyGroup(GROUP_UUID, "Novákovi", MEMBER_ID);
            when(familyGroupManagementService.getFamilyGroup(any(FamilyGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/family-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.addFamilyGroupParent.properties[?(@.name=='userId')].options.link.href")
                            .value("http://localhost/api/members/options"))
                    .andExpect(jsonPath("$._templates.addFamilyGroupParent.properties[?(@.name=='userId')].type")
                            .value("UserId"))
                    .andExpect(jsonPath("$._templates.addFamilyGroupParent.properties[?(@.name=='memberId')]")
                            .doesNotExist())
                    .andExpect(jsonPath("$._templates.addFamilyGroupChild.properties[?(@.name=='memberId')].options.link.href")
                            .value("http://localhost/api/members/options"))
                    .andExpect(jsonPath("$._templates.addFamilyGroupChild.properties[?(@.name=='memberId')].type")
                            .value("MemberId"))
                    .andExpect(jsonPath("$._templates.addFamilyGroupChild.properties[?(@.name=='userId')]")
                            .doesNotExist());
        }

        @Test
        @DisplayName("parent rows should expose a self link and a removeFamilyGroupParent affordance, including a parent without a member profile")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldExposeRemoveParentAffordanceOnUserIdPath() throws Exception {
            FamilyGroup group = FamilyGroup.reconstruct(new FamilyGroupId(GROUP_UUID), "Novákovi",
                    Set.of(new UserId(UUID.fromString(MEMBER_ID)), new UserId(UUID.fromString(NON_MEMBER_USER_ID))),
                    Set.of(), null);
            when(familyGroupManagementService.getFamilyGroup(any(FamilyGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/family-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.parents[*]._templates.removeFamilyGroupParent.method").value(hasItems("DELETE")))
                    .andExpect(jsonPath("$.parents[*]._links.self.href").value(hasItems(
                            "http://localhost/api/family-groups/" + GROUP_UUID + "/parents/" + MEMBER_ID,
                            "http://localhost/api/family-groups/" + GROUP_UUID + "/parents/" + NON_MEMBER_USER_ID)));
        }

        @Test
        @DisplayName("should omit delete, addParent and addChild affordances when user is group member but lacks MEMBERS:MANAGE")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_READ})
        void shouldOmitAffordancesWhenMemberButNotAdmin() throws Exception {
            FamilyGroup group = buildFamilyGroupWithChild(GROUP_UUID, "Novákovi", "dddddddd-dddd-dddd-dddd-dddddddddddd", MEMBER_ID);
            when(familyGroupManagementService.getFamilyGroup(any(FamilyGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/family-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.deleteFamilyGroup").doesNotExist())
                    .andExpect(jsonPath("$._templates.addFamilyGroupParent").doesNotExist())
                    .andExpect(jsonPath("$._templates.addFamilyGroupChild").doesNotExist());
        }
    }

    @Nested
    @DisplayName("Family groups navigation HAL link visibility")
    class FamilyGroupsNavigationLinkTests {

        @Test
        @DisplayName("should include family-groups collection link in detail response for MEMBERS:MANAGE user")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldIncludeCollectionLinkForAdmin() throws Exception {
            FamilyGroup group = buildFamilyGroup(GROUP_UUID, "Novákovi", MEMBER_ID);
            when(familyGroupManagementService.getFamilyGroup(any(FamilyGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/family-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.collection").exists());
        }

        @Test
        @DisplayName("should NOT include family-groups collection link in detail response for non-admin member")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_READ})
        void shouldNotIncludeCollectionLinkForNonAdmin() throws Exception {
            FamilyGroup group = buildFamilyGroupWithChild(GROUP_UUID, "Novákovi", "dddddddd-dddd-dddd-dddd-dddddddddddd", MEMBER_ID);
            when(familyGroupManagementService.getFamilyGroup(any(FamilyGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/family-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.collection").doesNotExist());
        }
    }
}
