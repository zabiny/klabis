package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.common.users.Authority;
import com.klabis.membershipfees.FeeSelectionCampaignId;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.MembershipFeesWebMvcTest;
import com.klabis.membershipfees.application.AdminFeeAssignmentPort;
import com.klabis.membershipfees.application.CampaignAlreadyProcessedException;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.application.FeeSelectionCampaignNotFoundException;
import com.klabis.membershipfees.application.ManualCampaignClosePort;
import com.klabis.membershipfees.application.MemberChoicePort;
import com.klabis.membershipfees.application.MembershipFeeGroupNotFoundException;
import com.klabis.membershipfees.application.MembershipFeeTierManagementPort;
import com.klabis.membershipfees.application.MembershipFeeTierNotFoundException;
import com.klabis.membershipfees.application.MembershipFeeTierOptionsPort;
import com.klabis.membershipfees.domain.ActiveCampaignExistsException;
import com.klabis.membershipfees.domain.CampaignClosedException;
import com.klabis.membershipfees.domain.DuplicatePaymentRuleException;
import com.klabis.membershipfees.domain.EventTypeReference;
import com.klabis.membershipfees.domain.PaymentRuleNotFoundException;
import com.klabis.membershipfees.domain.SnapshotFrozenException;
import com.klabis.membershipfees.domain.VotingClosedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Membership fees error types")
@MembershipFeesWebMvcTest
class MembershipFeesErrorTypesTest {

    private static final String ADMIN_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final UUID TIER_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final MembershipFeeTierId TIER_ID = new MembershipFeeTierId(TIER_UUID);
    private static final UUID CAMPAIGN_UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID GROUP_UUID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID EVENT_TYPE_UUID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final Set<String> STANDARD_PROPERTIES = Set.of("type", "title", "status", "detail", "instance");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MembershipFeeTierManagementPort tierPort;
    @Autowired
    private MembershipFeeTierOptionsPort optionsPort;
    @Autowired
    private FeeSelectionCampaignManagementPort campaignPort;
    @Autowired
    private ManualCampaignClosePort closePort;
    @Autowired
    private MemberChoicePort choicePort;
    @Autowired
    private AdminFeeAssignmentPort assignmentPort;

    @BeforeEach
    void stubOptions() {
        when(optionsPort.listEventTypeOptions())
                .thenReturn(List.of(new HalFormsInlineOption(EVENT_TYPE_UUID.toString(), "Závod jednotlivců")));
        when(optionsPort.listRankingOptions()).thenReturn(List.of(new HalFormsInlineOption("A", "Ž A")));
    }

    private ResultActions addRule(String body) throws Exception {
        return mockMvc.perform(post("/api/membership-fee-tiers/{id}/rules", TIER_UUID)
                .contentType("application/json")
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                .content(body));
    }

    private static final String ADD_PERCENT_RULE = """
            {"eventTypeId":"%s","rankingShortName":"A","ruleType":"PERCENTAGE","percentage":50}
            """.formatted(EVENT_TYPE_UUID);

    @SuppressWarnings("unchecked")
    private Map<String, Object> extensionProperties(ResultActions result) throws Exception {
        Map<String, Object> body = objectMapper.readValue(
                result.andReturn().getResponse().getContentAsString(), Map.class);
        Map<String, Object> extensions = new java.util.LinkedHashMap<>(body);
        extensions.keySet().removeAll(STANDARD_PROPERTIES);
        return extensions;
    }

    @Nested
    @DisplayName("duplicate payment rule")
    class DuplicateRule {

        @Test
        @DisplayName("names the event type and the ranking and carries no identifier in any parameter")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void namesEventTypeAndRanking() throws Exception {
            doThrow(new DuplicatePaymentRuleException(EventTypeReference.of(EVENT_TYPE_UUID), "A"))
                    .when(tierPort).addRule(any(), any());

            ResultActions result = addRule(ADD_PERCENT_RULE)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("/errors/duplicate-payment-rule"))
                    .andExpect(jsonPath("$.eventTypeName").value("Závod jednotlivců"))
                    .andExpect(jsonPath("$.rankingLabel").value("Ž A"));

            assertThat(extensionProperties(result).values().toString())
                    .doesNotContain(EVENT_TYPE_UUID.toString());
        }

