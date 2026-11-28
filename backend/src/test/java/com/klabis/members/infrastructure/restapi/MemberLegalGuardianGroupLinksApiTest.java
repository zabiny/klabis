package com.klabis.members.infrastructure.restapi;

import com.klabis.common.SelfGrants;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.MembersWebMvcTest;
import com.klabis.members.application.MemberViewAccess;
import com.klabis.members.domain.Member;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.ResultActions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /api/members/{id} — legal guardian group links")
@MembersWebMvcTest
class MemberLegalGuardianGroupLinksApiTest extends MemberApiTestBase {

    @Autowired
    private LegalGuardianGroupPort legalGuardianGroupPort;

    private static final String CHILD_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final MemberId CHILD = new MemberId(UUID.fromString(CHILD_ID));
    private static final String OTHER_CHILD_ID = "33333333-3333-3333-3333-333333333333";
    private static final MemberId GUARDIAN = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

    private LegalGuardianGroup groupOfChild() {
        return LegalGuardianGroup.create(
                Set.of(new LegalGuardianGroup.Guardian(GUARDIAN.toUserId(), "Novák")),
                new LegalGuardianGroup.Minor(CHILD, LocalDate.now().minusYears(9)));
    }

    private void givenMember(LocalDate dateOfBirth, LegalGuardianGroup group) {
        Member member = MemberTestDataBuilder.aMemberWithId(CHILD.uuid()).withDateOfBirth(dateOfBirth).build();
        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class)))
                .thenReturn(member);
        when(legalGuardianGroupPort.findGroupOf(any(MemberId.class))).thenReturn(Optional.ofNullable(group));
    }

    private ResultActions getChild() throws Exception {
        return mockMvc.perform(get("/api/members/{id}", CHILD_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("adds legalGuardianGroup link pointing at the group the minor belongs to")
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void addsLinkForMinor() throws Exception {
        LegalGuardianGroup group = groupOfChild();
        givenMember(LocalDate.now().minusYears(9), group);

        getChild().andExpect(jsonPath("$._links.legalGuardianGroup.href")
                .value(Matchers.endsWith("/api/legal-guardian-groups/" + group.getId().uuid())));
    }

    @Test
    @DisplayName("adds no legalGuardianGroup link when the caller lacks MEMBERS:MANAGE")
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_READ})
    void addsNoLinkWithoutManageAuthority() throws Exception {
        givenMember(LocalDate.now().minusYears(9), groupOfChild());

        getChild().andExpect(jsonPath("$._links.legalGuardianGroup").doesNotExist());
    }

    @Test
    @DisplayName("adds no legalGuardianGroup link when the member is not in any group")
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void addsNoLinkWithoutGroup() throws Exception {
        givenMember(LocalDate.now().minusYears(9), null);

        getChild().andExpect(jsonPath("$._links.legalGuardianGroup").doesNotExist());
    }

    @Test
    @DisplayName("adds legalGuardians link to the guardians of the group for MEMBERS:MANAGE")
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void addsGuardiansLinkForAdmin() throws Exception {
        LegalGuardianGroup group = groupOfChild();
        givenMember(LocalDate.now().minusYears(9), group);

        getChild().andExpect(jsonPath("$._links.legalGuardians.href")
                .value(Matchers.endsWith("/api/legal-guardian-groups/" + group.getId().uuid() + "/guardians")));
    }

    @Test
    @DisplayName("adds legalGuardians link on the minor's own detail without MEMBERS:MANAGE")
    @WithKlabisMockUser(memberId = CHILD_ID, authorities = {Authority.MEMBERS_READ}, selfGrants = SelfGrants.MINOR_MEMBER)
    void addsGuardiansLinkForTheMinorThemself() throws Exception {
        givenMember(LocalDate.now().minusYears(9), groupOfChild());

        getChild().andExpect(jsonPath("$._links.legalGuardians").exists());
    }

    @Test
    @DisplayName("adds no legalGuardians link on another member's detail without MEMBERS:MANAGE")
    @WithKlabisMockUser(memberId = OTHER_CHILD_ID, authorities = {Authority.MEMBERS_READ})
    void addsNoGuardiansLinkForOthers() throws Exception {
        givenMember(LocalDate.now().minusYears(9), groupOfChild());

        getChild().andExpect(jsonPath("$._links.legalGuardians").doesNotExist());
    }

    @Test
    @DisplayName("adds no legalGuardians link for a member who has turned 18 but is still in a group")
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void addsNoGuardiansLinkForAdult() throws Exception {
        givenMember(LocalDate.now().minusYears(18), groupOfChild());

        getChild().andExpect(jsonPath("$._links.legalGuardians").doesNotExist());
    }

    @Test
    @DisplayName("adds no legalGuardians link for a minor without a group")
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void addsNoGuardiansLinkWithoutGroup() throws Exception {
        givenMember(LocalDate.now().minusYears(9), null);

        getChild().andExpect(jsonPath("$._links.legalGuardians").doesNotExist());
    }
}
