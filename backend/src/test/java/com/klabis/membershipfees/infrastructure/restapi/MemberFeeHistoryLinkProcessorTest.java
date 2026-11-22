package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.TargetGrant;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.authorization.TargetType;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.application.ManagementPort;
import com.klabis.members.application.MemberViewAccess;
import com.klabis.membershipfees.MembershipFeesWebMvcTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("MemberFeeHistoryLinkProcessor")
@MembershipFeesWebMvcTest
class MemberFeeHistoryLinkProcessorTest {

    private static final String MEMBER_ID_STR = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final UUID MEMBER_UUID = UUID.fromString(MEMBER_ID_STR);
    private static final String OTHER_MEMBER_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ManagementPort memberManagementPort;

    @BeforeEach
    void stubMember() {
        when(memberManagementPort.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class)))
                .thenReturn(MemberTestDataBuilder.aMemberWithId(MEMBER_UUID).build());
    }

    @Test
    @DisplayName("adds feeHistory when the caller views their own profile")
    @WithKlabisMockUser(memberId = MEMBER_ID_STR, authorities = {Authority.MEMBERS_READ}, selfGrants = true)
    void addsFeeHistoryOnOwnProfile() throws Exception {
        mockMvc.perform(get("/api/members/{id}", MEMBER_UUID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.feeHistory.href").value(
                        containsString("/api/members/" + MEMBER_UUID + "/fee-history")));
    }

    @Test
    @DisplayName("does not add feeHistory when an ordinary member views someone else")
    @WithKlabisMockUser(memberId = OTHER_MEMBER_ID, authorities = {Authority.MEMBERS_READ})
    void omitsFeeHistoryOnOtherMember() throws Exception {
        mockMvc.perform(get("/api/members/{id}", MEMBER_UUID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.feeHistory").doesNotExist());
    }

    @Test
    @DisplayName("adds feeHistory for a MEMBERS:MANAGE holder viewing another member")
    @WithKlabisMockUser(memberId = OTHER_MEMBER_ID, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void addsFeeHistoryForManager() throws Exception {
        mockMvc.perform(get("/api/members/{id}", MEMBER_UUID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.feeHistory.href").value(
                        containsString("/api/members/" + MEMBER_UUID + "/fee-history")));
    }

    @Test
    @DisplayName("adds feeHistory for a guardian viewing their minor")
    @WithKlabisMockUser(memberId = OTHER_MEMBER_ID, authorities = {Authority.MEMBERS_READ}, targetGrants = @TargetGrant(
            authority = Authority.MEMBERS_SELECT_MEMBERSHIP_FEE_TYPE, type = TargetType.MEMBER, ids = MEMBER_ID_STR))
    void addsFeeHistoryForGuardianOfTheMember() throws Exception {
        mockMvc.perform(get("/api/members/{id}", MEMBER_UUID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.feeHistory.href").value(
                        containsString("/api/members/" + MEMBER_UUID + "/fee-history")));
    }

    @Test
    @DisplayName("does not add feeHistory for a guardian viewing a member who is not their minor")
    @WithKlabisMockUser(memberId = OTHER_MEMBER_ID, authorities = {Authority.MEMBERS_READ}, targetGrants = @TargetGrant(
            authority = Authority.MEMBERS_SELECT_MEMBERSHIP_FEE_TYPE, type = TargetType.MEMBER,
            ids = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"))
    void omitsFeeHistoryForGuardianOfDifferentMember() throws Exception {
        mockMvc.perform(get("/api/members/{id}", MEMBER_UUID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.feeHistory").doesNotExist());
    }

    @Test
    @DisplayName("does not add feeHistory for a MEMBERS:READ-only user viewing another member")
    @WithKlabisMockUser(userId = OTHER_MEMBER_ID, authorities = {Authority.MEMBERS_READ})
    void omitsFeeHistoryForMembersReadOnly() throws Exception {
        mockMvc.perform(get("/api/members/{id}", MEMBER_UUID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.feeHistory").doesNotExist());
    }

    @Test
    @DisplayName("does not add the old feeSummary link to the member detail")
    @WithKlabisMockUser(memberId = MEMBER_ID_STR, authorities = {Authority.MEMBERS_READ})
    void doesNotAddFeeSummary() throws Exception {
        mockMvc.perform(get("/api/members/{id}", MEMBER_UUID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.feeSummary").doesNotExist());
    }
}
