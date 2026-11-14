package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.TargetGrant;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.authorization.TargetType;
import com.klabis.membershipfees.MembershipFeesWebMvcTest;
import com.klabis.common.users.Authority;
import com.klabis.finance.domain.Money;
import com.klabis.members.MemberId;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.application.ManagementPort;
import com.klabis.members.application.MemberViewAccess;
import com.klabis.common.users.UserId;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.application.MemberFeeHistoryPort;
import com.klabis.membershipfees.domain.AssignmentSource;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("MemberFeeSummaryController API tests")
@MembershipFeesWebMvcTest
class MemberFeeSummaryControllerTest {

    private static final String MEMBER_ID_STR = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final UUID MEMBER_UUID = UUID.fromString(MEMBER_ID_STR);
    private static final String OTHER_MEMBER_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final UUID GROUP_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID LEVEL_UUID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final int YEAR = 2026;
    private static final Money FEE = Money.of(new BigDecimal("500.00"), Currency.getInstance("CZK"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberFeeHistoryPort memberFeeHistoryPort;

    @Autowired
    private FeeSelectionCampaignManagementPort publicationManagementPort;

    @Autowired
    private ManagementPort memberManagementPort;

    @Nested
    @DisplayName("GET /api/members/{memberId}/fee-summary/{year}")
    class GetFeeSummaryTests {

        @BeforeEach
        void stubGroups() {
            when(publicationManagementPort.listGroupsForYear(anyInt())).thenReturn(List.of());
        }

        @Test
        @DisplayName("should return 200 with current group info when member has chosen a level")
        @WithKlabisMockUser(memberId = MEMBER_ID_STR)
        void shouldReturnCurrentGroupWhenChosen() throws Exception {
            MemberFeeHistoryPort.CurrentLevelInfo info = new MemberFeeHistoryPort.CurrentLevelInfo(
                    new MembershipFeeGroupId(GROUP_UUID), "Základ", FEE, false, Optional.empty());
            when(memberFeeHistoryPort.getCurrentLevelInfo(any(), eq(YEAR))).thenReturn(info);

            mockMvc.perform(
                            get("/api/members/{memberId}/fee-summary/{year}", MEMBER_UUID, YEAR)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentGroup.id").value(GROUP_UUID.toString()))
                    .andExpect(jsonPath("$.currentGroup.name").value("Základ"))
                    .andExpect(jsonPath("$.currentGroup.yearlyFee").value(500.00))
                    .andExpect(jsonPath("$.votingOpen").value(false));
        }

        @Test
        @DisplayName("should return votingOpen=true and recommendedGroupId when no choice and voting open")
        @WithKlabisMockUser(memberId = MEMBER_ID_STR)
        void shouldReturnVotingOpenWithRecommended() throws Exception {
            MemberFeeHistoryPort.CurrentLevelInfo info = new MemberFeeHistoryPort.CurrentLevelInfo(
                    null, null, null, true,
                    Optional.of(new MembershipFeeGroupId(LEVEL_UUID)));
            when(memberFeeHistoryPort.getCurrentLevelInfo(any(), eq(YEAR))).thenReturn(info);

            mockMvc.perform(
                            get("/api/members/{memberId}/fee-summary/{year}", MEMBER_UUID, YEAR)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentGroup").doesNotExist())
                    .andExpect(jsonPath("$.votingOpen").value(true))
                    .andExpect(jsonPath("$.recommendedGroupId").value(LEVEL_UUID.toString()));
        }

        @Test
        @DisplayName("should include chooseTier affordance when voting is open")
        @WithKlabisMockUser(memberId = MEMBER_ID_STR)
        void shouldIncludeChooseLevelAffordanceWhenVotingOpen() throws Exception {
            MemberFeeHistoryPort.CurrentLevelInfo info = new MemberFeeHistoryPort.CurrentLevelInfo(
                    null, null, null, true, Optional.empty());
            when(memberFeeHistoryPort.getCurrentLevelInfo(any(), eq(YEAR))).thenReturn(info);

            mockMvc.perform(
                            get("/api/members/{memberId}/fee-summary/{year}", MEMBER_UUID, YEAR)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.chooseTier").exists());
        }

        @Test
        @DisplayName("should include inline group options on chooseTier affordance membershipFeeGroupId property")
        @WithKlabisMockUser(memberId = MEMBER_ID_STR)
        void shouldIncludeInlineGroupOptionsOnChooseLevelAffordance() throws Exception {
            MemberFeeHistoryPort.CurrentLevelInfo info = new MemberFeeHistoryPort.CurrentLevelInfo(
                    null, null, null, true, Optional.empty());
            when(memberFeeHistoryPort.getCurrentLevelInfo(any(), eq(YEAR))).thenReturn(info);

            MembershipFeeGroup group = MembershipFeeGroup.createSnapshot(
                    new MembershipFeeTierId(LEVEL_UUID), "Základní", YEAR, FEE, List.of(),
                    LocalDate.of(YEAR, 3, 31));
            when(publicationManagementPort.listGroupsForYear(YEAR)).thenReturn(List.of(group));

            mockMvc.perform(
                            get("/api/members/{memberId}/fee-summary/{year}", MEMBER_UUID, YEAR)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.chooseTier.properties[?(@.name=='membershipFeeGroupId')].options.inline[0].prompt")
                            .value("Základní"));
        }

        @Test
        @DisplayName("should include removeChoice affordance targeting the fee-choice endpoint when voting is open and member has chosen")
        @WithKlabisMockUser(memberId = MEMBER_ID_STR)
        void shouldIncludeRemoveChoiceAffordanceWhenChosenAndVotingOpen() throws Exception {
            MemberFeeHistoryPort.CurrentLevelInfo info = new MemberFeeHistoryPort.CurrentLevelInfo(
                    new MembershipFeeGroupId(GROUP_UUID), "Základ", FEE, true, Optional.empty());
            when(memberFeeHistoryPort.getCurrentLevelInfo(any(), eq(YEAR))).thenReturn(info);

            mockMvc.perform(
                            get("/api/members/{memberId}/fee-summary/{year}", MEMBER_UUID, YEAR)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.removeChoice.method").value("DELETE"))
                    .andExpect(jsonPath("$._templates.removeChoice.target").value(
                            org.hamcrest.Matchers.endsWith("/api/members/" + MEMBER_UUID + "/fee-choice/" + YEAR)));
        }

        @Test
        @DisplayName("should not include removeChoice affordance when voting is closed")
        @WithKlabisMockUser(memberId = MEMBER_ID_STR)
        void shouldNotIncludeRemoveChoiceAffordanceWhenVotingClosed() throws Exception {
            MemberFeeHistoryPort.CurrentLevelInfo info = new MemberFeeHistoryPort.CurrentLevelInfo(
                    new MembershipFeeGroupId(GROUP_UUID), "Základ", FEE, false, Optional.empty());
            when(memberFeeHistoryPort.getCurrentLevelInfo(any(), eq(YEAR))).thenReturn(info);

            mockMvc.perform(
                            get("/api/members/{memberId}/fee-summary/{year}", MEMBER_UUID, YEAR)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.removeChoice").doesNotExist());
        }

        @Test
        @DisplayName("should not include chooseTier affordance when voting is closed")
        @WithKlabisMockUser(memberId = MEMBER_ID_STR)
        void shouldNotIncludeChooseLevelAffordanceWhenVotingClosed() throws Exception {
            MemberFeeHistoryPort.CurrentLevelInfo info = new MemberFeeHistoryPort.CurrentLevelInfo(
                    new MembershipFeeGroupId(GROUP_UUID), "Základ", FEE, false, Optional.empty());
            when(memberFeeHistoryPort.getCurrentLevelInfo(any(), eq(YEAR))).thenReturn(info);

            mockMvc.perform(
                            get("/api/members/{memberId}/fee-summary/{year}", MEMBER_UUID, YEAR)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.chooseTier").doesNotExist());
        }

        @Test
        @DisplayName("should return 403 when member accesses another member's summary")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldReturn403WhenAccessingOtherMember() throws Exception {
            when(memberFeeHistoryPort.getCurrentLevelInfo(any(), eq(YEAR))).thenReturn(
                    new MemberFeeHistoryPort.CurrentLevelInfo(null, null, null, false, Optional.empty()));

            mockMvc.perform(
                            get("/api/members/{memberId}/fee-summary/{year}", MEMBER_UUID, YEAR)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return another member's summary for MEMBERS:MANAGE holder")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID, authorities = Authority.MEMBERS_MANAGE)
        void shouldReturnSummaryToManagerOfOtherMember() throws Exception {
            when(memberFeeHistoryPort.getCurrentLevelInfo(any(), eq(YEAR))).thenReturn(
                    new MemberFeeHistoryPort.CurrentLevelInfo(null, null, null, false, Optional.empty()));

            mockMvc.perform(
                            get("/api/members/{memberId}/fee-summary/{year}", MEMBER_UUID, YEAR)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/members/{memberId}/fee-history")
    class GetFeeHistoryTests {

        @Test
        @DisplayName("should return 200 with list of past assignments")
        @WithKlabisMockUser(memberId = MEMBER_ID_STR)
        void shouldReturnFeeHistory() throws Exception {
            List<MemberFeeHistoryPort.LevelAssignment> history = List.of(
                    new MemberFeeHistoryPort.LevelAssignment(
                            2025, new MembershipFeeGroupId(GROUP_UUID), "Základ",
                            LocalDate.of(2024, 11, 15), AssignmentSource.MEMBER_CHOICE),
                    new MemberFeeHistoryPort.LevelAssignment(
                            2024, new MembershipFeeGroupId(UUID.randomUUID()), "Základní",
                            LocalDate.of(2023, 11, 10), AssignmentSource.ADMIN_ASSIGNMENT)
            );
            when(memberFeeHistoryPort.getLevelHistory(any())).thenReturn(history);

            mockMvc.perform(
                            get("/api/members/{memberId}/fee-history", MEMBER_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assignments").isArray())
                    .andExpect(jsonPath("$.assignments[0].year").value(2025))
                    .andExpect(jsonPath("$.assignments[0].groupName").value("Základ"))
                    .andExpect(jsonPath("$.assignments[0].joinedAt").value("2024-11-15"))
                    .andExpect(jsonPath("$.assignments[0].source").value("MEMBER_CHOICE"))
                    .andExpect(jsonPath("$.assignments[1].year").value(2024))
                    .andExpect(jsonPath("$.assignments[1].source").value("ADMIN_ASSIGNMENT"));
        }

        @Test
        @DisplayName("should return 200 with empty assignments when no history")
        @WithKlabisMockUser(memberId = MEMBER_ID_STR)
        void shouldReturnEmptyHistory() throws Exception {
            when(memberFeeHistoryPort.getLevelHistory(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/members/{memberId}/fee-history", MEMBER_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assignments").isArray())
                    .andExpect(jsonPath("$.assignments").isEmpty());
        }

        @Test
        @DisplayName("should link back to the member")
        @WithKlabisMockUser(memberId = MEMBER_ID_STR, authorities = {Authority.MEMBERS_READ})
        void shouldLinkToMember() throws Exception {
            when(memberFeeHistoryPort.getLevelHistory(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/members/{memberId}/fee-history", MEMBER_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.member.href").value(
                            org.hamcrest.Matchers.endsWith("/api/members/" + MEMBER_UUID)));
        }

        @Test
        @DisplayName("should point feeSummary to the campaign year for the owner")
        @WithKlabisMockUser(memberId = MEMBER_ID_STR)
        void shouldPointFeeSummaryToCampaignYear() throws Exception {
            when(memberFeeHistoryPort.getLevelHistory(any())).thenReturn(List.of());
            when(publicationManagementPort.relevantFeeYear(any(LocalDate.class))).thenReturn(2027);

            mockMvc.perform(
                            get("/api/members/{memberId}/fee-history", MEMBER_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.feeSummary.href").value(
                            org.hamcrest.Matchers.containsString("/api/members/" + MEMBER_UUID + "/fee-summary/2027")));
        }

        @Test
        @DisplayName("should point feeSummary to the current year when no campaign is active")
        @WithKlabisMockUser(memberId = MEMBER_ID_STR)
        void shouldPointFeeSummaryToCurrentYear() throws Exception {
            when(memberFeeHistoryPort.getLevelHistory(any())).thenReturn(List.of());
            when(publicationManagementPort.relevantFeeYear(any(LocalDate.class))).thenReturn(2026);

            mockMvc.perform(
                            get("/api/members/{memberId}/fee-history", MEMBER_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.feeSummary.href").value(
                            org.hamcrest.Matchers.containsString("/api/members/" + MEMBER_UUID + "/fee-summary/2026")));
        }

        @Test
        @DisplayName("should return 403 when member accesses another member's history")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldReturn403WhenAccessingOtherMember() throws Exception {
            mockMvc.perform(
                            get("/api/members/{memberId}/fee-history", MEMBER_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return another member's history for MEMBERS:MANAGE holder with feeSummary link")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void shouldReturnHistoryToManagerWithFeeSummaryLink() throws Exception {
            when(memberFeeHistoryPort.getLevelHistory(any())).thenReturn(List.of(
                    new MemberFeeHistoryPort.LevelAssignment(
                            2025, new MembershipFeeGroupId(GROUP_UUID), "Základ",
                            LocalDate.of(2024, 11, 15), AssignmentSource.MEMBER_CHOICE)));
            when(publicationManagementPort.relevantFeeYear(any(LocalDate.class))).thenReturn(2026);

            mockMvc.perform(
                            get("/api/members/{memberId}/fee-history", MEMBER_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assignments[0].year").value(2025))
                    .andExpect(jsonPath("$._links.member.href").exists())
                    .andExpect(jsonPath("$._links.feeSummary.href").exists());
        }

        @Test
        @DisplayName("should return history to a guardian holding MEMBERS:EDIT_PROFILE over the member with feeSummary link")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID, targetGrants = @TargetGrant(
                authority = Authority.MEMBERS_EDIT_PROFILE, type = TargetType.MEMBER, ids = MEMBER_ID_STR))
        void shouldReturnHistoryToGuardianOfTheMember() throws Exception {
            when(memberFeeHistoryPort.getLevelHistory(any())).thenReturn(List.of());
            when(publicationManagementPort.relevantFeeYear(any(LocalDate.class))).thenReturn(2026);

            mockMvc.perform(
                            get("/api/members/{memberId}/fee-history", MEMBER_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.feeSummary.href").exists());
        }

        @Test
        @DisplayName("should return history to a guardian without a member profile")
        @WithKlabisMockUser(userId = OTHER_MEMBER_ID, targetGrants = @TargetGrant(
                authority = Authority.MEMBERS_EDIT_PROFILE, type = TargetType.MEMBER, ids = MEMBER_ID_STR))
        void shouldReturnHistoryToNonMemberGuardian() throws Exception {
            when(memberFeeHistoryPort.getLevelHistory(any())).thenReturn(List.of());
            when(publicationManagementPort.relevantFeeYear(any(LocalDate.class))).thenReturn(2026);

            mockMvc.perform(
                            get("/api/members/{memberId}/fee-history", MEMBER_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 403 to a guardian holding the grant over a different member")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID, targetGrants = @TargetGrant(
                authority = Authority.MEMBERS_EDIT_PROFILE, type = TargetType.MEMBER,
                ids = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"))
        void shouldReturn403ToGuardianOfDifferentMember() throws Exception {
            mockMvc.perform(
                            get("/api/members/{memberId}/fee-history", MEMBER_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 403 to a MEMBERS:READ holder viewing another member")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID, authorities = {Authority.MEMBERS_READ})
        void shouldReturn403ToMembersReadHolder() throws Exception {
            mockMvc.perform(
                            get("/api/members/{memberId}/fee-history", MEMBER_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 403 to a FINANCE:MANAGE holder viewing another member")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID, authorities = {Authority.FINANCE_MANAGE})
        void shouldReturn403ToFinanceManageHolder() throws Exception {
            mockMvc.perform(
                            get("/api/members/{memberId}/fee-history", MEMBER_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
        }
    }
}
