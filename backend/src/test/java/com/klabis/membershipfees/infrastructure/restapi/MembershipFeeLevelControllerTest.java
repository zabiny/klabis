package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.membershipfees.application.MembershipFeeLevelManagementPort;
import com.klabis.membershipfees.application.MembershipFeeLevelNotFoundException;
import com.klabis.membershipfees.domain.MembershipFeeLevel;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("MembershipFeeLevelController API tests")
@WebMvcTest(controllers = {MembershipFeeLevelController.class})
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
@WithPostprocessors
class MembershipFeeLevelControllerTest {

    private static final String MEMBER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final UUID LEVEL_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final MembershipFeeLevelId LEVEL_ID = new MembershipFeeLevelId(LEVEL_UUID);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MembershipFeeLevelManagementPort managementPort;

    private MembershipFeeLevel buildLevel(UUID id, String name) {
        return MembershipFeeLevel.reconstruct(
                new MembershipFeeLevelId(id), name,
                Money.ofCzk(new BigDecimal("1200.00")), List.of(), null);
    }

    @Nested
    @DisplayName("POST /api/membership-fee-levels")
    class CreateLevelTests {

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(
                            post("/api/membership-fee-levels")
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
            when(managementPort.createLevel(any()))
                    .thenReturn(LEVEL_ID);

            mockMvc.perform(
                            post("/api/membership-fee-levels")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Dospělý", "yearlyFeeAmount": 1200}
                                            """))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location",
                            org.hamcrest.Matchers.endsWith("/api/membership-fee-levels/" + LEVEL_UUID)));
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WhenNameIsBlank() throws Exception {
            mockMvc.perform(
                            post("/api/membership-fee-levels")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "", "yearlyFeeAmount": 1200}
                                            """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/membership-fee-levels")
    class ListLevelsTests {

        @Test
        @DisplayName("should return 200 with list of levels")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturnListOfLevels() throws Exception {
            when(managementPort.listLevels()).thenReturn(List.of(
                    buildLevel(LEVEL_UUID, "Dospělý"),
                    buildLevel(UUID.randomUUID(), "Mládež")
            ));

            mockMvc.perform(
                            get("/api/membership-fee-levels")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(jsonPath("$._embedded.membershipFeeLevelSummaryResponseList").isArray())
                    .andExpect(jsonPath("$._embedded.membershipFeeLevelSummaryResponseList.length()").value(2));
        }

        @Test
        @DisplayName("should return 200 with empty list when no levels exist")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturnEmptyList() throws Exception {
            when(managementPort.listLevels()).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/membership-fee-levels")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should include create affordance for MEMBERS:MANAGE user")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldIncludeCreateAffordance() throws Exception {
            when(managementPort.listLevels()).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/membership-fee-levels")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.createLevel").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/membership-fee-levels/{id}")
    class GetLevelTests {

        @Test
        @DisplayName("should return 200 with level details")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturnLevelDetails() throws Exception {
            when(managementPort.getLevel(LEVEL_ID))
                    .thenReturn(buildLevel(LEVEL_UUID, "Dospělý"));

            mockMvc.perform(
                            get("/api/membership-fee-levels/{id}", LEVEL_UUID)
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
            when(managementPort.getLevel(LEVEL_ID))
                    .thenThrow(new MembershipFeeLevelNotFoundException(LEVEL_ID));

            mockMvc.perform(
                            get("/api/membership-fee-levels/{id}", LEVEL_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should include self link and edit/delete affordances for MEMBERS:MANAGE user")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldIncludeAffordancesForAdmin() throws Exception {
            when(managementPort.getLevel(LEVEL_ID))
                    .thenReturn(buildLevel(LEVEL_UUID, "Dospělý"));

            mockMvc.perform(
                            get("/api/membership-fee-levels/{id}", LEVEL_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.collection.href").exists());
        }
    }

    @Nested
    @DisplayName("PATCH /api/membership-fee-levels/{id}")
    class EditLevelTests {

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(
                            patch("/api/membership-fee-levels/{id}", LEVEL_UUID)
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
            doNothing().when(managementPort).editLevel(eq(LEVEL_ID), any());

            mockMvc.perform(
                            patch("/api/membership-fee-levels/{id}", LEVEL_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Updated Name"}
                                            """))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("DELETE /api/membership-fee-levels/{id}")
    class DeleteLevelTests {

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(
                            delete("/api/membership-fee-levels/{id}", LEVEL_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 204 when delete is successful")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn204OnDelete() throws Exception {
            doNothing().when(managementPort).deleteLevel(LEVEL_ID);

            mockMvc.perform(
                            delete("/api/membership-fee-levels/{id}", LEVEL_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isNoContent());
        }
    }
}
