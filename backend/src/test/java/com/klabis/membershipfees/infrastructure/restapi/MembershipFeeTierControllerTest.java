package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.application.MembershipFeeTierManagementPort;
import com.klabis.membershipfees.application.MembershipFeeTierNotFoundException;
import com.klabis.membershipfees.application.RankingOptionsPort;
import com.klabis.membershipfees.domain.MembershipFeeTier;
import com.klabis.membershipfees.domain.MembershipPaymentRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("MembershipFeeTierController API tests")
@WebMvcTest(controllers = {MembershipFeeTierController.class, MembershipFeesExceptionHandler.class})
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
@WithPostprocessors
class MembershipFeeTierControllerTest {

    private static final String MEMBER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final UUID LEVEL_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final MembershipFeeTierId LEVEL_ID = new MembershipFeeTierId(LEVEL_UUID);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MembershipFeeTierManagementPort managementPort;

    @MockitoBean
    private RankingOptionsPort rankingOptionsPort;

    @BeforeEach
    void stubRankingOptions() {
        when(rankingOptionsPort.listRankingOptions()).thenReturn(List.of());
    }

    private MembershipFeeTier buildLevel(UUID id, String name) {
        return MembershipFeeTier.reconstruct(
                new MembershipFeeTierId(id), name,
                Money.ofCzk(new BigDecimal("1200.00")), List.of(), null);
    }

