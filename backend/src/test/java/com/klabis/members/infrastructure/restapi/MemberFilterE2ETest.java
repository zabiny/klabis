package com.klabis.members.infrastructure.restapi;

import com.klabis.E2ETest;
import com.klabis.common.security.JwtParams;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.groups.application.LastOwnershipCheckerImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static com.klabis.common.security.JwtParams.member;
import static com.klabis.common.security.KlabisMvcRequestBuilders.klabisAuthentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests verifying member list filter combinations.
 * <p>
 * Exercises the {@code q} and {@code status} filters through the full MVC stack against the
 * dev-profile H2 DB. Slice tests alone would miss runtime bugs in the JDBC adapter's unaccent
 * SQL — the same class of issue found with the events filter.
 */
@E2ETest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/sql/test-members-filter-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Member Filter E2E Tests")
class MemberFilterE2ETest {

    @MockitoBean
    @SuppressWarnings("unused")
    private LastOwnershipCheckerImpl lastOwnershipCheckerImpl;

    @MockitoBean
    @SuppressWarnings("unused")
    private com.klabis.groups.traininggroup.domain.TrainingGroupRepository trainingGroupRepository;

    @MockitoBean
    @SuppressWarnings("unused")
    private com.klabis.groups.familygroup.domain.FamilyGroupRepository familyGroupRepository;

    @Autowired
    private MockMvc mockMvc;

    private static final UUID ADMIN_MEMBER_ID = UUID.fromString("00000001-0001-0001-0001-000000000099");

    @Test
    @DisplayName("q=novak returns only members whose name contains 'novak' (case-insensitive)")
    void shouldFilterByFulltextQuery() throws Exception {
        mockMvc.perform(
                        get("/api/members")
                                .param("q", "novak")
                                .param("status", "ALL")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(adminAuthentication())
                )
                .andDo(print())
                .andExpect(status().isOk())
                // Jan Novak, Karel Novak, Jana Nováková — all contain "novak" via unaccent
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    @DisplayName("q=cermak returns member with diacritics in last name (unaccent)")
    void shouldFilterByFulltextQueryWithDiacritics() throws Exception {
        mockMvc.perform(
                        get("/api/members")
                                .param("q", "cermak")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(adminAuthentication())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0].lastName").value("Čermáková"));
    }

    @Test
    @DisplayName("status=INACTIVE with MANAGE authority returns only inactive members")
    void shouldFilterByInactiveStatus() throws Exception {
        mockMvc.perform(
                        get("/api/members")
                                .param("status", "INACTIVE")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(adminAuthentication())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0].lastName").value("Nováková"));
    }

    @Test
    @DisplayName("status=ALL with MANAGE authority returns active and inactive members")
    void shouldReturnAllMembersWhenStatusIsAll() throws Exception {
        mockMvc.perform(
                        get("/api/members")
                                .param("status", "ALL")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(adminAuthentication())
                )
                .andDo(print())
                .andExpect(status().isOk())
                // Admin row + 3 active + 1 inactive = 5
                .andExpect(jsonPath("$.page.totalElements").value(5));
    }

    @Test
    @DisplayName("no status param returns only active members by default (even for MANAGE caller)")
    void shouldDefaultToActiveStatusForAdminWithNoStatusParam() throws Exception {
        mockMvc.perform(
                        get("/api/members")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(adminAuthentication())
                )
                .andDo(print())
                .andExpect(status().isOk())
                // Admin row + 3 active = 4
                .andExpect(jsonPath("$.page.totalElements").value(4));
    }

    @Test
    @DisplayName("non-MANAGE caller with status=INACTIVE is silently forced to ACTIVE — no error, active-only result")
    void shouldForceActiveStatusForNonManageCaller() throws Exception {
        mockMvc.perform(
                        get("/api/members")
                                .param("status", "INACTIVE")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(memberAuthentication())
                )
                .andDo(print())
                .andExpect(status().isOk())
                // Only active members visible to non-MANAGE caller
                .andExpect(jsonPath("$.page.totalElements").value(4));
    }

    @Test
    @DisplayName("q + status=INACTIVE returns intersection: inactive members matching the fulltext query")
    void shouldCombineFulltextAndStatusFilters() throws Exception {
        mockMvc.perform(
                        get("/api/members")
                                .param("q", "novak")
                                .param("status", "INACTIVE")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(adminAuthentication())
                )
                .andDo(print())
                .andExpect(status().isOk())
                // Only Jana Nováková is both inactive and matches "novak"
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0].lastName").value("Nováková"));
    }

    @Test
    @DisplayName("default sort: members with same lastName are ordered by firstName ASC")
    void shouldOrderByLastNameThenFirstNameAscByDefault() throws Exception {
        mockMvc.perform(
                        get("/api/members")
                                .param("q", "novak")
                                .param("status", "ACTIVE")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(adminAuthentication())
                )
                .andDo(print())
                .andExpect(status().isOk())
                // Jan Novak and Karel Novak — same lastName; Jan < Karel alphabetically
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0].firstName").value("Jan"))
                .andExpect(jsonPath("$._embedded.memberSummaryResponseList[1].firstName").value("Karel"));
    }

    @Test
    @DisplayName("invalid status value returns 400")
    void shouldReturn400ForInvalidStatusValue() throws Exception {
        mockMvc.perform(
                        get("/api/members")
                                .param("status", "GARBAGE")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .with(adminAuthentication())
                )
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Test helpers
    // -------------------------------------------------------------------------

    private RequestPostProcessor adminAuthentication() {
        return klabisAuthentication(
                member(ADMIN_MEMBER_ID)
                        .withAuthorities(Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE)
        );
    }

    private RequestPostProcessor memberAuthentication() {
        return klabisAuthentication(
                JwtParams.jwtTokenParams("ZBM0001", UserId.fromString("00000001-0001-0001-0001-000000000001"))
                        .withAuthorities(Authority.MEMBERS_READ)
        );
    }
}
