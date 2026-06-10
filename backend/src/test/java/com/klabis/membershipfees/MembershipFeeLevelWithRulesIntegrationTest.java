package com.klabis.membershipfees;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.membershipfees.application.MembershipFeeLevelManagementPort;
import com.klabis.membershipfees.domain.MembershipFeeLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {TestApplicationConfiguration.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@CleanupTestData
@DisplayName("MembershipFeeLevel with rules — full-stack integration")
class MembershipFeeLevelWithRulesIntegrationTest {

    private static final String ADMIN_UUID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String EVENT_TYPE_UUID = "e2be588c-91ad-43e4-8d14-efa7de02782d";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MembershipFeeLevelManagementPort managementPort;

    @Test
    @WithKlabisMockUser(memberId = ADMIN_UUID, authorities = {Authority.MEMBERS_MANAGE})
    @DisplayName("POST /api/membership-fee-levels with PERCENTAGE rule should return 201")
    void shouldCreateLevelWithPercentageRuleAndReturn201() throws Exception {
        mockMvc.perform(post("/api/membership-fee-levels")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                        .content("""
                                {"name":"Závodní","rules":[{"eventTypeId":"%s","rankingShortName":"A","ruleType":"PERCENTAGE","percent":30}],"yearlyFeeAmount":1200,"yearlyFeeCurrency":"CZK"}
                                """.formatted(EVENT_TYPE_UUID)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithKlabisMockUser(memberId = ADMIN_UUID, authorities = {Authority.MEMBERS_MANAGE})
    @DisplayName("POST /api/membership-fee-levels with FIXED_AMOUNT rule should return 201")
    void shouldCreateLevelWithFixedAmountRuleAndReturn201() throws Exception {
        mockMvc.perform(post("/api/membership-fee-levels")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                        .content("""
                                {"name":"Mládež","rules":[{"eventTypeId":"%s","rankingShortName":"B","ruleType":"FIXED_AMOUNT","fixedAmount":200,"fixedCurrency":"CZK"}],"yearlyFeeAmount":800,"yearlyFeeCurrency":"CZK"}
                                """.formatted(EVENT_TYPE_UUID)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithKlabisMockUser(memberId = ADMIN_UUID, authorities = {Authority.MEMBERS_MANAGE})
    @DisplayName("PATCH /api/membership-fee-levels/{id} with rules should return 204 and persist correct ruleType")
    void shouldEditLevelWithRulesAndReturn204() throws Exception {
        var createResult = mockMvc.perform(post("/api/membership-fee-levels")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                        .content("""
                                {"name":"Závodní","yearlyFeeAmount":1200,"yearlyFeeCurrency":"CZK"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String location = createResult.getResponse().getHeader("Location");
        String levelUuid = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(patch("/api/membership-fee-levels/{id}", levelUuid)
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                        .content("""
                                {"rules":[{"eventTypeId":"%s","rankingShortName":"A","ruleType":"PERCENTAGE","percent":30}]}
                                """.formatted(EVENT_TYPE_UUID)))
                .andExpect(status().isNoContent());

        var levelId = new com.klabis.membershipfees.MembershipFeeLevelId(java.util.UUID.fromString(levelUuid));
        MembershipFeeLevel saved = managementPort.getLevel(levelId);
        assertThat(saved.getRules()).hasSize(1);
        assertThat(saved.getRules().get(0).value())
                .isInstanceOf(com.klabis.membershipfees.domain.MembershipPaymentRule.RuleValue.Percentage.class);
    }
}