    @Nested
    @DisplayName("POST /api/membership-fee-tiers")
    class CreateLevelTests {

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(
                            post("/api/membership-fee-tiers")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Dospělý", "yearlyFeeAmount": 1200}
                                            """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 201 with Location header when user has MEMBERS:MANAGE")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldCreateLevelAndReturn201() throws Exception {
            when(managementPort.createTier(any()))
                    .thenReturn(LEVEL_ID);

            mockMvc.perform(
                            post("/api/membership-fee-tiers")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Dospělý", "yearlyFeeAmount": 1200}
                                            """))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location",
                            org.hamcrest.Matchers.endsWith("/api/membership-fee-tiers/" + LEVEL_UUID)));
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WhenNameIsBlank() throws Exception {
            mockMvc.perform(
                            post("/api/membership-fee-tiers")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "", "yearlyFeeAmount": 1200}
                                            """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should map name and yearlyFee to CreateTierCommand without rules")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldMapRequestToCommandWithoutRules() throws Exception {
            when(managementPort.createTier(any())).thenReturn(LEVEL_ID);

            mockMvc.perform(
                            post("/api/membership-fee-tiers")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name":"Závodní","yearlyFeeAmount":1200,"yearlyFeeCurrency":"CZK"}
                                            """))
                    .andExpect(status().isCreated());

            var captor = forClass(MembershipFeeTierManagementPort.CreateTierCommand.class);
            verify(managementPort).createTier(captor.capture());
            MembershipFeeTierManagementPort.CreateTierCommand command = captor.getValue();

            assertThat(command.name()).isEqualTo("Závodní");
            assertThat(command.yearlyFee()).isNotNull();
        }
    }

    @Nested
    @DisplayName("GET /api/membership-fee-tiers")
    class ListLevelsTests {

        @Test
        @DisplayName("should return 200 with list of levels")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturnListOfLevels() throws Exception {
            when(managementPort.listTiers()).thenReturn(List.of(
                    buildLevel(LEVEL_UUID, "Dospělý"),
                    buildLevel(UUID.randomUUID(), "Mládež")
            ));

            mockMvc.perform(
                            get("/api/membership-fee-tiers")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(jsonPath("$._embedded.membershipFeeTierSummaryResponseList").isArray())
                    .andExpect(jsonPath("$._embedded.membershipFeeTierSummaryResponseList.length()").value(2));
        }

        @Test
        @DisplayName("should return 200 with empty list when no levels exist")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturnEmptyList() throws Exception {
            when(managementPort.listTiers()).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/membership-fee-tiers")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should include create affordance for MEMBERS:MANAGE user")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldIncludeCreateAffordance() throws Exception {
            when(managementPort.listTiers()).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/membership-fee-tiers")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.createTier").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/membership-fee-tiers/{id}")
    class GetLevelTests {

        @Test
        @DisplayName("should return 200 with level details")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturnLevelDetails() throws Exception {
            when(managementPort.getTier(LEVEL_ID))
                    .thenReturn(buildLevel(LEVEL_UUID, "Dospělý"));

            mockMvc.perform(
                            get("/api/membership-fee-tiers/{id}", LEVEL_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Dospělý"))
                    .andExpect(jsonPath("$.yearlyFeeAmount").value(1200))
                    .andExpect(jsonPath("$.yearlyFeeCurrency").value("CZK"));
        }

        @Test
        @DisplayName("should return 400 when level not found")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn400WhenNotFound() throws Exception {
            when(managementPort.getTier(LEVEL_ID))
                    .thenThrow(new MembershipFeeTierNotFoundException(LEVEL_ID));

            mockMvc.perform(
                            get("/api/membership-fee-tiers/{id}", LEVEL_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should include self link and edit/delete affordances for MEMBERS:MANAGE user")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldIncludeAffordancesForAdmin() throws Exception {
            when(managementPort.getTier(LEVEL_ID))
                    .thenReturn(buildLevel(LEVEL_UUID, "Dospělý"));

            mockMvc.perform(
                            get("/api/membership-fee-tiers/{id}", LEVEL_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.collection.href").exists());
        }

        @Test
        @DisplayName("addRule template should include ranking property with inline options from ORIS")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldIncludeRankingInlineOptionsInAddRuleTemplate() throws Exception {
            when(managementPort.getTier(LEVEL_ID))
                    .thenReturn(buildLevel(LEVEL_UUID, "Závodní"));
            when(rankingOptionsPort.listRankingOptions()).thenReturn(List.of(
                    new HalFormsInlineOption("A", "Elita"),
                    new HalFormsInlineOption("B", "Výkonnostní"),
                    new HalFormsInlineOption("WRE", "World Ranking Event")
            ));

            mockMvc.perform(
                            get("/api/membership-fee-tiers/{id}", LEVEL_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.addRule").exists())
                    .andExpect(jsonPath("$._templates.addRule.properties[?(@.name=='rankingShortName')]").exists())
                    .andExpect(jsonPath("$._templates.addRule.properties[?(@.name=='rankingShortName')].options.inline").isArray())
                    .andExpect(jsonPath("$._templates.addRule.properties[?(@.name=='rankingShortName')].options.inline[0].value").value("A"))
                    .andExpect(jsonPath("$._templates.addRule.properties[?(@.name=='rankingShortName')].options.inline[0].prompt").value("Elita"))
                    .andExpect(jsonPath("$._templates.addRule.properties[?(@.name=='rankingShortName')].options.inline[1].value").value("B"))
                    .andExpect(jsonPath("$._templates.addRule.properties[?(@.name=='rankingShortName')].options.inline[1].prompt").value("Výkonnostní"))
                    .andExpect(jsonPath("$._templates.addRule.properties[?(@.name=='rankingShortName')].options.promptField").value("prompt"))
                    .andExpect(jsonPath("$._templates.addRule.properties[?(@.name=='rankingShortName')].options.valueField").value("value"));
        }

        @Test
        @DisplayName("addRule template should include empty ranking options when ORIS is unavailable")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldIncludeEmptyRankingOptionsWhenOrisUnavailable() throws Exception {
            when(managementPort.getTier(LEVEL_ID))
                    .thenReturn(buildLevel(LEVEL_UUID, "Dospělý"));
            when(rankingOptionsPort.listRankingOptions()).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/membership-fee-tiers/{id}", LEVEL_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.addRule").exists());
        }
    }

    @Nested
    @DisplayName("PATCH /api/membership-fee-tiers/{id}")
    class EditLevelTests {

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(
                            patch("/api/membership-fee-tiers/{id}", LEVEL_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "New Name"}
                                            """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 204 when edit is successful")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn204OnSuccess() throws Exception {
            doNothing().when(managementPort).editTier(eq(LEVEL_ID), any());

            mockMvc.perform(
                            patch("/api/membership-fee-tiers/{id}", LEVEL_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Updated Name"}
                                            """))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should deserialize FIXED_AMOUNT rule from PATCH request and pass to command")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldDeserializeRulesInEditCommand() throws Exception {
            var eventTypeUuid = UUID.fromString("e2be588c-91ad-43e4-8d14-efa7de02782d");
            doNothing().when(managementPort).editTier(eq(LEVEL_ID), any());

            mockMvc.perform(
                            patch("/api/membership-fee-tiers/{id}", LEVEL_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"rules":[{"eventTypeId":"%s","rankingShortName":"B","ruleType":"FIXED_AMOUNT","fixedAmount":200,"fixedCurrency":"CZK"}]}
                                            """.formatted(eventTypeUuid)))
                    .andExpect(status().isNoContent());

            var captor = forClass(MembershipFeeTierManagementPort.EditTierCommand.class);
            verify(managementPort).editTier(eq(LEVEL_ID), captor.capture());
            MembershipFeeTierManagementPort.EditTierCommand command = captor.getValue();

            assertThat(command.rules()).hasSize(1);
            MembershipPaymentRule rule = command.rules().get(0);
            assertThat(rule.rankingShortName()).isEqualTo("B");
            assertThat(rule.value()).isInstanceOf(MembershipPaymentRule.RuleValue.FixedAmount.class);
        }
    }

    @Nested
    @DisplayName("POST /api/membership-fee-tiers/{id}/rules")
    class AddRuleTests {

        private static final UUID EVENT_TYPE_UUID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(
                            post("/api/membership-fee-tiers/{id}/rules", LEVEL_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"eventTypeId":"%s","rankingShortName":"A","ruleType":"PERCENTAGE","percentage":50}
                                            """.formatted(EVENT_TYPE_UUID)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 201 when adding a percentage rule")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn201ForPercentageRule() throws Exception {
            doNothing().when(managementPort).addRule(eq(LEVEL_ID), any());

            mockMvc.perform(
                            post("/api/membership-fee-tiers/{id}/rules", LEVEL_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"eventTypeId":"%s","rankingShortName":"A","ruleType":"PERCENTAGE","percentage":50}
                                            """.formatted(EVENT_TYPE_UUID)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("should return 201 when adding a fixed-amount rule")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn201ForFixedAmountRule() throws Exception {
            doNothing().when(managementPort).addRule(eq(LEVEL_ID), any());

            mockMvc.perform(
                            post("/api/membership-fee-tiers/{id}/rules", LEVEL_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"eventTypeId":"%s","rankingShortName":"B","ruleType":"FIXED_AMOUNT","fixedAmount":200,"fixedCurrency":"CZK"}
                                            """.formatted(EVENT_TYPE_UUID)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("should return 409 when rule with same key already exists")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn409OnDuplicateRule() throws Exception {
            doThrow(new com.klabis.membershipfees.domain.DuplicatePaymentRuleException(
                    com.klabis.membershipfees.domain.EventTypeReference.of(EVENT_TYPE_UUID), "A"))
                    .when(managementPort).addRule(eq(LEVEL_ID), any());

            mockMvc.perform(
                            post("/api/membership-fee-tiers/{id}/rules", LEVEL_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"eventTypeId":"%s","rankingShortName":"A","ruleType":"PERCENTAGE","percentage":50}
                                            """.formatted(EVENT_TYPE_UUID)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should map request fields to AddRuleCommand correctly")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldMapRequestToCommand() throws Exception {
            doNothing().when(managementPort).addRule(eq(LEVEL_ID), any());

            mockMvc.perform(
                            post("/api/membership-fee-tiers/{id}/rules", LEVEL_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"eventTypeId":"%s","rankingShortName":"B","ruleType":"FIXED_AMOUNT","fixedAmount":300,"fixedCurrency":"CZK"}
                                            """.formatted(EVENT_TYPE_UUID)))
                    .andExpect(status().isCreated());

            var captor = forClass(MembershipFeeTierManagementPort.AddRuleCommand.class);
            verify(managementPort).addRule(eq(LEVEL_ID), captor.capture());
            MembershipFeeTierManagementPort.AddRuleCommand command = captor.getValue();

            assertThat(command.rule().rankingShortName()).isEqualTo("B");
            assertThat(command.rule().value()).isInstanceOf(MembershipPaymentRule.RuleValue.FixedAmount.class);
        }
    }

    @Nested
    @DisplayName("PATCH /api/membership-fee-tiers/{id}/rules/{eventTypeId}/{ranking}")
    class EditRuleTests {

        private static final UUID EVENT_TYPE_UUID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        private static final String RANKING = "A";

        @Test
        @DisplayName("should return 204 when editing a rule's percentage value")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn204ForPercentageEdit() throws Exception {
            doNothing().when(managementPort).editRule(eq(LEVEL_ID), any());

            mockMvc.perform(
                            patch("/api/membership-fee-tiers/{id}/rules/{eventTypeId}/{ranking}",
                                    LEVEL_UUID, EVENT_TYPE_UUID, RANKING)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"ruleType":"PERCENTAGE","percentage":75}
                                            """))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 204 when editing a rule's fixed amount value")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn204ForFixedAmountEdit() throws Exception {
            doNothing().when(managementPort).editRule(eq(LEVEL_ID), any());

            mockMvc.perform(
                            patch("/api/membership-fee-tiers/{id}/rules/{eventTypeId}/{ranking}",
                                    LEVEL_UUID, EVENT_TYPE_UUID, RANKING)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"ruleType":"FIXED_AMOUNT","fixedAmount":300,"fixedCurrency":"CZK"}
                                            """))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should pass correct EditRuleCommand from path variables and body")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldMapPathVariablesAndBodyToCommand() throws Exception {
            doNothing().when(managementPort).editRule(eq(LEVEL_ID), any());

            mockMvc.perform(
                            patch("/api/membership-fee-tiers/{id}/rules/{eventTypeId}/{ranking}",
                                    LEVEL_UUID, EVENT_TYPE_UUID, "B")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"ruleType":"PERCENTAGE","percentage":40}
                                            """))
                    .andExpect(status().isNoContent());

            var captor = forClass(MembershipFeeTierManagementPort.EditRuleCommand.class);
            verify(managementPort).editRule(eq(LEVEL_ID), captor.capture());
            MembershipFeeTierManagementPort.EditRuleCommand command = captor.getValue();

            assertThat(command.eventTypeId().value()).isEqualTo(EVENT_TYPE_UUID);
            assertThat(command.rankingShortName()).isEqualTo("B");
            assertThat(command.newValue()).isInstanceOf(MembershipPaymentRule.RuleValue.Percentage.class);
        }

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(
                            patch("/api/membership-fee-tiers/{id}/rules/{eventTypeId}/{ranking}",
                                    LEVEL_UUID, EVENT_TYPE_UUID, RANKING)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"ruleType":"PERCENTAGE","percentage":75}
                                            """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when rule with given key does not exist")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn404WhenRuleNotFound() throws Exception {
            doThrow(new com.klabis.membershipfees.domain.PaymentRuleNotFoundException(
                    com.klabis.membershipfees.domain.EventTypeReference.of(EVENT_TYPE_UUID), RANKING))
                    .when(managementPort).editRule(eq(LEVEL_ID), any());

            mockMvc.perform(
                            patch("/api/membership-fee-tiers/{id}/rules/{eventTypeId}/{ranking}",
                                    LEVEL_UUID, EVENT_TYPE_UUID, RANKING)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"ruleType":"PERCENTAGE","percentage":75}
                                            """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/membership-fee-tiers/{id}")
    class DeleteLevelTests {

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(
                            delete("/api/membership-fee-tiers/{id}", LEVEL_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 204 when delete is successful")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn204OnDelete() throws Exception {
            doNothing().when(managementPort).deleteTier(LEVEL_ID);

            mockMvc.perform(
                            delete("/api/membership-fee-tiers/{id}", LEVEL_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isNoContent());
        }
    }
}
