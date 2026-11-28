package com.klabis.members.infrastructure.restapi;

import com.klabis.common.TargetGrant;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.authorization.TargetType;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.groups.traininggroup.TrainingGroupId;
import com.klabis.groups.traininggroup.application.TrainingGroupManagementPort;
import com.klabis.groups.traininggroup.domain.AgeRange;
import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.members.MemberId;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.MembersWebMvcTest;
import com.klabis.members.application.EffectiveContact;
import com.klabis.members.application.MemberAccountActivationPort;
import com.klabis.members.application.MemberCompletenessPort;
import com.klabis.members.application.MemberContactPort;
import com.klabis.members.application.MemberNotFoundException;
import com.klabis.members.application.MemberViewAccess;
import com.klabis.members.domain.Address;
import com.klabis.members.domain.Country;
import com.klabis.members.domain.DeactivationReason;
import com.klabis.members.domain.DrivingLicenseGroup;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.Gender;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MissingDataItem;
import com.klabis.members.domain.PhoneNumber;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncPairing;
import com.klabis.sync.domain.SyncTarget;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /api/members/{id}")
@MembersWebMvcTest
class MemberDetailApiTest extends MemberApiTestBase {

    @Autowired
    private MemberCompletenessPort memberCompletenessPort;

    @Autowired
    private MemberContactPort memberContactPort;

    @Autowired
    private MemberAccountActivationPort accountActivationPort;

    @Autowired
    private TrainingGroupManagementPort trainingGroupManagementPort;

    @Autowired
    private LegalGuardianGroupPort legalGuardianGroupPort;

    private MockHttpServletRequestBuilder getMemberById(MemberId memberId) {
        return getMemberById(memberId.uuid());
    }

