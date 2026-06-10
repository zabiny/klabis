package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.membershipfees.application.AdminFeeAssignmentPort;
import com.klabis.membershipfees.application.FeeYearPublicationManagementPort;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.PublishedLevelStatus;
import com.klabis.membershipfees.domain.SnapshotFrozenException;
import com.klabis.finance.domain.Money;
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
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("MembershipFeeGroupController API tests")
@WebMvcTest(controllers = MembershipFeeGroupController.class)
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
@WithPostprocessors
class MembershipFeeGroupControllerTest {

    private static final UUID GROUP_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID MEMBER_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final String ADMIN_MEMBER_ID = "00000000-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeeYearPublicationManagementPort managementPort;

    @MockitoBean
    private AdminFeeAssignmentPort adminFeeAssignmentPort;

    private static final java.time.LocalDate VOTING_DEADLINE = java.time.LocalDate.of(2026, 3, 31);

    private MembershipFeeGroup buildFrozenGroup() {
        return MembershipFeeGroup.reconstruct(
                new MembershipFeeGroupId(GROUP_UUID),
                new MembershipFeeLevelId(UUID.randomUUID()),
                "Dospělý", 2026, VOTING_DEADLINE,
                Money.ofCzk(new BigDecimal("1200.00")),
                PublishedLevelStatus.FROZEN,
                List.of(), Set.of(), null);
    }

    private MembershipFeeGroup buildEditableGroup() {
        return MembershipFeeGroup.reconstruct(
                new MembershipFeeGroupId(GROUP_UUID),
                new MembershipFeeLevelId(UUID.randomUUID()),
                "Dospělý", 2026, VOTING_DEADLINE,
                Money.ofCzk(new BigDecimal("1200.00")),
                PublishedLevelStatus.EDITABLE,
                List.of(), Set.of(), null);
    }

    @Nested
    @DisplayName("GET /api/membership-fee-groups/{id}")
    class GetGroupTests {

        @Test
        @DisplayName("should return 200 with group details")
        @WithKlabisMockUser
        void shouldReturnGroupDetails() throws Exception {
            MembershipFeeGroup group = buildFrozenGroup();
            org.mockito.Mockito.when(managementPort.getGroup(new MembershipFeeGroupId(GROUP_UUID)))
                    .thenReturn(group);

            mockMvc.perform(
                            get("/api/membership-fee-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Dospělý"))
                    .andExpect(jsonPath("$.year").value(2026));
        }
    }

    @Nested
    @DisplayName("POST /api/membership-fee-groups/{groupId}/members/{memberId} — admin assignment")
    class AdminAssignTests {

        @Test
        @DisplayName("should return 204 when admin assigns a member to a group")
        @WithKlabisMockUser(memberId = ADMIN_MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn204WhenAdminAssigns() throws Exception {
            doNothing().when(adminFeeAssignmentPort).assignLevel(any());

            mockMvc.perform(
                            post("/api/membership-fee-groups/{groupId}/members/{memberId}", GROUP_UUID, MEMBER_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"year": 2026}
                                            """))
                    .andExpect(status().isNoContent());

            verify(adminFeeAssignmentPort).assignLevel(any(AdminFeeAssignmentPort.AssignFeeLevel.class));
        }

        @Test
        @DisplayName("should return 403 when user does not have MEMBERS_MANAGE authority")
        @WithKlabisMockUser(memberId = ADMIN_MEMBER_ID)
        void shouldReturn403WhenNotAdmin() throws Exception {
            mockMvc.perform(
                            post("/api/membership-fee-groups/{groupId}/members/{memberId}", GROUP_UUID, MEMBER_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"year": 2026}
                                            """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 400 when year is missing from request body")
        @WithKlabisMockUser(memberId = ADMIN_MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WhenYearMissing() throws Exception {
            mockMvc.perform(
                            post("/api/membership-fee-groups/{groupId}/members/{memberId}", GROUP_UUID, MEMBER_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET should include admin-assign affordance on group detail")
        @WithKlabisMockUser(memberId = ADMIN_MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldIncludeAdminAssignAffordanceForAdmin() throws Exception {
            MembershipFeeGroup group = buildFrozenGroup();
            org.mockito.Mockito.when(managementPort.getGroup(new MembershipFeeGroupId(GROUP_UUID)))
                    .thenReturn(group);

            mockMvc.perform(
                            get("/api/membership-fee-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.assignMember").exists());
        }
    }

    @Nested
    @DisplayName("PATCH /api/membership-fee-groups/{id} — edit snapshot")
    class EditSnapshotTests {

        private static final String EDIT_BODY = """
                {"yearlyFeeAmount": 1500.00, "yearlyFeeCurrency": "CZK", "rules": []}
                """;

        @Test
        @DisplayName("should return 204 when admin edits an EDITABLE group")
        @WithKlabisMockUser(memberId = ADMIN_MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn204WhenEditable() throws Exception {
            doNothing().when(managementPort).editGroupSnapshot(any(), any());

            mockMvc.perform(
                            patch("/api/membership-fee-groups/{id}", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(EDIT_BODY))
                    .andExpect(status().isNoContent());

            verify(managementPort).editGroupSnapshot(
                    eq(new MembershipFeeGroupId(GROUP_UUID)),
                    any(FeeYearPublicationManagementPort.EditGroupSnapshotCommand.class));
        }

        @Test
        @DisplayName("should return 400 when group is FROZEN (SnapshotFrozenException)")
        @WithKlabisMockUser(memberId = ADMIN_MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WhenFrozen() throws Exception {
            doThrow(new SnapshotFrozenException())
                    .when(managementPort).editGroupSnapshot(any(), any());

            mockMvc.perform(
                            patch("/api/membership-fee-groups/{id}", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(EDIT_BODY))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when user does not have MEMBERS_MANAGE authority")
        @WithKlabisMockUser(memberId = ADMIN_MEMBER_ID)
        void shouldReturn403WhenNotAdmin() throws Exception {
            mockMvc.perform(
                            patch("/api/membership-fee-groups/{id}", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(EDIT_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET should include editSnapshot affordance only for EDITABLE groups")
        @WithKlabisMockUser(memberId = ADMIN_MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldIncludeEditAffordanceOnlyForEditable() throws Exception {
            MembershipFeeGroup editableGroup = buildEditableGroup();
            org.mockito.Mockito.when(managementPort.getGroup(new MembershipFeeGroupId(GROUP_UUID)))
                    .thenReturn(editableGroup);

            mockMvc.perform(
                            get("/api/membership-fee-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.editSnapshot").exists());
        }

        @Test
        @DisplayName("GET should NOT include editSnapshot affordance for FROZEN groups")
        @WithKlabisMockUser(memberId = ADMIN_MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldNotIncludeEditAffordanceForFrozen() throws Exception {
            MembershipFeeGroup frozenGroup = buildFrozenGroup();
            org.mockito.Mockito.when(managementPort.getGroup(new MembershipFeeGroupId(GROUP_UUID)))
                    .thenReturn(frozenGroup);

            mockMvc.perform(
                            get("/api/membership-fee-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.editSnapshot").doesNotExist());
        }
    }
}
