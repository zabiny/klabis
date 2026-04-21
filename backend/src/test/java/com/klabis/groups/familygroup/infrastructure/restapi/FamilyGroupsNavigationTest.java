package com.klabis.groups.familygroup.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.usergroup.GroupMembership;
import com.klabis.common.users.Authority;
import com.klabis.groups.familygroup.FamilyGroupId;
import com.klabis.groups.familygroup.application.FamilyGroupManagementPort;
import com.klabis.groups.familygroup.domain.FamilyGroup;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests that the "family-groups" navigation link (collection rel) in the family group detail response
 * is only visible to users with MEMBERS:MANAGE permission.
 * This mirrors the FamilyGroupsRootPostprocessor behaviour: klabisLinkTo respects @HasAuthority on
 * listFamilyGroups and omits the link for users without MEMBERS:MANAGE.
 */
@DisplayName("FamilyGroups navigation HAL link visibility")
@WebMvcTest(controllers = {FamilyGroupController.class})
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
@WithPostprocessors
class FamilyGroupsNavigationTest {

    private static final String ADMIN_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String MEMBER_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final UUID GROUP_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FamilyGroupManagementPort familyGroupManagementService;

    @Test
    @DisplayName("should include family-groups collection link for MEMBERS:MANAGE users")
    @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
    void shouldIncludeFamilyGroupsLinkForAdmin() throws Exception {
        MemberId adminMember = new MemberId(UUID.fromString(ADMIN_ID));
        FamilyGroup group = FamilyGroup.reconstruct(new FamilyGroupId(GROUP_UUID), "Novákovi", Set.of(adminMember), Set.of(), null);
        when(familyGroupManagementService.getFamilyGroup(any(FamilyGroupId.class))).thenReturn(group);

        mockMvc.perform(
                        get("/api/family-groups/{id}", GROUP_UUID)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.collection").exists());
    }

    @Test
    @DisplayName("should NOT include family-groups collection link for users without MEMBERS:MANAGE")
    @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_READ})
    void shouldNotIncludeFamilyGroupsLinkForNonAdmin() throws Exception {
        MemberId member = new MemberId(UUID.fromString(MEMBER_ID));
        GroupMembership membership = GroupMembership.of(member.toUserId());
        MemberId parent = new MemberId(UUID.fromString(ADMIN_ID));
        FamilyGroup group = FamilyGroup.reconstruct(new FamilyGroupId(GROUP_UUID), "Novákovi", Set.of(parent), Set.of(membership), null);
        when(familyGroupManagementService.getFamilyGroup(any(FamilyGroupId.class))).thenReturn(group);

        mockMvc.perform(
                        get("/api/family-groups/{id}", GROUP_UUID)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.collection").doesNotExist());
    }
}