        @Test
        @DisplayName("leaves a parameter out when its name cannot be resolved, never replacing it with the identifier")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void leavesOutUnresolvedParameters() throws Exception {
            when(optionsPort.listEventTypeOptions()).thenThrow(new IllegalStateException("events unavailable"));
            when(optionsPort.listRankingOptions()).thenReturn(List.of());
            doThrow(new DuplicatePaymentRuleException(EventTypeReference.of(EVENT_TYPE_UUID), "A"))
                    .when(tierPort).addRule(any(), any());

            ResultActions result = addRule(ADD_PERCENT_RULE)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("/errors/duplicate-payment-rule"))
                    .andExpect(jsonPath("$.eventTypeName").doesNotExist())
                    .andExpect(jsonPath("$.rankingLabel").doesNotExist());

            assertThat(extensionProperties(result)).isEmpty();
        }

        @Test
        @DisplayName("keeps the resolved parameter when only the other lookup fails")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void keepsResolvedParameter() throws Exception {
            when(optionsPort.listRankingOptions()).thenThrow(new IllegalStateException("oris unavailable"));
            doThrow(new DuplicatePaymentRuleException(EventTypeReference.of(EVENT_TYPE_UUID), "A"))
                    .when(tierPort).addRule(any(), any());

            addRule(ADD_PERCENT_RULE)
                    .andExpect(jsonPath("$.eventTypeName").value("Závod jednotlivců"))
                    .andExpect(jsonPath("$.rankingLabel").doesNotExist());
        }
    }

    @Nested
    @DisplayName("payment rule not found")
    class RuleNotFound {

        @Test
        @DisplayName("names the event type and the ranking")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void namesEventTypeAndRanking() throws Exception {
            doThrow(new PaymentRuleNotFoundException(EventTypeReference.of(EVENT_TYPE_UUID), "A"))
                    .when(tierPort).removeRule(any(), any());

            ResultActions result = mockMvc.perform(
                            delete("/api/membership-fee-tiers/{id}/rules/{eventTypeId}/{ranking}",
                                    TIER_UUID, EVENT_TYPE_UUID, "A")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("/errors/payment-rule-not-found"))
                    .andExpect(jsonPath("$.eventTypeName").value("Závod jednotlivců"))
                    .andExpect(jsonPath("$.rankingLabel").value("Ž A"));

            assertThat(extensionProperties(result).values().toString())
                    .doesNotContain(EVENT_TYPE_UUID.toString());
        }

        @Test
        @DisplayName("leaves out names that cannot be resolved")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void leavesOutUnresolved() throws Exception {
            when(optionsPort.listEventTypeOptions()).thenReturn(List.of());
            when(optionsPort.listRankingOptions()).thenReturn(List.of());
            doThrow(new PaymentRuleNotFoundException(EventTypeReference.of(EVENT_TYPE_UUID), "A"))
                    .when(tierPort).removeRule(any(), any());

            ResultActions result = mockMvc.perform(
                            delete("/api/membership-fee-tiers/{id}/rules/{eventTypeId}/{ranking}",
                                    TIER_UUID, EVENT_TYPE_UUID, "A")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(jsonPath("$.type").value("/errors/payment-rule-not-found"));

            assertThat(extensionProperties(result)).isEmpty();
        }
    }

    @Nested
    @DisplayName("input checks a user can trigger")
    class InputChecks {

        @Test
        @DisplayName("percentage rule without a percentage names the rule type")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void missingPercentage() throws Exception {
            addRule("""
                    {"eventTypeId":"%s","rankingShortName":"A","ruleType":"PERCENTAGE"}
                    """.formatted(EVENT_TYPE_UUID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/errors/payment-rule-value-missing"))
                    .andExpect(jsonPath("$.ruleType").value("PERCENTAGE"));
        }

        @Test
        @DisplayName("fixed amount rule without an amount names the rule type")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void missingFixedAmount() throws Exception {
            addRule("""
                    {"eventTypeId":"%s","rankingShortName":"A","ruleType":"FIXED_AMOUNT"}
                    """.formatted(EVENT_TYPE_UUID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/errors/payment-rule-value-missing"))
                    .andExpect(jsonPath("$.ruleType").value("FIXED_AMOUNT"));
        }

        @Test
        @DisplayName("negative percentage carries the entered percentage")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void negativePercentage() throws Exception {
            addRule("""
                    {"eventTypeId":"%s","rankingShortName":"A","ruleType":"PERCENTAGE","percentage":-5}
                    """.formatted(EVENT_TYPE_UUID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/errors/negative-payment-rule-percentage"))
                    .andExpect(jsonPath("$.percent").value(-5));
        }
    }

    @Nested
    @DisplayName("campaign and group rules")
    class CampaignRules {

        @Test
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        @DisplayName("another campaign is active")
        void activeCampaignExists() throws Exception {
            when(campaignPort.publishYear(any())).thenThrow(new ActiveCampaignExistsException());

            mockMvc.perform(post("/api/fee-selection-campaigns")
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content("""
                                    {"year": 2026, "votingDeadline": "2099-03-31", "levelIds": ["%s"]}
                                    """.formatted(TIER_UUID)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("/errors/active-campaign-exists"));
        }

        @Test
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        @DisplayName("campaign is closed")
        void campaignClosed() throws Exception {
            doThrow(new CampaignClosedException()).when(campaignPort).changeDeadline(any(), any());

            mockMvc.perform(patch("/api/fee-selection-campaigns/{id}/deadline", CAMPAIGN_UUID)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content("""
                                    {"votingDeadline": "2099-01-01"}
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("/errors/campaign-closed"));
        }

        @Test
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        @DisplayName("campaign already processed")
        void campaignAlreadyProcessed() throws Exception {
            doThrow(new CampaignAlreadyProcessedException()).when(closePort).closeCampaign(any());

            mockMvc.perform(post("/api/fee-selection-campaigns/{id}/close", CAMPAIGN_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("/errors/campaign-already-processed"));
        }

        @Test
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        @DisplayName("snapshot of a frozen group")
        void snapshotFrozen() throws Exception {
            doThrow(new SnapshotFrozenException()).when(campaignPort).editGroupSnapshot(any(), any());

            mockMvc.perform(patch("/api/membership-fee-groups/{id}", GROUP_UUID)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content("""
                                    {"yearlyFeeAmount": 1200}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/errors/snapshot-frozen"));
        }

        @Test
        @WithKlabisMockUser(memberId = ADMIN_ID)
        @DisplayName("voting is closed")
        void votingClosed() throws Exception {
            doThrow(new VotingClosedException()).when(choicePort).chooseFeeLevel(any());

            mockMvc.perform(post("/api/members/{memberId}/fee-choice/{year}", ADMIN_ID, 2026)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content("""
                                    {"membershipFeeGroupId": "%s"}
                                    """.formatted(GROUP_UUID)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/errors/voting-closed"));
        }
    }

    @Nested
    @DisplayName("records that do not exist")
    class NotFound {

        @Test
        @WithKlabisMockUser(memberId = ADMIN_ID)
        @DisplayName("fee tier")
        void tierNotFound() throws Exception {
            when(tierPort.getTier(TIER_ID)).thenThrow(new MembershipFeeTierNotFoundException(TIER_ID));

            ResultActions result = mockMvc.perform(get("/api/membership-fee-tiers/{id}", TIER_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("/errors/membership-fee-tier-not-found"));

            assertThat(extensionProperties(result)).isEmpty();
        }

        @Test
        @WithKlabisMockUser(memberId = ADMIN_ID)
        @DisplayName("fee group")
        void groupNotFound() throws Exception {
            when(campaignPort.getGroup(new MembershipFeeGroupId(GROUP_UUID)))
                    .thenThrow(new MembershipFeeGroupNotFoundException(new MembershipFeeGroupId(GROUP_UUID)));

            ResultActions result = mockMvc.perform(get("/api/membership-fee-groups/{id}", GROUP_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("/errors/membership-fee-group-not-found"));

            assertThat(extensionProperties(result)).isEmpty();
        }

        @Test
        @WithKlabisMockUser(memberId = ADMIN_ID)
        @DisplayName("campaign by id")
        void campaignNotFoundById() throws Exception {
            FeeSelectionCampaignId id = new FeeSelectionCampaignId(CAMPAIGN_UUID);
            when(campaignPort.getPublication(id)).thenThrow(new FeeSelectionCampaignNotFoundException(id));

            ResultActions result = mockMvc.perform(get("/api/fee-selection-campaigns/{id}", CAMPAIGN_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("/errors/fee-selection-campaign-not-found"));

            assertThat(extensionProperties(result)).isEmpty();
        }

        @Test
        @WithKlabisMockUser(memberId = ADMIN_ID)
        @DisplayName("campaign by year names the year")
        void campaignNotFoundByYear() throws Exception {
            when(campaignPort.getPublication(any(FeeSelectionCampaignId.class)))
                    .thenThrow(new FeeSelectionCampaignNotFoundException(2031));

            mockMvc.perform(get("/api/fee-selection-campaigns/{id}", CAMPAIGN_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("/errors/fee-selection-campaign-not-found"))
                    .andExpect(jsonPath("$.year").value(2031));
        }
    }
}
