package com.klabis.members.infrastructure.restapi;

import com.klabis.common.TargetGrant;
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
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Legal guardian edits their minor child's profile (API)")
@MembersWebMvcTest
class GuardianEditsMinorApiTest {

    private static final String GUARDIAN_ID = "11111111-1111-1111-1111-111111111111";
    private static final String CHILD_ID = "22222222-2222-2222-2222-222222222222";
    private static final String OTHER_CHILD_ID = "33333333-3333-3333-3333-333333333333";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ManagementPort managementService;

    @Autowired
    private LegalGuardianGroupPort legalGuardianGroupPort;

    private static LegalGuardianGroup groupOf(String childId) {
        return LegalGuardianGroup.create(
                Set.of(new LegalGuardianGroup.Guardian(new UserId(UUID.fromString(GUARDIAN_ID)), "Nováková")),
                new LegalGuardianGroup.Minor(new MemberId(UUID.fromString(childId)), LocalDate.now().minusYears(9)));
    }

    private Member givenMinor(String childId) {
        Member child = MemberTestDataBuilder.aMemberWithId(UUID.fromString(childId))
                .withDateOfBirth(LocalDate.now().minusYears(9))
                .withNationality("CZ")
                .withBirthNumber("900101/1234")
                .withDietaryRestrictions("no nuts")
                .withActive(true)
                .build();
        when(managementService.getMemberAndRecordView(eq(new MemberId(UUID.fromString(childId))), any(UserId.class), any(MemberViewAccess.class)))
                .thenReturn(child);
        when(managementService.prefilledUpdateCommand(new MemberId(UUID.fromString(childId))))
                .thenReturn(Member.UpdateMember.from(child));
        when(managementService.updateMember(eq(new MemberId(UUID.fromString(childId))), any(Member.UpdateMember.class)))
                .thenReturn(child);
        when(legalGuardianGroupPort.findGroupOf(new MemberId(UUID.fromString(childId))))
                .thenReturn(Optional.of(groupOf(childId)));
        return child;
    }

    private ResultActions getMember(String id) throws Exception {
        return mockMvc.perform(get("/api/members/{id}", id).accept(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    @Test
    @DisplayName("guardian sees the minor's full detail and gets the edit template with reserved fields read-only and no administrator actions")
    @WithKlabisMockUser(userId = GUARDIAN_ID, memberId = GUARDIAN_ID, authorities = {Authority.MEMBERS_READ},
            targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE, type = TargetType.MEMBER,
                    ids = CHILD_ID))
    void guardianSeesFullDetailAndEditTemplateOfMinor() throws Exception {
        givenMinor(CHILD_ID);

        ResultActions result = getMember(CHILD_ID)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.birthNumber").value("900101/1234"))
                .andExpect(jsonPath("$.dateOfBirth").exists())
                .andExpect(jsonPath("$.gender").exists())
                .andExpect(jsonPath("$.dietaryRestrictions").value("no nuts"))
                .andExpect(jsonPath("$._links.legalGuardians.href").value(endsWith("/guardians")))
                .andExpect(jsonPath("$._templates.updateMember.method").value("PATCH"));
        for (String reserved : new String[]{"firstName", "lastName", "dateOfBirth", "gender", "birthNumber", "nationality"}) {
            result.andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='" + reserved + "')].readOnly")
                    .value(true));
        }
        result.andExpect(jsonPath("$._templates.suspendMember").doesNotExist())
                .andExpect(jsonPath("$._templates.setMemberLegalGuardians").doesNotExist())
                .andExpect(jsonPath("$._links.legalGuardianGroup").doesNotExist());
    }

    @Test
    @DisplayName("guardian saves a change of the minor's telephone")
    @WithKlabisMockUser(userId = GUARDIAN_ID, memberId = GUARDIAN_ID, authorities = {Authority.MEMBERS_READ},
            targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE, type = TargetType.MEMBER,
                    ids = CHILD_ID))
    void guardianSavesPhoneChange() throws Exception {
        givenMinor(CHILD_ID);

        mockMvc.perform(patch("/api/members/{id}", CHILD_ID)
                        .contentType("application/json")
                        .content("{\"phone\": \"+420777123456\"}"))
                .andExpect(status().isNoContent());

        verify(managementService).updateMember(eq(new MemberId(UUID.fromString(CHILD_ID))), any(Member.UpdateMember.class));
    }

    @Test
    @DisplayName("guardian of one child sees an unrelated minor without edit action, sensitive data or guardians")
    @WithKlabisMockUser(userId = GUARDIAN_ID, memberId = GUARDIAN_ID, authorities = {Authority.MEMBERS_READ},
            targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE, type = TargetType.MEMBER,
                    ids = CHILD_ID))
    void guardianSeesUnrelatedMinorWithoutEditAction() throws Exception {
        givenMinor(OTHER_CHILD_ID);

        getMember(OTHER_CHILD_ID)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.updateMember").doesNotExist())
                .andExpect(jsonPath("$.birthNumber").doesNotExist())
                .andExpect(jsonPath("$.dietaryRestrictions").doesNotExist())
                .andExpect(jsonPath("$._links.legalGuardians").doesNotExist());
    }

    @Test
    @DisplayName("caller holding only VIEW_PROFILE over the minor sees the profile data but gets no edit action")
    @WithKlabisMockUser(userId = GUARDIAN_ID, memberId = GUARDIAN_ID, authorities = {Authority.MEMBERS_READ},
            targetGrants = @TargetGrant(authority = Authority.MEMBERS_VIEW_PROFILE, type = TargetType.MEMBER,
                    ids = CHILD_ID))
    void viewProfileHolderSeesDataWithoutEditAction() throws Exception {
        givenMinor(CHILD_ID);

        getMember(CHILD_ID)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateOfBirth").exists())
                .andExpect(jsonPath("$.gender").exists())
                .andExpect(jsonPath("$.nationality").exists())
                .andExpect(jsonPath("$._templates.updateMember").doesNotExist());
    }
}
