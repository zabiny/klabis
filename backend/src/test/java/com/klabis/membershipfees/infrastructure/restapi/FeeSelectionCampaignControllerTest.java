package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.finance.domain.Money;
import com.klabis.members.Members;
import com.klabis.membershipfees.FeeSelectionCampaignId;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.application.AdminFeeAssignmentPort;
import com.klabis.membershipfees.application.CampaignAlreadyProcessedException;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.application.FeeSelectionCampaignNotFoundException;
import com.klabis.membershipfees.application.ManualCampaignClosePort;
import com.klabis.membershipfees.application.MembershipFeeTierManagementPort;
import com.klabis.membershipfees.domain.CampaignClosedException;
import com.klabis.membershipfees.domain.DeadlineNotInFutureException;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @MockitoBean
    private ManualCampaignClosePort manualCampaignClosePort;

    @MockitoBean
    private Members members;

    private FeeSelectionCampaign buildPublication(UUID id, int year) {
        return FeeSelectionCampaign.reconstruct(
                new FeeSelectionCampaignId(id),
                year,
                LocalDate.of(year, 3, 31),
                null,
                List.of(new MembershipFeeGroupId(GROUP_UUID)));
    }

    private FeeSelectionCampaign buildActiveCampaign(UUID id) {
        return FeeSelectionCampaign.reconstruct(
                new FeeSelectionCampaignId(id),
                2099,
                LocalDate.of(2099, 12, 31),
                null,
                List.of(new MembershipFeeGroupId(GROUP_UUID)));
    }

    private FeeSelectionCampaign buildClosedCampaign(UUID id) {
        return FeeSelectionCampaign.reconstruct(
                new FeeSelectionCampaignId(id),
                2020,
                LocalDate.of(2020, 3, 31),
                Instant.parse("2020-04-01T00:00:00Z"),
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

        @Test
        @DisplayName("should return only closed campaigns when status=closed is provided")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturnOnlyClosedCampaignsWhenStatusClosed() throws Exception {
            when(managementPort.listClosedPublications()).thenReturn(List.of(
                    buildClosedCampaign(PUBLICATION_UUID)
            ));

            mockMvc.perform(
                            get("/api/fee-selection-campaigns")
                                    .param("status", "closed")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(jsonPath("$._embedded.feeSelectionCampaignResponseList").isArray())
                    .andExpect(jsonPath("$._embedded.feeSelectionCampaignResponseList.length()").value(1));
        }

        @Test
        @DisplayName("should return all campaigns when no status parameter is provided")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturnAllCampaignsWithoutStatusParam() throws Exception {
            when(managementPort.listPublications()).thenReturn(List.of(
                    buildActiveCampaign(UUID.randomUUID()),
                    buildClosedCampaign(PUBLICATION_UUID)
            ));

            mockMvc.perform(
                            get("/api/fee-selection-campaigns")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.feeSelectionCampaignResponseList.length()").value(2));
        }

        @Test
        @DisplayName("each closed campaign in status=closed response should carry a self link")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void eachClosedCampaignShouldHaveSelfLink() throws Exception {
            when(managementPort.listClosedPublications()).thenReturn(List.of(
                    buildClosedCampaign(PUBLICATION_UUID)
            ));

            mockMvc.perform(
                            get("/api/fee-selection-campaigns")
                                    .param("status", "closed")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.feeSelectionCampaignResponseList[0]._links.self.href")
                            .value(org.hamcrest.Matchers.endsWith("/api/fee-selection-campaigns/" + PUBLICATION_UUID)));
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

    @Nested
    @DisplayName("PATCH /api/fee-selection-campaigns/{id}/deadline")
    class ChangeDeadlineTests {

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(
                            patch("/api/fee-selection-campaigns/{id}/deadline", PUBLICATION_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"votingDeadline": "2099-12-31"}
                                            """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 200 with updated campaign when deadline changed successfully")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn200WithUpdatedCampaign() throws Exception {
            FeeSelectionCampaign activeCampaign = buildActiveCampaign(PUBLICATION_UUID);
            when(managementPort.changeDeadline(eq(PUBLICATION_ID), any())).thenReturn(activeCampaign);

            mockMvc.perform(
                            patch("/api/fee-selection-campaigns/{id}/deadline", PUBLICATION_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"votingDeadline": "2099-12-31"}
                                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.year").value(2099))
                    .andExpect(jsonPath("$.votingDeadline").value("2099-12-31"));
        }

        @Test
        @DisplayName("should return 400 when deadline is in the past (DeadlineNotInFutureException)")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WhenDeadlineInPast() throws Exception {
            doThrow(new DeadlineNotInFutureException(LocalDate.of(2020, 1, 1)))
                    .when(managementPort).changeDeadline(eq(PUBLICATION_ID), any());

            mockMvc.perform(
                            patch("/api/fee-selection-campaigns/{id}/deadline", PUBLICATION_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"votingDeadline": "2020-01-01"}
                                            """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 409 when campaign is closed (CampaignClosedException)")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn409WhenCampaignClosed() throws Exception {
            doThrow(new CampaignClosedException())
                    .when(managementPort).changeDeadline(eq(PUBLICATION_ID), any());

            mockMvc.perform(
                            patch("/api/fee-selection-campaigns/{id}/deadline", PUBLICATION_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"votingDeadline": "2020-01-01"}
                                            """))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should include changeDeadline affordance for active campaign")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldIncludeChangeDeadlineAffordanceForActiveCampaign() throws Exception {
            when(managementPort.getPublication(PUBLICATION_ID))
                    .thenReturn(buildActiveCampaign(PUBLICATION_UUID));

            mockMvc.perform(
                            get("/api/fee-selection-campaigns/{id}", PUBLICATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.changeDeadline").exists());
        }

        @Test
        @DisplayName("should NOT include changeDeadline affordance for closed campaign")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldNotIncludeChangeDeadlineAffordanceForClosedCampaign() throws Exception {
            when(managementPort.getPublication(PUBLICATION_ID))
                    .thenReturn(buildClosedCampaign(PUBLICATION_UUID));

            mockMvc.perform(
                            get("/api/fee-selection-campaigns/{id}", PUBLICATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.changeDeadline").doesNotExist());
        }
    }

    @Nested
    @DisplayName("POST /api/fee-selection-campaigns/{id}/close")
    class CloseCampaignTests {

        @Test
        @DisplayName("should return 204 when campaign closed successfully with MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn204WhenClosedSuccessfully() throws Exception {
            mockMvc.perform(
                            post("/api/fee-selection-campaigns/{id}/close", PUBLICATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 409 when campaign was already processed (CampaignAlreadyProcessedException)")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn409WhenAlreadyProcessed() throws Exception {
            doThrow(new CampaignAlreadyProcessedException())
                    .when(manualCampaignClosePort).closeCampaign(PUBLICATION_ID);

            mockMvc.perform(
                            post("/api/fee-selection-campaigns/{id}/close", PUBLICATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(
                            post("/api/fee-selection-campaigns/{id}/close", PUBLICATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
        }
    }
}
