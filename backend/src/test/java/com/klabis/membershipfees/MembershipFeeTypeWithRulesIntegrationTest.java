package com.klabis.membershipfees;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.membershipfees.application.MembershipFeeTypeManagementPort;
import com.klabis.membershipfees.domain.MembershipFeeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("MembershipFeeType with rules — full-stack integration")
class MembershipFeeTypeWithRulesIntegrationTest {

    private static final String ADMIN_UUID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String EVENT_TYPE_UUID = "e2be588c-91ad-43e4-8d14-efa7de02782d";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MembershipFeeTypeManagementPort managementPort;

    @Test
    @WithKlabisMockUser(memberId = ADMIN_UUID, authorities = {Authority.MEMBERS_MANAGE})
    @DisplayName("POST /api/membership-fee-types should create type with empty rules list")
    void shouldCreateTypeWithEmptyRulesAndReturn201() throws Exception {
        var createResult = mockMvc.perform(post("/api/membership-fee-types")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                        .content("""
                                {"name":"Závodní","yearlyFeeAmount":1200,"yearlyFeeCurrency":"CZK"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String location = createResult.getResponse().getHeader("Location");
        String typeUuid = location.substring(location.lastIndexOf('/') + 1);

        var typeId = new MembershipFeeTypeId(java.util.UUID.fromString(typeUuid));
        MembershipFeeType created = managementPort.getFeeType(typeId);
        assertThat(created.getRules()).isEmpty();
    }

    @Test
    @WithKlabisMockUser(memberId = ADMIN_UUID, authorities = {Authority.MEMBERS_MANAGE})
    @DisplayName("PATCH /api/membership-fee-types/{id} should update name and yearlyFee")
    void shouldEditTypeNameAndReturn204() throws Exception {
        var createResult = mockMvc.perform(post("/api/membership-fee-types")
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                        .content("""
                                {"name":"Závodní","yearlyFeeAmount":1200,"yearlyFeeCurrency":"CZK"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String location = createResult.getResponse().getHeader("Location");
        String typeUuid = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(patch("/api/membership-fee-types/{id}", typeUuid)
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                        .content("""
                                {"name":"Updated Name"}
                                """))
                .andExpect(status().isNoContent());

        var typeId = new MembershipFeeTypeId(java.util.UUID.fromString(typeUuid));
        MembershipFeeType saved = managementPort.getFeeType(typeId);
        assertThat(saved.getName()).isEqualTo("Updated Name");
    }
}
