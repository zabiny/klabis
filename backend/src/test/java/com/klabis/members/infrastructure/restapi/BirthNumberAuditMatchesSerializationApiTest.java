package com.klabis.members.infrastructure.restapi;

import com.jayway.jsonpath.JsonPath;
import com.klabis.common.TargetGrant;
import com.klabis.common.SelfGrants;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.authorization.TargetType;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.MembersWebMvcTest;
import com.klabis.members.application.ManagementPort;
import com.klabis.members.application.MemberViewAccess;
import com.klabis.members.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Detail visibility, edit action and birth number audit per viewer (API)")
@MembersWebMvcTest
class BirthNumberAuditMatchesSerializationApiTest {

    private static final String VIEWER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String MEMBER_ID = "22222222-2222-2222-2222-222222222222";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ManagementPort managementService;

    private void assertViewerSees(LocalDate dateOfBirth, boolean memberActive, MemberViewAccess expectedAccess,
                                  boolean expectedCanEdit) throws Exception {
        MemberId memberId = new MemberId(UUID.fromString(MEMBER_ID));
        Member member = MemberTestDataBuilder.aMemberWithId(memberId.uuid())
                .withDateOfBirth(dateOfBirth)
                .withNationality("CZ")
                .withBirthNumber("900101/1234")
                .withActive(memberActive)
                .build();
        when(managementService.getMemberAndRecordView(eq(memberId), any(UserId.class), any(MemberViewAccess.class)))
                .thenReturn(member);

        String body = mockMvc.perform(get("/api/members/{id}", MEMBER_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ArgumentCaptor<MemberViewAccess> access = ArgumentCaptor.forClass(MemberViewAccess.class);
        verify(managementService).getMemberAndRecordView(eq(memberId), any(UserId.class), access.capture());
        Map<?, ?> json = JsonPath.read(body, "$");
        boolean serialized = json.containsKey("birthNumber");
        assertThat(access.getValue()).isEqualTo(expectedAccess);
        assertThat(serialized)
                .as("audit decision vs birthNumber serialized")
                .isEqualTo(access.getValue().birthNumberVisible());
        boolean canEdit = json.get("_templates") instanceof Map<?, ?> templates && templates.containsKey("updateMember");
        assertThat(canEdit).as("edit action offered").isEqualTo(expectedCanEdit);
    }

    private static final LocalDate ADULT = LocalDate.of(1990, 1, 1);

    private static LocalDate minor() {
        return LocalDate.now().minusYears(10);
    }

    @Test
    @DisplayName("administrator")
    @WithKlabisMockUser(userId = VIEWER_ID, memberId = VIEWER_ID,
            authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void administrator() throws Exception {
        assertViewerSees(ADULT, true, new MemberViewAccess(true, true), true);
    }

    @Test
    @DisplayName("holder of the delegated permission over the member")
    @WithKlabisMockUser(userId = VIEWER_ID, memberId = VIEWER_ID, authorities = {Authority.MEMBERS_READ},
            targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE, type = TargetType.MEMBER,
                    ids = MEMBER_ID))
    void holder() throws Exception {
        assertViewerSees(ADULT, true, new MemberViewAccess(true, true), true);
    }

    @Test
    @DisplayName("holder of the delegated permission over a suspended minor")
    @WithKlabisMockUser(userId = VIEWER_ID, memberId = VIEWER_ID, authorities = {Authority.MEMBERS_READ},
            targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE, type = TargetType.MEMBER,
                    ids = MEMBER_ID))
    void holderOverSuspendedMinor() throws Exception {
        assertViewerSees(minor(), false, new MemberViewAccess(true, true), true);
    }

    @Test
    @DisplayName("holder of the permission over somebody else")
    @WithKlabisMockUser(userId = VIEWER_ID, memberId = VIEWER_ID, authorities = {Authority.MEMBERS_READ},
            targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE, type = TargetType.MEMBER,
                    ids = VIEWER_ID))
    void holderOverSomebodyElse() throws Exception {
        assertViewerSees(ADULT, true, new MemberViewAccess(false, false), false);
    }

    @Test
    @DisplayName("the member themself")
    @WithKlabisMockUser(userId = MEMBER_ID, memberId = MEMBER_ID, authorities = {Authority.MEMBERS_READ},
            selfGrants = SelfGrants.MEMBER)
    void self() throws Exception {
        assertViewerSees(ADULT, true, new MemberViewAccess(true, true), true);
    }

    @Test
    @DisplayName("the minor themself")
    @WithKlabisMockUser(userId = MEMBER_ID, memberId = MEMBER_ID, authorities = {Authority.MEMBERS_READ},
            selfGrants = SelfGrants.MINOR_MEMBER)
    void minorSelf() throws Exception {
        assertViewerSees(minor(), true, new MemberViewAccess(false, true), false);
    }

    @Test
    @DisplayName("another club member")
    @WithKlabisMockUser(userId = VIEWER_ID, memberId = VIEWER_ID, authorities = {Authority.MEMBERS_READ})
    void otherMember() throws Exception {
        assertViewerSees(ADULT, true, new MemberViewAccess(false, false), false);
    }
}
