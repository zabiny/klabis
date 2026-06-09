package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.membershipfees.FeeYearPublicationId;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.membershipfees.application.AdminFeeAssignmentPort;
import com.klabis.membershipfees.application.FeeYearPublicationManagementPort;
import com.klabis.membershipfees.application.FeeYearPublicationNotFoundException;
import com.klabis.membershipfees.application.MembershipFeeLevelManagementPort;
import com.klabis.membershipfees.domain.FeeYearPublication;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("FeeYearPublicationController API tests")
@WebMvcTest(controllers = {FeeYearPublicationController.class, MembershipFeeGroupController.class})
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
@WithPostprocessors
class FeeYearPublicationControllerTest {

    private static final String MEMBER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final UUID PUBLICATION_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final FeeYearPublicationId PUBLICATION_ID = new FeeYearPublicationId(PUBLICATION_UUID);
    private static final UUID GROUP_UUID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeeYearPublicationManagementPort managementPort;

    @MockitoBean
    private MembershipFeeLevelManagementPort levelManagementPort;

    @MockitoBean
    private AdminFeeAssignmentPort adminFeeAssignmentPort;

    private FeeYearPublication buildPublication(UUID id, int year) {
        return FeeYearPublication.reconstruct(
                new FeeYearPublicationId(id),
                year,
                LocalDate.of(year, 3, 31),
                null,
                List.of(new MembershipFeeGroupId(GROUP_UUID)));
    }

    private MembershipFeeGroup buildGroup(UUID id, int year) {
        return MembershipFeeGroup.reconstruct(
                new MembershipFeeGroupId(id),
                new MembershipFeeLevelId(UUID.randomUUID()),
                "Dospělý", year,
                Money.ofCzk(new BigDecimal("1200.00")),
                com.klabis.membershipfees.domain.PublishedLevelStatus.EDITABLE,
                List.of(), java.util.Set.of(), null);
    }

    @Nested
    @DisplayName("POST /api/fee-year-publications")
    class PublishYearTests {

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(
                            post("/api/fee-year-publications")
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
                            post("/api/fee-year-publications")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"year": 2026, "votingDeadline": "2026-03-31", "levelIds": ["%s"]}
                                            """.formatted(UUID.randomUUID())))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location",
                            org.hamcrest.Matchers.endsWith("/api/fee-year-publications/" + PUBLICATION_UUID)));
        }

        @Test
        @DisplayName("should return 400 when levelIds is empty")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WhenLevelIdsIsEmpty() throws Exception {
            mockMvc.perform(
                            post("/api/fee-year-publications")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"year": 2026, "votingDeadline": "2026-03-31", "levelIds": []}
                                            """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/fee-year-publications")
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
                            get("/api/fee-year-publications")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(jsonPath("$._embedded.feeYearPublicationResponseList").isArray())
                    .andExpect(jsonPath("$._embedded.feeYearPublicationResponseList.length()").value(2));
        }

        @Test
        @DisplayName("should include publishYear affordance for MEMBERS:MANAGE user")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldIncludePublishAffordance() throws Exception {
            when(managementPort.listPublications()).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/fee-year-publications")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.publishYear").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/fee-year-publications/{id}")
    class GetPublicationTests {

        @Test
        @DisplayName("should return 200 with publication details")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturnPublicationDetails() throws Exception {
            when(managementPort.getPublication(PUBLICATION_ID))
                    .thenReturn(buildPublication(PUBLICATION_UUID, 2026));

            mockMvc.perform(
                            get("/api/fee-year-publications/{id}", PUBLICATION_UUID)
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
                    .thenThrow(new FeeYearPublicationNotFoundException(PUBLICATION_ID));

            mockMvc.perform(
                            get("/api/fee-year-publications/{id}", PUBLICATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/fee-year-publications/{year}/levels")
    class ListGroupsForYearTests {

        @Test
        @DisplayName("should return 200 with list of groups for a year")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturnGroupsForYear() throws Exception {
            when(managementPort.listGroupsForYear(2026))
                    .thenReturn(List.of(buildGroup(GROUP_UUID, 2026)));

            mockMvc.perform(
                            get("/api/fee-year-publications/{year}/levels", 2026)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.membershipFeeGroupResponseList").isArray())
                    .andExpect(jsonPath("$._embedded.membershipFeeGroupResponseList.length()").value(1));
        }
    }
}
