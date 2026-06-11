package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.FeeSelectionCampaignId;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.application.AdminFeeAssignmentPort;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.application.FeeSelectionCampaignNotFoundException;
import com.klabis.membershipfees.application.MembershipFeeTierManagementPort;
import com.klabis.membershipfees.domain.FeeSelectionCampaign;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("FeeSelectionCampaignController API tests")
@WebMvcTest(controllers = {FeeSelectionCampaignController.class, MembershipFeeGroupController.class})
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
@WithPostprocessors
class FeeSelectionCampaignControllerTest {

    private static final String MEMBER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final UUID PUBLICATION_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final FeeSelectionCampaignId PUBLICATION_ID = new FeeSelectionCampaignId(PUBLICATION_UUID);
    private static final UUID GROUP_UUID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeeSelectionCampaignManagementPort managementPort;

    @MockitoBean
    private MembershipFeeTierManagementPort levelManagementPort;

    @MockitoBean
    private AdminFeeAssignmentPort adminFeeAssignmentPort;

    private FeeSelectionCampaign buildPublication(UUID id, int year) {
        return FeeSelectionCampaign.reconstruct(
                new FeeSelectionCampaignId(id),
                year,
                LocalDate.of(year, 3, 31),
                null,
                List.of(new MembershipFeeGroupId(GROUP_UUID)));
    }

    private MembershipFeeGroup buildGroup(UUID id, int year) {
        return MembershipFeeGroup.reconstruct(
                new MembershipFeeGroupId(id),
                new MembershipFeeTierId(UUID.randomUUID()),
                "Dospělý", year, LocalDate.of(year, 3, 31),
                Money.ofCzk(new BigDecimal("1200.00")),
                com.klabis.membershipfees.domain.PublishedLevelStatus.EDITABLE,
                List.of(), java.util.Set.of(), null);
    }

    @Nested
    @DisplayName("POST /api/fee-selection-campaigns")
    class PublishYearTests {

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(
                            post("/api/fee-selection-campaigns")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"year": 2026, "votingDeadline": "2026-03-31", "levelIds": ["%s"]}
                                            """.formatted(UUID.randomUUID())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 201 with Location header when user has MEMBERS:MANAGE")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldPublishYearAndReturn201() throws Exception {
            when(managementPort.publishYear(any())).thenReturn(PUBLICATION_ID);

            mockMvc.perform(
                            post("/api/fee-selection-campaigns")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"year": 2026, "votingDeadline": "2026-03-31", "levelIds": ["%s"]}
                                            """.formatted(UUID.randomUUID())))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location",
                            org.hamcrest.Matchers.endsWith("/api/fee-selection-campaigns/" + PUBLICATION_UUID)));
        }

        @Test
        @DisplayName("should return 400 when levelIds is empty")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WhenLevelIdsIsEmpty() throws Exception {
            mockMvc.perform(
                            post("/api/fee-selection-campaigns")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"year": 2026, "votingDeadline": "2026-03-31", "levelIds": []}
                                            """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/fee-selection-campaigns")
    class ListPublicationsTests {

        @Test
        @DisplayName("should return 200 with list of publications")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturnListOfPublications() throws Exception {
            when(managementPort.listPublications()).thenReturn(List.of(
                    buildPublication(PUBLICATION_UUID, 2026),
                    buildPublication(UUID.randomUUID(), 2025)
            ));

            mockMvc.perform(
                            get("/api/fee-selection-campaigns")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(jsonPath("$._embedded.feeSelectionCampaignResponseList").isArray())
                    .andExpect(jsonPath("$._embedded.feeSelectionCampaignResponseList.length()").value(2));
        }

        @Test
        @DisplayName("should include publishYear affordance for MEMBERS:MANAGE user")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldIncludePublishAffordance() throws Exception {
            when(managementPort.listPublications()).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/fee-selection-campaigns")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.publishYear").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/fee-selection-campaigns/{id}")
    class GetPublicationTests {

        @Test
        @DisplayName("should return 200 with publication details")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturnPublicationDetails() throws Exception {
            when(managementPort.getPublication(PUBLICATION_ID))
                    .thenReturn(buildPublication(PUBLICATION_UUID, 2026));

            mockMvc.perform(
                            get("/api/fee-selection-campaigns/{id}", PUBLICATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.year").value(2026))
                    .andExpect(jsonPath("$.votingDeadline").value("2026-03-31"));
        }

        @Test
        @DisplayName("should return 400 when publication not found")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn400WhenNotFound() throws Exception {
            when(managementPort.getPublication(PUBLICATION_ID))
                    .thenThrow(new FeeSelectionCampaignNotFoundException(PUBLICATION_ID));

            mockMvc.perform(
                            get("/api/fee-selection-campaigns/{id}", PUBLICATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/fee-selection-campaigns/{year}/levels")
    class ListGroupsForYearTests {

        @Test
        @DisplayName("should return 200 with list of groups for a year")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturnGroupsForYear() throws Exception {
            when(managementPort.listGroupsForYear(2026))
                    .thenReturn(List.of(buildGroup(GROUP_UUID, 2026)));

            mockMvc.perform(
                            get("/api/fee-selection-campaigns/{year}/levels", 2026)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.membershipFeeGroupResponseList").isArray())
                    .andExpect(jsonPath("$._embedded.membershipFeeGroupResponseList.length()").value(1));
        }
    }
}
