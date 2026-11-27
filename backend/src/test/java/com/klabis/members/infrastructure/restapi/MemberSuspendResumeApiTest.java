package com.klabis.members.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.MembersWebMvcTest;
import com.klabis.members.MonetaryAmount;
import com.klabis.members.OwnedGroup;
import com.klabis.members.application.MemberFinancialStatePort;
import com.klabis.members.application.NotSoleGuardianGroupException;
import com.klabis.members.application.SuspensionBlockedException;
import com.klabis.members.domain.DeactivationReason;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberAlreadySuspendedException;
import com.klabis.members.domain.MemberResumeMembershipBuilder;
import com.klabis.members.domain.MemberSuspendMembershipBuilder;
import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("POST /api/members/{id}/suspend and /resume")
@MembersWebMvcTest
class MemberSuspendResumeApiTest extends MemberApiTestBase {

    private MockHttpServletRequestBuilder postMemberIdSuspend(UUID memberId) {
        return post("/api/members/" + memberId.toString() + "/suspend")
                .contentType("application/json");
    }

    @Test
    @DisplayName("valid suspension request should call the service with the acting user and answer 204 with link to members")
    @WithKlabisMockUser(userId = "48e11797-a61b-4783-bc1d-1c11d1b1d288", authorities = {Authority.MEMBERS_MANAGE})
    void shouldSuspendMemberSuccessfully() throws Exception {
        UUID memberId = UUID.randomUUID();

        mockMvc.perform(postMemberIdSuspend(memberId).content("""
                        {
                            "reason": "ODHLASKA",
                            "note": "Member requested termination"
                        }
                        """)
                )
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/api/members"));

        Member.SuspendMembership expectedCommand = MemberSuspendMembershipBuilder.builder()
                .suspendedBy(UserId.fromString("48e11797-a61b-4783-bc1d-1c11d1b1d288"))
                .reason(DeactivationReason.ODHLASKA)
                .note("Member requested termination")
                .build();
        Mockito.verify(managementService)
                .suspendMember(eq(new MemberId(memberId)), eq(expectedCommand), eq(Set.of()));
    }

