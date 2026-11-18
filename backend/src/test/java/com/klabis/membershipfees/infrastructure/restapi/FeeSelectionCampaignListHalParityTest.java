package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.HalShape;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeTypeId;
import com.klabis.membershipfees.MembershipFeesWebMvcTest;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.application.MembershipFeeTypeManagementPort;
import com.klabis.membershipfees.domain.MembershipFeeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Fee selection campaign list HAL parity (publishYear template)")
@MembershipFeesWebMvcTest
class FeeSelectionCampaignListHalParityTest {

    private static final UUID TYPE_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID TYPE_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FeeSelectionCampaignManagementPort campaignManagement;

    @Autowired
    private MembershipFeeTypeManagementPort typeManagement;

    private static MembershipFeeType type(UUID id, String name) {
        return MembershipFeeType.reconstruct(new MembershipFeeTypeId(id), name,
                Money.ofCzk(new BigDecimal("1200.00")), List.of(), null);
    }

    @Test
    @DisplayName("admin gets self link carrying publishYear with one inline option per type, and nothing else")
    @WithKlabisMockUser(memberId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", authorities = {Authority.MEMBERS_MANAGE})
    void adminSeesPublishYearWithTypeOptions() throws Exception {
        when(campaignManagement.listPublications(any())).thenReturn(List.of());
        when(typeManagement.listFeeTypes()).thenReturn(List.of(type(TYPE_A, "Závodní"), type(TYPE_B, "Rekreační")));

        String body = mockMvc.perform(get("/api/fee-selection-campaigns").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.publishYear.properties[?(@.name=='typeIds')].options.inline.length()")
                        .value(2))
                .andExpect(jsonPath("$._templates.publishYear.properties[?(@.name=='typeIds')].options.inline[0].value")
                        .value(TYPE_A.toString()))
                .andExpect(jsonPath("$._templates.publishYear.properties[?(@.name=='typeIds')].options.inline[0].prompt")
                        .value("Závodní"))
                .andExpect(jsonPath("$._templates.publishYear.properties[?(@.name=='typeIds')].options.inline[1].value")
                        .value(TYPE_B.toString()))
                .andExpect(jsonPath("$._templates.publishYear.properties[?(@.name=='typeIds')].options.inline[1].prompt")
                        .value("Rekreační"))
                .andReturn().getResponse().getContentAsString();

        assertThat(HalShape.of(body, "$")).isEqualTo(List.of("link self -> /api/fee-selection-campaigns", "template publishYear POST /api/fee-selection-campaigns [typeIds, votingDeadline, year]"));
    }

    @Test
    @DisplayName("user without MEMBERS:MANAGE gets no publishYear template")
    @WithKlabisMockUser(memberId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    void nonAdminSeesNoPublishYear() throws Exception {
        when(campaignManagement.listPublications(any())).thenReturn(List.of());
        when(typeManagement.listFeeTypes()).thenReturn(List.of(type(TYPE_A, "Závodní")));

        String body = mockMvc.perform(get("/api/fee-selection-campaigns").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(HalShape.of(body, "$")).isEqualTo(List.of("link self -> /api/fee-selection-campaigns"));
    }
}
