package com.klabis.members.infrastructure.restapi;

import com.klabis.common.TargetGrant;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.authorization.TargetType;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.MembersWebMvcTest;
import com.klabis.members.application.ManagementPort;
import com.klabis.members.application.MemberViewAccess;
import com.klabis.members.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Free group owner edits a member's profile through delegated permission (API)")
@MembersWebMvcTest
class GroupOwnerEditsMemberApiTest {

    private static final String OWNER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String MEMBER_ID = "22222222-2222-2222-2222-222222222222";
    private static final String OTHER_MEMBER_ID = "33333333-3333-3333-3333-333333333333";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ManagementPort managementService;

    private void givenMember(String id) {
        MemberId memberId = new MemberId(UUID.fromString(id));
        Member member = MemberTestDataBuilder.aMemberWithId(UUID.fromString(id))
                .withNationality("CZ")
                .withBirthNumber("900101/1234")
                .withDietaryRestrictions("no nuts")
                .withActive(true)
                .build();
        when(managementService.getMemberAndRecordView(eq(memberId), any(UserId.class), any(MemberViewAccess.class)))
                .thenReturn(member);
        when(managementService.prefilledUpdateCommand(memberId)).thenReturn(Member.UpdateMember.from(member));
        when(managementService.updateMember(eq(memberId), any(Member.UpdateMember.class))).thenReturn(member);
    }

    private ResultActions getMember(String id) throws Exception {
        return mockMvc.perform(get("/api/members/{id}", id).accept(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    @Test
    @DisplayName("owner holding the delegated permission over a member gets the edit template with reserved fields read-only")
    @WithKlabisMockUser(userId = OWNER_ID, memberId = OWNER_ID, authorities = {Authority.MEMBERS_READ},
            targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE, type = TargetType.MEMBER,
                    ids = MEMBER_ID))
    void ownerGetsEditTemplateOnMember() throws Exception {
        givenMember(MEMBER_ID);

        getMember(MEMBER_ID)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.birthNumber").value("900101/1234"))
                .andExpect(jsonPath("$.dietaryRestrictions").value("no nuts"))
                .andExpect(jsonPath("$._templates.updateMember.method").value("PATCH"))
                .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='birthNumber')].readOnly")
                        .value(true))
                .andExpect(jsonPath("$._templates.suspendMember").doesNotExist());
    }

    @Test
    @DisplayName("owner saves a change of the member's telephone")
    @WithKlabisMockUser(userId = OWNER_ID, memberId = OWNER_ID, authorities = {Authority.MEMBERS_READ},
            targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE, type = TargetType.MEMBER,
                    ids = MEMBER_ID))
    void ownerSavesPhoneChange() throws Exception {
        givenMember(MEMBER_ID);

        mockMvc.perform(patch("/api/members/{id}", MEMBER_ID)
                        .contentType("application/json")
                        .content("{\"phone\": \"+420777123456\"}"))
                .andExpect(status().isNoContent());

        verify(managementService).updateMember(eq(new MemberId(UUID.fromString(MEMBER_ID))),
                any(Member.UpdateMember.class));
    }

    @Test
    @DisplayName("a member of the group does not get the edit template on another member")
    @WithKlabisMockUser(userId = MEMBER_ID, memberId = MEMBER_ID, authorities = {Authority.MEMBERS_READ})
    void memberDoesNotGetEditTemplateOnOtherMember() throws Exception {
        givenMember(OTHER_MEMBER_ID);

        getMember(OTHER_MEMBER_ID)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.updateMember").doesNotExist())
                .andExpect(jsonPath("$.birthNumber").doesNotExist())
                .andExpect(jsonPath("$.dietaryRestrictions").doesNotExist());
    }

    @Test
    @DisplayName("a member of the group cannot change another member's data")
    @WithKlabisMockUser(userId = MEMBER_ID, memberId = MEMBER_ID, authorities = {Authority.MEMBERS_READ})
    void memberCannotEditOtherMember() throws Exception {
        givenMember(OTHER_MEMBER_ID);

        mockMvc.perform(patch("/api/members/{id}", OTHER_MEMBER_ID)
                        .contentType("application/json")
                        .content("{\"phone\": \"+420777123456\"}"))
                .andExpect(status().isForbidden());

        verify(managementService, never()).updateMember(any(), any());
    }

    @Test
    @DisplayName("owner does not get the edit template on a member the permission is not held over")
    @WithKlabisMockUser(userId = OWNER_ID, memberId = OWNER_ID, authorities = {Authority.MEMBERS_READ},
            targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE, type = TargetType.MEMBER,
                    ids = MEMBER_ID))
    void ownerDoesNotGetEditTemplateOnForeignMember() throws Exception {
        givenMember(OTHER_MEMBER_ID);

        getMember(OTHER_MEMBER_ID)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.updateMember").doesNotExist());
    }
}