    @Test
    @DisplayName("should return 400 with its own type when the member is already suspended")
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
    void shouldReturn400WhenSuspendingAlreadySuspendedMember() throws Exception {
        UUID memberId = UUID.randomUUID();

        when(managementService.suspendMember(eq(new MemberId(memberId)),
                any(Member.SuspendMembership.class), any()))
                .thenThrow(new MemberAlreadySuspendedException());
        mockMvc.perform(postMemberIdSuspend(memberId).content("""
                        {
                            "reason": "OTHER",
                            "note": "Second termination attempt"
                        }
                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.type").value("/errors/member-already-suspended"))
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("already suspended")));
    }

    @Test
    @DisplayName("missing reason should return 400 with fieldErrors.reason")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
    void shouldReturn400WithFieldErrorWhenReasonIsMissing() throws Exception {
        UUID memberId = UUID.randomUUID();
        mockMvc.perform(postMemberIdSuspend(memberId).content("""
                        {
                            "note": "Test note"
                        }
                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.reason").value("nesmí mít hodnotu Null"));
    }

    @Test
    @DisplayName("note exceeding 500 characters should return 400 with fieldErrors.note")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
    void shouldReturn400WithFieldErrorWhenNoteExceeds500Chars() throws Exception {
        UUID memberId = UUID.randomUUID();
        String longNote = "a".repeat(501);
        mockMvc.perform(postMemberIdSuspend(memberId).content("""
                        {
                            "reason": "ODHLASKA",
                            "note": "%s"
                        }
                        """.formatted(longNote))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.note").value("velikost musí ležet v rozsahu 0 až 500"));
    }

    @Test
    @DisplayName("should return 409 with affected groups when member is last owner of a group")
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
    void shouldReturn409WhenMemberIsLastGroupOwner() throws Exception {
        UUID memberId = UUID.randomUUID();
        List<OwnedGroup> groups = List.of(
                new OwnedGroup("cccccccc-cccc-cccc-cccc-cccccccccccc", "Trail Runners", "FREE",
                        URI.create("http://localhost/api/groups/cccccccc-cccc-cccc-cccc-cccccccccccc"))
        );

        when(managementService.suspendMember(eq(new MemberId(memberId)), any(Member.SuspendMembership.class), any()))
                .thenThrow(new SuspensionBlockedException(groups, null, List.of()));

        mockMvc.perform(postMemberIdSuspend(memberId).content("""
                        {
                            "reason": "ODHLASKA"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("/errors/suspension-blocked"))
                .andExpect(jsonPath("$.groups.affectedGroups").isArray())
                .andExpect(jsonPath("$.groups.affectedGroups[0].groupName").value("Trail Runners"))
                .andExpect(jsonPath("$.groups.affectedGroups[0].groupType").value("FREE"))
                .andExpect(jsonPath("$.groups.affectedGroups[0].groupLink").value(
                        "http://localhost/api/groups/cccccccc-cccc-cccc-cccc-cccccccccccc"))
                .andExpect(jsonPath("$.debt").doesNotExist());
    }

    @ParameterizedTest(name = "{0} group link is exposed in 409 body")
    @CsvSource({
            "TRAINING, http://localhost/api/training-groups/dddddddd-dddd-dddd-dddd-dddddddddddd",
            "LEGAL_GUARDIAN, http://localhost/api/legal-guardian-groups/dddddddd-dddd-dddd-dddd-dddddddddddd"
    })
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
    void shouldExposeGroupLinkForEveryGroupType(String groupType, String link) throws Exception {
        UUID memberId = UUID.randomUUID();
        List<OwnedGroup> groups = List.of(
                new OwnedGroup("dddddddd-dddd-dddd-dddd-dddddddddddd", "Žáci", groupType, URI.create(link)));

        when(managementService.suspendMember(eq(new MemberId(memberId)), any(Member.SuspendMembership.class), any()))
                .thenThrow(new SuspensionBlockedException(groups, null, List.of()));

        mockMvc.perform(postMemberIdSuspend(memberId).content("""
                        {
                            "reason": "ODHLASKA"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.groups.affectedGroups[0].groupType").value(groupType))
                .andExpect(jsonPath("$.groups.affectedGroups[0].groupLink").value(link));
    }

    @Test
    @DisplayName("should return 409 with balance and accountLink when member has outstanding debt")
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
    void shouldReturn409WhenMemberHasOutstandingDebt() throws Exception {
        UUID memberId = UUID.randomUUID();
        var snapshot = new MemberFinancialStatePort.MemberFinancialSnapshot(
                new MemberId(memberId),
                new MonetaryAmount(new BigDecimal("-250"), "CZK"),
                true);

        when(managementService.suspendMember(eq(new MemberId(memberId)), any(Member.SuspendMembership.class), any()))
                .thenThrow(new SuspensionBlockedException(List.of(), snapshot, List.of()));

        mockMvc.perform(postMemberIdSuspend(memberId).content("""
                        {
                            "reason": "ODHLASKA"
                        }
                        """))
                .andExpect(status().is(409))
                .andExpect(jsonPath("$.type").value("/errors/suspension-blocked"))
                .andExpect(jsonPath("$.debt.balance.amount").value(-250))
                .andExpect(jsonPath("$.debt.balance.currency").value("CZK"))
                .andExpect(jsonPath("$.debt.accountLink").value(
                        "http://localhost/api/members/%s/account".formatted(memberId)))
                .andExpect(jsonPath("$.groups").doesNotExist());
    }

    @Test
    @DisplayName("should pass cascadeGroupIds to the service and answer 204")
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
    void shouldPassCascadeGroupIdsToService() throws Exception {
        UUID memberId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        mockMvc.perform(postMemberIdSuspend(memberId).content("""
                        {
                            "reason": "ODHLASKA",
                            "cascadeGroupIds": ["%s"]
                        }
                        """.formatted(groupId)))
                .andExpect(status().isNoContent());

        Mockito.verify(managementService).suspendMember(eq(new MemberId(memberId)),
                any(Member.SuspendMembership.class), eq(Set.of(new LegalGuardianGroupId(groupId))));
    }

    @Test
    @DisplayName("should return 400 when a cascade group is not a sole-guardian group of the member")
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
    void shouldReturn400WhenCascadeGroupIsNotSoleGuardianGroup() throws Exception {
        UUID memberId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        when(managementService.suspendMember(eq(new MemberId(memberId)), any(Member.SuspendMembership.class), any()))
                .thenThrow(new NotSoleGuardianGroupException(new LegalGuardianGroupId(groupId)));

        mockMvc.perform(postMemberIdSuspend(memberId).content("""
                        {
                            "reason": "ODHLASKA",
                            "cascadeGroupIds": ["%s"]
                        }
                        """.formatted(groupId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/errors/not-sole-guardian-group"));
    }

    @Test
    @DisplayName("should return 409 with dependentMembers when a cascaded minor is blocked")
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
    void shouldReturn409WithDependentMembers() throws Exception {
        UUID memberId = UUID.randomUUID();
        UUID minorId = UUID.randomUUID();
        var debt = new MemberFinancialStatePort.MemberFinancialSnapshot(
                new MemberId(minorId), new MonetaryAmount(new BigDecimal("-100"), "CZK"), true);
        var freeGroup = new OwnedGroup("cccccccc-cccc-cccc-cccc-cccccccccccc", "Kids", "FREE",
                URI.create("http://localhost/api/groups/cccccccc-cccc-cccc-cccc-cccccccccccc"));
        when(managementService.suspendMember(eq(new MemberId(memberId)), any(Member.SuspendMembership.class), any()))
                .thenThrow(new SuspensionBlockedException(List.of(), null, List.of(
                        new SuspensionBlockedException.DependentBlockers(new MemberId(minorId), "Anna Smith",
                                List.of(freeGroup), debt))));

        mockMvc.perform(postMemberIdSuspend(memberId).content("""
                        {
                            "reason": "ODHLASKA",
                            "cascadeGroupIds": ["%s"]
                        }
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.debt").doesNotExist())
                .andExpect(jsonPath("$.groups").doesNotExist())
                .andExpect(jsonPath("$.dependentMembers[0].memberId").value(minorId.toString()))
                .andExpect(jsonPath("$.dependentMembers[0].memberName").value("Anna Smith"))
                .andExpect(jsonPath("$.dependentMembers[0].debt.balance.amount").value(-100))
                .andExpect(jsonPath("$.dependentMembers[0].debt.accountLink").value(
                        "http://localhost/api/members/%s/account".formatted(minorId)))
                .andExpect(jsonPath("$.dependentMembers[0].groups.affectedGroups[0].groupName").value("Kids"));
    }


    private MockHttpServletRequestBuilder postMemberIdResume(UUID memberId) {
        return post("/api/members/" + memberId.toString() + "/resume");
    }

    @Test
    @DisplayName("valid resume request should call the service with the acting user and answer 204 with Location")
    @WithKlabisMockUser(userId = "48e11797-a61b-4783-bc1d-1c11d1b1d288", authorities = {Authority.MEMBERS_MANAGE})
    void shouldResumeMemberSuccessfully() throws Exception {
        UUID memberId = UUID.randomUUID();

        mockMvc.perform(postMemberIdResume(memberId))
                .andExpect(status().isNoContent())
                .andExpect(header().exists(HttpHeaders.LOCATION));

        Member.ResumeMembership expectedCommand = MemberResumeMembershipBuilder.builder()
                .resumedBy(UserId.fromString("48e11797-a61b-4783-bc1d-1c11d1b1d288"))
                .build();
        Mockito.verify(managementService).resumeMember(eq(new MemberId(memberId)), eq(expectedCommand));
    }
}