    private MockHttpServletRequestBuilder getMemberById(UUID memberId) {
        return get("/api/members/{id}", memberId.toString())
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE);
    }

    @Test
    @DisplayName("should return 200 with member details")
    @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ})
    void shouldReturnMemberDetailsWhenFound() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withFirstName("Jan")
                .withLastName("Novák")
                .withRegistrationNumber("ZBM0501")
                .withEmail("jan.novak@example.com")
                .withDateOfBirth(LocalDate.of(2005, 6, 15))
                .withGender(Gender.MALE)
                .withPhone("+420777888999")
                .withAddress(new Address("Hlavní 123", "Praha", "11000", Country.of("CZ")))
                .withActive(true)
                .withNationality("CZ")
                .build();

        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class))).thenReturn(member);

        mockMvc.perform(getMemberById(member.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(jsonPath("$.id").value(memberId.toString()))
                .andExpect(jsonPath("$.registrationNumber").value("ZBM0501"))
                .andExpect(jsonPath("$.firstName").exists())
                .andExpect(jsonPath("$.lastName").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.phone").exists())
                .andExpect(jsonPath("$.address").exists())
                .andExpect(jsonPath("$.active").doesNotExist())
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.self.href").value(Matchers.containsString(
                        "/api/members/" + memberId)))
                .andExpect(jsonPath("$._links.collection.href").exists())
                .andExpect(jsonPath("$._links.collection.href").value(Matchers.containsString(
                        "/api/members")));
    }

    @Test
    @DisplayName("should return 404 when member not found")
    @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ})
    void shouldReturn404WhenMemberNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class)))
                .thenThrow(new MemberNotFoundException(new MemberId(nonExistentId)));

        mockMvc.perform(getMemberById(nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value(Matchers.containsString(
                        nonExistentId.toString())));
    }

    @Test
    @DisplayName("should omit active field for caller without MEMBERS:MANAGE authority")
    @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ})
    void shouldOmitActiveFieldWhenUserLacksMembersManageAuthority() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withActive(false)
                .build();

        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class))).thenReturn(member);

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").doesNotExist());
    }

    @Test
    @DisplayName("should include active field for caller with MEMBERS:MANAGE authority")
    @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void shouldIncludeActiveFieldWhenUserHasMembersManageAuthority() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withActive(false)
                .build();

        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class))).thenReturn(member);

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("should return single email and phone with address")
    @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ})
    void shouldReturnSingleEmailAndPhoneWithAddress() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withAddress(new Address("Main Street 123", "Bratislava", "81101", Country.of("SK")))
                .build();

        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class))).thenReturn(member);

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").isNotEmpty())
                .andExpect(jsonPath("$.address.street").exists())
                .andExpect(jsonPath("$.address.city").exists())
                .andExpect(jsonPath("$.address.postalCode").exists())
                .andExpect(jsonPath("$.address.country").exists());
    }

    @Test
    @DisplayName("HAL+FORMS: user with MEMBERS_PERMISSIONS authority: should include permissions link")
    @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_PERMISSIONS})
    void activeMemberShouldReturnPermissionsLink() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withActive(true)
                .build();

        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class))).thenReturn(member);

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").doesNotExist())
                .andExpect(jsonPath("$._links.permissions.href").value("http://localhost/api/users/" + memberId + "/permissions"));
    }

    @Test
    @DisplayName("HAL+FORMS: user with MEMBERS_PERMISSIONS authority: should not return permissions link for deactivated member response")
    @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_PERMISSIONS})
    void activeMemberShouldNotReturnPermissionsLinkForDeactivatedMember() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withActive(false)
                .build();

        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class))).thenReturn(member);

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").doesNotExist())
                .andExpect(jsonPath("$._links.permissions").doesNotExist());
    }

    @Test
    @DisplayName("HAL+FORMS: user with MEMBERS_MANAGE authority: should include update and suspend in active member response (no permissions)")
    @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void activeMemberShouldReturnUpdateAndSuspendAffordances() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withActive(true)
                .build();

        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class))).thenReturn(member);

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$._templates").exists())
                .andExpect(jsonPath("$._templates.updateMember.method").value("PATCH"))  // "UPDATE member"
                .andExpect(jsonPath("$._templates.updateMember.target").doesNotExist())
                .andExpect(jsonPath("$._templates.resumeMember").doesNotExist())
                .andExpect(jsonPath("$._templates.suspendMember.method").value("POST"))
                .andExpect(jsonPath("$._templates.suspendMember.target").value(
                        "http://localhost/api/members/%s/suspend".formatted(memberId)));
    }

    @Test
    @DisplayName("HAL+FORMS: updateMember affordance carries inline gender and drivingLicenseGroup options")
    @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void updateMemberAffordanceCarriesGenderAndDrivingLicenseInlineOptions() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withActive(true)
                .build();

        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class))).thenReturn(member);

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='gender')].options.inline")
                        .value(Matchers.hasItem(Matchers.hasItems("MALE", "FEMALE"))))
                .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='gender')].options.inline.length()")
                        .value(Matchers.hasItem(2)))
                .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='drivingLicenseGroup')].options.inline")
                        .value(Matchers.hasItem(Matchers.hasItems("AM", "T"))))
                .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='drivingLicenseGroup')].options.inline.length()")
                        .value(Matchers.hasItem(11)))
                // x-hal-input-type pins a codegen-independent type: without it this would be
                // the generated inline enum class name (UpdateMemberRequestGender).
                .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='gender')].type").value("Gender"))
                .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='drivingLicenseGroup')].type").value("DrivingLicenseGroup"));
    }

    @Test
    @DisplayName("HAL+FORMS: suspendMember affordance carries inline deactivation reason options")
    @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void suspendMemberAffordanceCarriesDeactivationReasonInlineOptions() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withActive(true)
                .build();

        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class))).thenReturn(member);

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.suspendMember.properties[?(@.name=='reason')].options.inline[*]")
                        .value(Matchers.contains("ODHLASKA", "PRESTUP", "OTHER")))
                .andExpect(jsonPath("$._templates.suspendMember.properties[?(@.name=='reason')].type").value("DeactivationReason"));
    }

    @Test
    @DisplayName("HAL+FORMS: user with MEMBERS_MANAGE authority: should include update and resume affordances for suspended member")
    @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void suspendedMemberShouldReturnUpdateAndResumeAffordances() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .suspended(DeactivationReason.PRESTUP, "Transfer to Brno")
                .build();

        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class))).thenReturn(member);

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.suspensionReason").value("PRESTUP"))
                .andExpect(jsonPath("$.suspensionNote").value("Transfer to Brno"))
                .andExpect(jsonPath("$.suspendedAt").exists())
                .andExpect(jsonPath("$.suspendedBy").exists())
                .andExpect(jsonPath("$._links.permissions").doesNotExist())
                .andExpect(jsonPath("$._templates").exists())
                .andExpect(jsonPath("$._templates.updateMember.method").value("PATCH"))
                .andExpect(jsonPath("$._templates.updateMember.target").doesNotExist())
                .andExpect(jsonPath("$._templates.suspendMember").doesNotExist())
                .andExpect(jsonPath("$._templates.resumeMember.method").value("POST"))
                .andExpect(jsonPath("$._templates.resumeMember.target").value(
                        "http://localhost/api/members/%s/resume".formatted(memberId)));
    }

    @Test
    @DisplayName("HAL+FORMS: administrator's template has reserved fields editable")
    @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void adminTemplateShouldKeepReservedFieldsEditable() throws Exception {
        UUID memberId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Member member = MemberTestDataBuilder.aMemberWithId(memberId).withActive(true).build();
        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class))).thenReturn(member);

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='firstName')]").exists())
                .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='firstName')].readOnly")
                        .value(Matchers.not(Matchers.hasItem(true))))
                .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='nationality')]").exists())
                .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='nationality')].readOnly")
                        .value(Matchers.not(Matchers.hasItem(true))));
    }

    @Test
    @DisplayName("holder of EDIT_PROFILE over a member sees all data and the edit template with reserved fields read-only, but no admin-only data")
    @WithKlabisMockUser(username = "ZBM0101", memberId = "22222222-2222-2222-2222-222222222222", authorities = {Authority.MEMBERS_READ},
            targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE, type = TargetType.MEMBER,
                    ids = "11111111-1111-1111-1111-111111111111"))
    void holderShouldSeeAllDataAndEditTemplate() throws Exception {
        UUID memberId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withActive(true)
                .withDateOfBirth(LocalDate.of(1990, 1, 1))
                .withDietaryRestrictions("vegan")
                .build();
        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class))).thenReturn(member);

        var result = mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateOfBirth").value("1990-01-01"))
                .andExpect(jsonPath("$.dietaryRestrictions").value("vegan"))
                .andExpect(jsonPath("$.gender").exists())
                .andExpect(jsonPath("$.missingData").doesNotExist())
                .andExpect(jsonPath("$._templates.updateMember.method").value("PATCH"))
                .andExpect(jsonPath("$._templates.updateMember.target").doesNotExist())
                .andExpect(jsonPath("$._templates.suspendMember").doesNotExist());

        for (String reserved : List.of("firstName", "lastName", "dateOfBirth", "gender", "birthNumber", "nationality")) {
            result.andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='" + reserved + "')].readOnly")
                    .value(true));
        }
        for (String editable : List.of("email", "phone", "dietaryRestrictions", "chipNumber")) {
            result.andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='" + editable + "')]").exists())
                    .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='" + editable + "')].readOnly")
                            .value(Matchers.not(Matchers.hasItem(true))));
        }
    }

    @Test
    @DisplayName("HAL+FORMS: member viewing another member's profile — should not include any PATCH affordance")
    @WithKlabisMockUser(username = "ZBM0101", memberId = "22222222-2222-2222-2222-222222222222", authorities = {Authority.MEMBERS_READ})
    void otherMemberProfileShouldNotReturnAnyPatchAffordance() throws Exception {
        UUID memberId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withActive(true)
                .build();

        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class))).thenReturn(member);

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates").doesNotExist());
    }

    @Test
    @DisplayName("non-admin user gets 404 when accessing detail of an inactive member")
    @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
    void nonAdminUserGets404ForInactiveMember() throws Exception {
        UUID memberId = UUID.randomUUID();
        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), eq(new MemberViewAccess(false, false))))
                .thenThrow(new MemberNotFoundException(new MemberId(memberId)));

        mockMvc.perform(get("/api/members/{id}", memberId).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("admin user can still access detail of an inactive member")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void adminUserCanAccessInactiveMember() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member inactiveMember = MemberTestDataBuilder.aMemberWithId(memberId).withActive(false).build();
        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), eq(new MemberViewAccess(true, true))))
                .thenReturn(inactiveMember);

        mockMvc.perform(get("/api/members/{id}", memberId).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("should include trainingGroup link when member belongs to a training group")
    @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
    void shouldIncludeTrainingGroupLinkWhenMemberBelongsToGroup() throws Exception {
        UUID memberId = UUID.randomUUID();
        UUID groupId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();
        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class)))
                .thenReturn(member);
        TrainingGroup trainingGroup =
                TrainingGroup.reconstruct(
                        new TrainingGroupId(groupId), "Juniors",
                        Set.of(new MemberId(UUID.randomUUID())), Set.of(),
                        new AgeRange(10, 18), null);
        when(trainingGroupManagementPort.findTrainingGroupOfMember(any(MemberId.class)))
                .thenReturn(Optional.of(trainingGroup));
        when(legalGuardianGroupPort.findGroupOf(any(MemberId.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.trainingGroup.href")
                        .value(Matchers.endsWith("/api/training-groups/" + groupId)))
                .andExpect(jsonPath("$._links.legalGuardianGroup").doesNotExist());
    }

    @Test
    @DisplayName("should not include group links when member belongs to no groups")
    @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
    void shouldNotIncludeGroupLinksWhenMemberBelongsToNoGroups() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();
        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class)))
                .thenReturn(member);
        when(trainingGroupManagementPort.findTrainingGroupOfMember(any(MemberId.class)))
                .thenReturn(Optional.empty());
        when(legalGuardianGroupPort.findGroupOf(any(MemberId.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.trainingGroup").doesNotExist())
                .andExpect(jsonPath("$._links.legalGuardianGroup").doesNotExist());
    }

    @Test
    @DisplayName("member detail never contains ical-token link")
    @WithKlabisMockUser(username = MEMBER_USERNAME, memberId = "11111111-1111-1111-1111-111111111111", authorities = {Authority.MEMBERS_READ})
    void memberDetailShouldNotIncludeIcalTokenLink() throws Exception {
        UUID memberId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Member member = MemberTestDataBuilder.aMemberWithId(memberId).withActive(true).build();
        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class)))
                .thenReturn(member);
        when(trainingGroupManagementPort.findTrainingGroupOfMember(any(MemberId.class)))
                .thenReturn(Optional.empty());
        when(legalGuardianGroupPort.findGroupOf(any(MemberId.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links['ical-token']").doesNotExist());
    }

    @Test
    @DisplayName("own member detail contains changePassword link with affordance")
    @WithKlabisMockUser(username = MEMBER_USERNAME, memberId = "11111111-1111-1111-1111-111111111111", authorities = {Authority.MEMBERS_READ})
    void ownDetailShouldIncludeChangePasswordLink() throws Exception {
        UUID memberId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Member member = MemberTestDataBuilder.aMemberWithId(memberId).withActive(true).build();
        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class)))
                .thenReturn(member);

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.changePassword.href").value(Matchers.endsWith("/api/me/password-change")))
                .andExpect(jsonPath("$._templates.changePassword.method").value("POST"));
    }

    @Test
    @DisplayName("another member's detail does not contain changePassword link")
    @WithKlabisMockUser(username = ADMIN_USERNAME, memberId = "11111111-1111-1111-1111-111111111111", authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void otherDetailShouldNotIncludeChangePasswordLink() throws Exception {
        UUID otherId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(otherId).withActive(true).build();
        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class)))
                .thenReturn(member);

        mockMvc.perform(getMemberById(otherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.changePassword").doesNotExist())
                .andExpect(jsonPath("$._templates.changePassword").doesNotExist());
    }

    @Test
    @DisplayName("offers setMemberLegalGuardians affordance for a minor, not for an adult")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void offersSetLegalGuardiansOnlyForMinor() throws Exception {
        UUID minorId = UUID.randomUUID();
        UUID adultId = UUID.randomUUID();
        Member minor = MemberTestDataBuilder.aMemberWithId(minorId)
                .withDateOfBirth(LocalDate.now().minusYears(10)).build();
        Member adult = MemberTestDataBuilder.aMemberWithId(adultId)
                .withDateOfBirth(LocalDate.of(1990, 1, 1)).build();
        when(managementService.getMemberAndRecordView(eq(new MemberId(minorId)), any(UserId.class), any(MemberViewAccess.class))).thenReturn(minor);
        when(managementService.getMemberAndRecordView(eq(new MemberId(adultId)), any(UserId.class), any(MemberViewAccess.class))).thenReturn(adult);

        mockMvc.perform(getMemberById(minorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.setMemberLegalGuardians.method").value("PUT"));
        mockMvc.perform(getMemberById(adultId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.setMemberLegalGuardians").doesNotExist());
    }

    @Test
    @DisplayName("offers sendMemberAccountActivation only when activation is available for the member")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void offersAccountActivationOnlyWhenAvailable() throws Exception {
        UUID availableId = UUID.randomUUID();
        UUID unavailableId = UUID.randomUUID();
        Member available = MemberTestDataBuilder.aMemberWithId(availableId)
                .withDateOfBirth(LocalDate.now().minusYears(10)).build();
        Member unavailable = MemberTestDataBuilder.aMemberWithId(unavailableId)
                .withDateOfBirth(LocalDate.now().minusYears(10)).build();
        when(managementService.getMemberAndRecordView(eq(new MemberId(availableId)), any(UserId.class), any(MemberViewAccess.class))).thenReturn(available);
        when(managementService.getMemberAndRecordView(eq(new MemberId(unavailableId)), any(UserId.class), any(MemberViewAccess.class))).thenReturn(unavailable);
        when(accountActivationPort.isAvailableFor(available)).thenReturn(true);
        when(accountActivationPort.isAvailableFor(unavailable)).thenReturn(false);

        mockMvc.perform(getMemberById(availableId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.sendMemberAccountActivation.method").value("POST"))
                .andExpect(jsonPath("$._templates.sendMemberAccountActivation.target").value(
                        "http://localhost/api/members/%s/account-activation".formatted(availableId)));
        mockMvc.perform(getMemberById(unavailableId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.sendMemberAccountActivation").doesNotExist());
    }

    @Test
    @DisplayName("does not offer sendMemberAccountActivation without MEMBERS:MANAGE")
    @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
    void hidesAccountActivationWithoutManage() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withDateOfBirth(LocalDate.now().minusYears(10)).build();
        when(managementService.getMemberAndRecordView(eq(new MemberId(memberId)), any(UserId.class), any(MemberViewAccess.class))).thenReturn(member);
        when(accountActivationPort.isAvailableFor(member)).thenReturn(true);

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.sendMemberAccountActivation").doesNotExist());
    }

    @Test
    @DisplayName("7.1 — detail endpoint serializes a member without an address (field simply omitted)")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void detailSerializesMemberWithoutAddress() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withDateOfBirth(LocalDate.of(1990, 1, 1))
                .withAddress(null)
                .build();

        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class))).thenReturn(member);
        when(memberCompletenessPort.missingData(eq(member), any())).thenReturn(Set.of(MissingDataItem.ADDRESS));

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").doesNotExist())
                .andExpect(jsonPath("$.missingData").value(Matchers.hasItem("ADDRESS")));
    }

    private Member minorWithoutContact(UUID memberId) {
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withDateOfBirth(LocalDate.now().minusYears(10))
                .withEmail((EmailAddress) null)
                .withPhone((PhoneNumber) null)
                .build();
        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class))).thenReturn(member);
        when(memberContactPort.effectiveContactOf(eq(member), any())).thenReturn(new EffectiveContact(
                new EffectiveContact.Value("jan@example.com", "Jan Novák"),
                EffectiveContact.Value.EMPTY));
        return member;
    }

    @Test
    @DisplayName("displayContact — MEMBERS:MANAGE caller sees guardian marker and raw email/phone stay empty")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void manageCallerSeesGuardianMarker() throws Exception {
        UUID memberId = UUID.randomUUID();
        minorWithoutContact(memberId);

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayContact.email.value").value("jan@example.com"))
                .andExpect(jsonPath("$.displayContact.email.fromGuardian").value(true))
                .andExpect(jsonPath("$.displayContact.email.guardianName").value("Jan Novák"))
                .andExpect(jsonPath("$.displayContact.phone.value").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist());
    }

    @Test
    @DisplayName("displayContact — caller without MEMBERS:MANAGE sees the value only")
    @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
    void regularCallerDoesNotSeeGuardianMarker() throws Exception {
        UUID memberId = UUID.randomUUID();
        minorWithoutContact(memberId);

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayContact.email.value").value("jan@example.com"))
                .andExpect(jsonPath("$.displayContact.email.fromGuardian").doesNotExist())
                .andExpect(jsonPath("$.displayContact.email.guardianName").doesNotExist());
    }

    @Test
    @DisplayName("edit template of a minor without own e-mail does not prefill the guardian's e-mail")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void editTemplateDoesNotPrefillGuardianEmail() throws Exception {
        UUID memberId = UUID.randomUUID();
        minorWithoutContact(memberId);

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='email')].value").isEmpty());
    }

    @Test
    @DisplayName("7.2 — MEMBERS:MANAGE caller sees missingData on an incomplete member")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void manageCallerSeesMissingDataInDetail() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withDateOfBirth(LocalDate.of(1990, 1, 1))
                .withPhone((PhoneNumber) null)
                .build();

        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class))).thenReturn(member);
        when(memberCompletenessPort.missingData(eq(member), any())).thenReturn(Set.of(MissingDataItem.PHONE));

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missingData").isArray())
                .andExpect(jsonPath("$.missingData[0]").value("PHONE"));
    }

    @Test
    @DisplayName("7.2 — caller without MEMBERS:MANAGE does not see missingData")
    @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
    void nonManageCallerDoesNotSeeMissingData() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withDateOfBirth(LocalDate.of(1990, 1, 1))
                .withPhone((PhoneNumber) null)
                .build();

        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class))).thenReturn(member);

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missingData").doesNotExist());
    }

    @Test
    @DisplayName("7.2 — member viewing their own incomplete profile does not see missingData")
    @WithKlabisMockUser(username = MEMBER_USERNAME, memberId = "11111111-1111-1111-1111-111111111111", authorities = {Authority.MEMBERS_READ})
    void ownProfileDoesNotSeeMissingDataWithoutManage() throws Exception {
        UUID memberId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withDateOfBirth(LocalDate.of(1990, 1, 1))
                .withPhone((PhoneNumber) null)
                .build();

        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class))).thenReturn(member);

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missingData").doesNotExist());
    }

    @Test
    @DisplayName("should include sync link for a member brought in from ORIS")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ})
    void shouldIncludeSyncLinkForOrisLinkedMember() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();
        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class)))
                .thenReturn(member);
        when(pairingLookup.pairingOf(targetFor(new MemberId(memberId)))).thenReturn(SyncPairing.ACTIVE);

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.sync.href").exists());

        Mockito.verify(pairingLookup, Mockito.times(1))
                .pairingOf(targetFor(new MemberId(memberId)));
    }

    @Test
    @DisplayName("should not include sync link for a hand-registered member")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ})
    void shouldNotIncludeSyncLinkForHandRegisteredMember() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();
        when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), any(MemberViewAccess.class)))
                .thenReturn(member);

        mockMvc.perform(getMemberById(memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.sync").doesNotExist());
    }

    private static SyncTarget targetFor(MemberId memberId) {
        return new SyncTarget(SyncEntityType.MEMBER, memberId.uuid().toString());
    }
}
