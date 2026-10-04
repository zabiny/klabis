package com.klabis.members.infrastructure.restapi;

import com.klabis.members.MembersWebMvcTest;
import com.klabis.common.TargetGrant;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.authorization.TargetType;
import com.klabis.common.settings.OrisClubKeyManagementPort;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.MonetaryAmount;
import com.klabis.members.OwnedGroup;
import com.klabis.members.application.*;
import com.klabis.members.domain.*;
import com.klabis.members.domain.DeactivationReason;
import com.klabis.members.domain.Gender;
import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.*;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API tests for MemberController.
 * <p>
 * Tests validation, error handling, and HATEOAS link generation.
 * <p>
 * <b>Test Scope:</b> Controller unit tests focus on:
 * <ul>
 *   <li>HTTP status codes (200, 201, 204, 400, 403, 404, etc.)</li>
 *   <li>Service method invocation with expected parameters (using Mockito.verify)</li>
 *   <li>HATEOAS links presence and basic structure</li>
 *   <li>Exception handling and error responses</li>
 * </ul>
 * <p>
 * <b>What is NOT tested here:</b>
 * <ul>
 *   <li>Detailed JSON field-by-field mapping - tested in {@link MemberMappingTests}</li>
 *   <li>Domain logic - tested in domain unit tests</li>
 *   <li>Integration flows - tested in integration/E2E tests</li>
 * </ul>
 */
@DisplayName("Member Controller API Tests")
@MembersWebMvcTest
class MemberControllerApiTest {

    private static final String ADMIN_USERNAME = "ZBM0001";
    private static final String MEMBER_USERNAME = "ZBM0101";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.klabis.members.application.MemberCompletenessPort memberCompletenessPort;

    @Autowired
    private ManagementPort managementService;

    @Autowired
    private RegistrationPort registrationService;

    @Autowired
    private OrisClubKeyManagementPort orisClubKeyManagementPort;

    @Autowired
    private SynchronizationPort synchronizationPort;

    @BeforeEach
    void stubSynchronizationPortAbsentByDefault() {
        when(synchronizationPort.findByTarget(any())).thenReturn(Optional.empty());
    }

    @Autowired
    private com.klabis.members.application.MemberAccountActivationPort accountActivationPort;

    @Autowired
    private com.klabis.groups.traininggroup.application.TrainingGroupManagementPort trainingGroupManagementPort;

    @Autowired
    private LegalGuardianGroupPort legalGuardianGroupPort;

    @Nested
    @DisplayName("GET /api/members/{id}")
    class GetMemberTests {

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
                    .withAddress(new Address("Hlavní 123", "Praha", "11000", "CZ"))
                    .withActive(true)
                    .withNationality("CZ")
                    .build();

            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);

            mockMvc.perform(getMemberById(member.getId()))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                    // Assert only key fields - detailed JSON structure is tested in MemberMappingTests
                    .andExpect(jsonPath("$.id").value(memberId.toString()))
                    .andExpect(jsonPath("$.registrationNumber").value("ZBM0501"))
                    .andExpect(jsonPath("$.firstName").exists())
                    .andExpect(jsonPath("$.lastName").exists())
                    .andExpect(jsonPath("$.email").exists())
                    .andExpect(jsonPath("$.phone").exists())
                    .andExpect(jsonPath("$.address").exists())
                    .andExpect(jsonPath("$.active").doesNotExist())
                    // Assert HATEOAS links presence
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.containsString(
                            "/api/members/" + memberId)))
                    .andExpect(jsonPath("$._links.collection.href").exists())
                    .andExpect(jsonPath("$._links.collection.href").value(org.hamcrest.Matchers.containsString(
                            "/api/members")));
        }

        @Test
        @DisplayName("should return 404 when member not found")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ})
        void shouldReturn404WhenMemberNotFound() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean()))
                    .thenThrow(new MemberNotFoundException(new MemberId(nonExistentId)));

            mockMvc.perform(getMemberById(nonExistentId))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(
                            nonExistentId.toString())));
        }

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:READ authority")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn403WhenUnauthorized() throws Exception {
            UUID memberId = UUID.randomUUID();
            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean()))
                    .thenReturn(MemberTestDataBuilder.aMemberWithId(memberId).build());

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            UUID memberId = UUID.randomUUID();

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should omit active field for caller without MEMBERS:MANAGE authority")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ})
        void shouldOmitActiveFieldWhenUserLacksMembersManageAuthority() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withActive(false)
                    .build();

            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);

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

            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);

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
                    .withAddress(new Address("Main Street 123", "Bratislava", "81101", "SK"))
                    .build();

            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    // Assert only address presence - detailed structure is tested in MemberMappingTests
                    .andExpect(jsonPath("$.address").isNotEmpty())
                    .andExpect(jsonPath("$.address.street").exists())
                    .andExpect(jsonPath("$.address.city").exists())
                    .andExpect(jsonPath("$.address.postalCode").exists())
                    .andExpect(jsonPath("$.address.country").exists());
        }

        @Test
        @DisplayName("HAL+FORMS: user with MEMBERS_READ permission - should include only update affordance (no permissions neither terminate)")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ})
        @Disabled("need to finish authorization for klabisAfford")
        void shouldNotIncludePermissionsLinkWhenUserLacksMembersPermissionsAuthority() throws Exception {
            UUID memberId = UUID.randomUUID();
            Address address = Address.of("Test Street", "Test City", "10000", "CZ");
            EmailAddress email = EmailAddress.of("test@example.com");
            PhoneNumber phone = PhoneNumber.of("+420123456789");

            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withName("Test", "Member")
                    .withRegistrationNumber("ZBM1234")
                    .withDateOfBirth(LocalDate.of(2000, 1, 1))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(address)
                    .withPhone(phone)
                    .withEmail(email)
                    .build();

            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.permissions").doesNotExist())
                    .andExpect(jsonPath("$._templates").exists())
                    .andExpect(jsonPath("$._templates.updateMember.method").value("PATCH"))  // "UPDATE member"
                    .andExpect(jsonPath("$._templates.updateMember.target").doesNotExist())
                    .andExpect(jsonPath("$._templates.suspendMember").doesNotExist());
        }

        @Test
        @DisplayName("HAL+FORMS: user with MEMBERS_PERMISSIONS authority: should include permissions link")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_PERMISSIONS})
        void activeMemberShouldReturnPermissionsLink() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withActive(true)
                    .build();

            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);

            mockMvc.perform(getMemberById(memberId))
                    .andDo(MockMvcResultHandlers.print())
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

            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);

            mockMvc.perform(getMemberById(memberId))
                    .andDo(MockMvcResultHandlers.print())
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

            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$._templates").exists())
                    .andExpect(jsonPath("$._templates.updateMember.method").value("PATCH"))  // "UPDATE member"
                    .andExpect(jsonPath("$._templates.updateMember.target").doesNotExist())
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

            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='gender')].options.inline[0]").value("MALE"))
                    .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='gender')].options.inline[1]").value("FEMALE"))
                    .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='drivingLicenseGroup')].options.inline[0]").value("AM"))
                    .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='drivingLicenseGroup')].options.inline[10]").value("T"))
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

            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.suspendMember.properties[?(@.name=='reason')].options.inline[0]").value("ODHLASKA"))
                    .andExpect(jsonPath("$._templates.suspendMember.properties[?(@.name=='reason')].options.inline[1]").value("PRESTUP"))
                    .andExpect(jsonPath("$._templates.suspendMember.properties[?(@.name=='reason')].options.inline[2]").value("OTHER"))
                    .andExpect(jsonPath("$._templates.suspendMember.properties[?(@.name=='reason')].type").value("DeactivationReason"));
        }

        @Test
        @DisplayName("HAL+FORMS: user with MEMBERS_MANAGE authority: should include update and resume affordances for suspended member")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void suspendedMemberShouldReturnUpdateAndResumeAffordances() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withActive(false)
                    .build();

            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false))
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
        @DisplayName("HAL+FORMS: member viewing own profile — should include update affordance pointing to PATCH /{id}")
        @WithKlabisMockUser(username = "ZBM0101", memberId = "11111111-1111-1111-1111-111111111111", authorities = {Authority.MEMBERS_READ},
                targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE, type = TargetType.MEMBER,
                        ids = "11111111-1111-1111-1111-111111111111"))
        void ownProfileShouldReturnUpdateAffordance() throws Exception {
            UUID memberId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withActive(true)
                    .withDateOfBirth(LocalDate.of(1990, 1, 1))
                    .build();

            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);

            mockMvc.perform(getMemberById(memberId))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates").exists())
                    .andExpect(jsonPath("$._templates.updateMember.method").value("PATCH"))
                    .andExpect(jsonPath("$._templates.updateMember.target").doesNotExist())
                    .andExpect(jsonPath("$._templates.suspendMember").doesNotExist());
        }

        @Test
        @DisplayName("HAL+FORMS: self-edit template shows reserved fields read-only and the other fields editable")
        @WithKlabisMockUser(username = "ZBM0101", memberId = "11111111-1111-1111-1111-111111111111", authorities = {Authority.MEMBERS_READ},
                targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE, type = TargetType.MEMBER,
                        ids = "11111111-1111-1111-1111-111111111111"))
        void selfEditTemplateShouldMarkReservedFieldsReadOnly() throws Exception {
            UUID memberId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withActive(true)
                    .withDateOfBirth(LocalDate.of(1990, 1, 1))
                    .build();
            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);

            var result = mockMvc.perform(getMemberById(memberId)).andExpect(status().isOk());

            for (String reserved : List.of("firstName", "lastName", "dateOfBirth", "gender", "birthNumber")) {
                result.andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='" + reserved + "')].readOnly")
                        .value(true));
            }
            for (String editable : List.of("email", "phone", "dietaryRestrictions", "chipNumber")) {
                result.andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='" + editable + "')]").exists())
                        .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='" + editable + "')].readOnly")
                                .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(true))));
            }
        }

        @Test
        @DisplayName("HAL+FORMS: administrator's template has reserved fields editable")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void adminTemplateShouldKeepReservedFieldsEditable() throws Exception {
            UUID memberId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            Member member = MemberTestDataBuilder.aMemberWithId(memberId).withActive(true).build();
            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='firstName')]").exists())
                    .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='firstName')].readOnly")
                            .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(true))));
        }

        @Test
        @DisplayName("holder of EDIT_PROFILE over another member sees all data and the edit template, but not admin-only data")
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
            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dateOfBirth").value("1990-01-01"))
                    .andExpect(jsonPath("$.dietaryRestrictions").value("vegan"))
                    .andExpect(jsonPath("$.gender").exists())
                    .andExpect(jsonPath("$.missingData").doesNotExist())
                    .andExpect(jsonPath("$._templates.updateMember.method").value("PATCH"))
                    .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='firstName')].readOnly")
                            .value(true))
                    .andExpect(jsonPath("$._templates.suspendMember").doesNotExist());
        }

        @Test
        @DisplayName("HAL+FORMS: minor viewing own profile - should not include update affordance")
        @WithKlabisMockUser(username = "ZBM0101", memberId = "11111111-1111-1111-1111-111111111111", authorities = {Authority.MEMBERS_READ})
        void minorOwnProfileShouldNotReturnUpdateAffordance() throws Exception {
            UUID memberId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withActive(true)
                    .withDateOfBirth(LocalDate.now().minusYears(15))
                    .build();

            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.updateMember").doesNotExist());
        }

        @Test
        @DisplayName("HAL+FORMS: member who turned 18 today viewing own profile - should include update affordance")
        @WithKlabisMockUser(username = "ZBM0101", memberId = "11111111-1111-1111-1111-111111111111", authorities = {Authority.MEMBERS_READ},
                targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE, type = TargetType.MEMBER,
                        ids = "11111111-1111-1111-1111-111111111111"))
        void justTurnedAdultOwnProfileShouldReturnUpdateAffordance() throws Exception {
            UUID memberId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withActive(true)
                    .withDateOfBirth(LocalDate.now().minusYears(18))
                    .build();

            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.updateMember.method").value("PATCH"));
        }

        @Test
        @DisplayName("HAL+FORMS: admin viewing a minor profile - should include update affordance")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void adminViewingMinorShouldReturnUpdateAffordance() throws Exception {
            UUID memberId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withActive(true)
                    .withDateOfBirth(LocalDate.now().minusYears(15))
                    .build();

            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.updateMember.method").value("PATCH"));
        }

        @Test
        @DisplayName("HAL+FORMS: member viewing another member's profile — should not include any PATCH affordance")
        @WithKlabisMockUser(username = "ZBM0101", memberId = "22222222-2222-2222-2222-222222222222", authorities = {Authority.MEMBERS_READ})
        void otherMemberProfileShouldNotReturnAnyPatchAffordance() throws Exception {
            UUID memberId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withActive(true)
                    .build();

            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);

            mockMvc.perform(getMemberById(memberId))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates").doesNotExist());
        }

        @Test
        @DisplayName("non-admin user gets 404 when accessing detail of an inactive member")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void nonAdminUserGets404ForInactiveMember() throws Exception {
            UUID memberId = UUID.randomUUID();
            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), eq(false)))
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
            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), eq(true)))
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
            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean()))
                    .thenReturn(member);
            com.klabis.groups.traininggroup.domain.TrainingGroup mockTrainingGroup =
                    Mockito.mock(com.klabis.groups.traininggroup.domain.TrainingGroup.class);
            Mockito.when(mockTrainingGroup.getId())
                    .thenReturn(new com.klabis.groups.traininggroup.TrainingGroupId(groupId));
            when(trainingGroupManagementPort.findTrainingGroupOfMember(any(MemberId.class)))
                    .thenReturn(java.util.Optional.of(mockTrainingGroup));
            when(legalGuardianGroupPort.findGroupOf(any(MemberId.class)))
                    .thenReturn(java.util.Optional.empty());

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.trainingGroup.href")
                            .value(org.hamcrest.Matchers.endsWith("/api/training-groups/" + groupId)))
                    .andExpect(jsonPath("$._links.legalGuardianGroup").doesNotExist());
        }

        @Test
        @DisplayName("should include legalGuardianGroup link when member belongs to a legal guardian group")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void shouldIncludeLegalGuardianGroupLinkWhenMemberBelongsToGroup() throws Exception {
            UUID memberId = UUID.randomUUID();
            UUID groupId = UUID.fromString("11111111-2222-3333-4444-555555555555");
            Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();
            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean()))
                    .thenReturn(member);
            when(trainingGroupManagementPort.findTrainingGroupOfMember(any(MemberId.class)))
                    .thenReturn(java.util.Optional.empty());
            LegalGuardianGroup mockLegalGuardianGroup =
                    Mockito.mock(LegalGuardianGroup.class);
            Mockito.when(mockLegalGuardianGroup.getId())
                    .thenReturn(new LegalGuardianGroupId(groupId));
            when(legalGuardianGroupPort.findGroupOf(any(MemberId.class)))
                    .thenReturn(java.util.Optional.of(mockLegalGuardianGroup));

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.trainingGroup").doesNotExist())
                    .andExpect(jsonPath("$._links.legalGuardianGroup.href")
                            .value(org.hamcrest.Matchers.endsWith("/api/legal-guardian-groups/" + groupId)));
        }

        @Test
        @DisplayName("should not include group links when member belongs to no groups")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldNotIncludeGroupLinksWhenMemberBelongsToNoGroups() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();
            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean()))
                    .thenReturn(member);
            when(trainingGroupManagementPort.findTrainingGroupOfMember(any(MemberId.class)))
                    .thenReturn(java.util.Optional.empty());
            when(legalGuardianGroupPort.findGroupOf(any(MemberId.class)))
                    .thenReturn(java.util.Optional.empty());

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.trainingGroup").doesNotExist())
                    .andExpect(jsonPath("$._links.legalGuardianGroup").doesNotExist());
        }

        @Test
        @DisplayName("member viewing own profile — ical-token link is present")
        @WithKlabisMockUser(username = MEMBER_USERNAME, memberId = "11111111-1111-1111-1111-111111111111", authorities = {Authority.MEMBERS_READ})
        void ownProfileShouldIncludeIcalTokenLink() throws Exception {
            UUID memberId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            Member member = MemberTestDataBuilder.aMemberWithId(memberId).withActive(true).build();
            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean()))
                    .thenReturn(member);
            when(trainingGroupManagementPort.findTrainingGroupOfMember(any(MemberId.class)))
                    .thenReturn(java.util.Optional.empty());
            when(legalGuardianGroupPort.findGroupOf(any(MemberId.class)))
                    .thenReturn(java.util.Optional.empty());

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links['ical-token'].href").value(org.hamcrest.Matchers.containsString("/api/me/ical-token")));
        }

        @Test
        @DisplayName("member viewing another member's profile — ical-token link is absent")
        @WithKlabisMockUser(username = MEMBER_USERNAME, memberId = "22222222-2222-2222-2222-222222222222", authorities = {Authority.MEMBERS_READ})
        void otherMemberProfileShouldNotIncludeIcalTokenLink() throws Exception {
            UUID memberId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            Member member = MemberTestDataBuilder.aMemberWithId(memberId).withActive(true).build();
            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean()))
                    .thenReturn(member);
            when(trainingGroupManagementPort.findTrainingGroupOfMember(any(MemberId.class)))
                    .thenReturn(java.util.Optional.empty());
            when(legalGuardianGroupPort.findGroupOf(any(MemberId.class)))
                    .thenReturn(java.util.Optional.empty());

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links['ical-token']").doesNotExist());
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
            when(managementService.getMemberAndRecordView(eq(new MemberId(minorId)), any(UserId.class), anyBoolean())).thenReturn(minor);
            when(managementService.getMemberAndRecordView(eq(new MemberId(adultId)), any(UserId.class), anyBoolean())).thenReturn(adult);

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
            when(managementService.getMemberAndRecordView(eq(new MemberId(availableId)), any(UserId.class), anyBoolean())).thenReturn(available);
            when(managementService.getMemberAndRecordView(eq(new MemberId(unavailableId)), any(UserId.class), anyBoolean())).thenReturn(unavailable);
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
            when(managementService.getMemberAndRecordView(eq(new MemberId(memberId)), any(UserId.class), anyBoolean())).thenReturn(member);
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

            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);
            when(memberCompletenessPort.missingData(eq(member), any())).thenReturn(java.util.Set.of(com.klabis.members.domain.MissingDataItem.ADDRESS));

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.address").doesNotExist())
                    .andExpect(jsonPath("$.missingData").value(org.hamcrest.Matchers.hasItem("ADDRESS")));
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

            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);
            when(memberCompletenessPort.missingData(eq(member), any())).thenReturn(java.util.Set.of(com.klabis.members.domain.MissingDataItem.PHONE));

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

            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);

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

            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean())).thenReturn(member);

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
            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean()))
                    .thenReturn(member);
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), targetFor(new MemberId(memberId)),
                    new ExternalReference(ExternalSystem.ORIS, "100"));
            when(synchronizationPort.findByTarget(targetFor(new MemberId(memberId)))).thenReturn(Optional.of(record));

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.sync.href").exists());
        }

        @Test
        @DisplayName("should not include sync link for a hand-registered member")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldNotIncludeSyncLinkForHandRegisteredMember() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();
            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean()))
                    .thenReturn(member);

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.sync").doesNotExist());
        }

        private static SyncTarget targetFor(MemberId memberId) {
            return new SyncTarget(SyncEntityType.MEMBER, memberId.uuid().toString());
        }
    }

    @Nested
    @DisplayName("POST /api/members")
    class RegisterMemberTests {

        static ResultMatcher locationHeaderWithMemberDetailRedirect(UUID expectedMemberId) {
            return header().string(HttpHeaders.LOCATION,
                    "http://localhost/api/members/%s".formatted(expectedMemberId.toString()));
        }

        private MockHttpServletRequestBuilder postMembers() {
            return post("/api/members")
                    .contentType(MediaType.APPLICATION_JSON);
        }

        @Test
        @DisplayName("should call service with correct personal information arguments")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {com.klabis.common.users.Authority.MEMBERS_MANAGE})
        void shouldCallServiceWithCorrectPersonalInformation() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();
            when(registrationService.registerMember(any(RegistrationPort.RegisterNewMember.class))).thenReturn(member);

            mockMvc.perform(postMembers().content("""
                    {
                        "firstName": "Jan",
                        "lastName": "Novák",
                        "dateOfBirth": "2000-06-15",
                        "nationality": "CZ",
                        "gender": "MALE",
                        "email": "jan.novak@example.com",
                        "phone": "+420777123456",
                        "address": {
                            "street": "Hlavní 123",
                            "city": "Praha",
                            "postalCode": "11000",
                            "country": "CZ"
                        }
                    }
                    """));

            Mockito.verify(registrationService).registerMember(argThat(cmd ->
                    cmd.personalInformation().getFirstName().equals("Jan") &&
                    cmd.personalInformation().getLastName().equals("Novák") &&
                    cmd.personalInformation().getDateOfBirth().equals(LocalDate.of(2000, 6, 15)) &&
                    cmd.personalInformation().getNationalityCode().equals("CZ") &&
                    cmd.personalInformation().getGender() == Gender.MALE
            ));
        }

        @Test
        @DisplayName("should call service with correct address arguments")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {com.klabis.common.users.Authority.MEMBERS_MANAGE})
        void shouldCallServiceWithCorrectAddress() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();
            when(registrationService.registerMember(any(RegistrationPort.RegisterNewMember.class))).thenReturn(member);

            mockMvc.perform(postMembers().content("""
                    {
                        "firstName": "Jan",
                        "lastName": "Novák",
                        "dateOfBirth": "2000-06-15",
                        "nationality": "CZ",
                        "gender": "MALE",
                        "email": "jan.novak@example.com",
                        "phone": "+420777123456",
                        "address": {
                            "street": "Hlavní 123",
                            "city": "Praha",
                            "postalCode": "11000",
                            "country": "CZ"
                        }
                    }
                    """));

            Mockito.verify(registrationService).registerMember(argThat(cmd ->
                    cmd.address() != null &&
                    cmd.address().street().equals("Hlavní 123") &&
                    cmd.address().city().equals("Praha") &&
                    cmd.address().postalCode().equals("11000") &&
                    cmd.address().country().equals("CZ")
            ));
        }

        @Test
        @DisplayName("should call service with correct email and phone arguments")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {com.klabis.common.users.Authority.MEMBERS_MANAGE})
        void shouldCallServiceWithCorrectEmailAndPhone() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();
            when(registrationService.registerMember(any(RegistrationPort.RegisterNewMember.class))).thenReturn(member);

            mockMvc.perform(postMembers().content("""
                    {
                        "firstName": "Jan",
                        "lastName": "Novák",
                        "dateOfBirth": "2000-06-15",
                        "nationality": "CZ",
                        "gender": "MALE",
                        "email": "jan.novak@example.com",
                        "phone": "+420777123456",
                        "address": {
                            "street": "Hlavní 123",
                            "city": "Praha",
                            "postalCode": "11000",
                            "country": "CZ"
                        }
                    }
                    """));

            Mockito.verify(registrationService).registerMember(argThat(cmd ->
                    cmd.email().value().equals("jan.novak@example.com") &&
                    cmd.phone().value().equals("+420777123456")
            ));
        }

        @Test
        @DisplayName("should call service with correct birthNumber and bankAccountNumber when provided")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {com.klabis.common.users.Authority.MEMBERS_MANAGE})
        void shouldCallServiceWithCorrectBirthNumberAndBankAccount() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();
            when(registrationService.registerMember(any(RegistrationPort.RegisterNewMember.class))).thenReturn(member);

            mockMvc.perform(postMembers().content("""
                    {
                        "firstName": "Jan",
                        "lastName": "Novák",
                        "dateOfBirth": "2000-06-15",
                        "nationality": "CZ",
                        "gender": "MALE",
                        "email": "jan.novak@example.com",
                        "phone": "+420777123456",
                        "address": {
                            "street": "Hlavní 123",
                            "city": "Praha",
                            "postalCode": "11000",
                            "country": "CZ"
                        },
                        "birthNumber": "0001011234",
                        "bankAccountNumber": "123456789/2010"
                    }
                    """));

            Mockito.verify(registrationService).registerMember(argThat(cmd ->
                    cmd.birthNumber() != null &&
                    cmd.birthNumber().value().equals("000101/1234") &&
                    cmd.bankAccountNumber() != null &&
                    cmd.bankAccountNumber().value().equals("123456789/2010")
            ));
        }

        @Test
        @DisplayName("with valid data should return 201 with HATEOAS links")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {com.klabis.common.users.Authority.MEMBERS_MANAGE})
        void shouldCreateMemberWithValidData() throws Exception {
            UUID memberId = UUID.randomUUID();

            Address address = Address.of("Test Street", "Test City", "10000", "CZ");
            EmailAddress email = EmailAddress.of("test@example.com");
            PhoneNumber phone = PhoneNumber.of("+420123456789");

            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withName("Test", "Member")
                    .withRegistrationNumber("ZBM1234")
                    .withDateOfBirth(LocalDate.of(2000, 1, 1))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(address)
                    .withPhone(phone)
                    .withEmail(email)
                    .build();

            when(registrationService.registerMember(any(RegistrationPort.RegisterNewMember.class))).thenReturn(member);

            mockMvc.perform(
                            postMembers().content("""
                                    {
                                        "firstName": "Jan",
                                        "lastName": "Novák",
                                        "dateOfBirth": "2000-06-15",
                                        "nationality": "CZ",
                                        "gender": "MALE",
                                        "email": "jan.novak@example.com",
                                        "phone": "+420777123456",
                                        "address": {
                                            "street": "Hlavní 123",
                                            "city": "Praha",
                                            "postalCode": "11000",
                                            "country": "CZ"
                                        }
                                    }
                                    """)
                    )
                    .andExpect(status().isCreated())
                    .andExpect(locationHeaderWithMemberDetailRedirect(memberId));
        }

        @Test
        @DisplayName("with minor should pass existing and new legal guardians to the service")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
        void shouldPassLegalGuardiansOfMinor() throws Exception {
            UUID memberId = UUID.randomUUID();
            UUID existingGuardian = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();
            when(registrationService.registerMember(any(RegistrationPort.RegisterNewMember.class))).thenReturn(member);

            mockMvc.perform(postMembers().content("""
                            {
                                "firstName": "Petra",
                                "lastName": "Nováková",
                                "dateOfBirth": "2010-06-20",
                                "nationality": "CZ",
                                "gender": "FEMALE",
                                "address": {
                                    "street": "Hlavní 456",
                                    "city": "Brno",
                                    "postalCode": "60000",
                                    "country": "CZ"
                                },
                                "legalGuardians": [
                                    {"userId": "%s"},
                                    {"firstName": "Eva", "lastName": "Nováková",
                                     "email": "eva@example.com", "phone": "+420123456789"}
                                ]
                            }
                            """.formatted(existingGuardian))
                    )
                    .andExpect(status().isCreated())
                    .andExpect(locationHeaderWithMemberDetailRedirect(memberId));

            Mockito.verify(registrationService).registerMember(argThat(cmd ->
                    cmd.email() == null && cmd.phone() == null &&
                    cmd.takenOverLegalGuardian() == null &&
                    cmd.legalGuardians().size() == 2 &&
                    existingGuardian.equals(cmd.legalGuardians().get(0).userId().uuid()) &&
                    "eva@example.com".equals(cmd.legalGuardians().get(1).newGuardian().email())
            ));
        }

        @Test
        @DisplayName("with legalGuardianUserId should pass the guardian to take over")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
        void shouldPassTakenOverLegalGuardian() throws Exception {
            UUID memberId = UUID.randomUUID();
            UUID guardian = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();
            when(registrationService.registerMember(any(RegistrationPort.RegisterNewMember.class))).thenReturn(member);

            mockMvc.perform(postMembers().content("""
                            {
                                "firstName": "Eva",
                                "lastName": "Svobodová",
                                "dateOfBirth": "1985-06-20",
                                "nationality": "CZ",
                                "gender": "FEMALE",
                                "email": "eva@example.com",
                                "phone": "+420111222333",
                                "address": {
                                    "street": "Hlavní 456",
                                    "city": "Brno",
                                    "postalCode": "60000",
                                    "country": "CZ"
                                },
                                "legalGuardianUserId": "%s"
                            }
                            """.formatted(guardian))
                    )
                    .andExpect(status().isCreated());

            Mockito.verify(registrationService).registerMember(argThat(cmd ->
                    cmd.takenOverLegalGuardian() != null && guardian.equals(cmd.takenOverLegalGuardian().uuid()) &&
                    cmd.legalGuardians().isEmpty()
            ));
        }

        @Test
        @DisplayName("with missing first name should return 400")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WhenFirstNameMissing() throws Exception {
            mockMvc.perform(postMembers().content("""
                            {
                                "firstName": "",
                                "lastName": "Novák",
                                "dateOfBirth": "2000-06-15",
                                "nationality": "CZ",
                                "gender": "MALE",
                                "email": "jan@example.com",
                                "phone": "+420777123456",
                                "address": {
                                    "street": "Hlavní 123",
                                    "city": "Praha",
                                    "postalCode": "11000",
                                    "country": "CZ"
                                }
                            }
                            """)
                    )
                    .andExpect(status().isBadRequest())

                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors.firstName").value("must not be blank"));
        }

        @Test
        @DisplayName("with invalid email should return 400")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WhenEmailInvalid() throws Exception {
            mockMvc.perform(postMembers().content("""
                            {
                                "firstName": "Jan",
                                "lastName": "Novák",
                                "dateOfBirth": "2005-05-15",
                                "nationality": "CZ",
                                "gender": "MALE",
                                "email": "invalid-email",
                                "phone": "+420777123456",
                                "address": {
                                    "street": "Hlavní 123",
                                    "city": "Praha",
                                    "postalCode": "11000",
                                    "country": "CZ"
                                }
                            }
                            """)
                    )
                    .andExpect(status().isBadRequest())

                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors.email").value("must be a well-formed email address"));
        }

        @Test
        @DisplayName("with invalid phone should return 400")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WhenPhoneInvalid() throws Exception {
            mockMvc.perform(postMembers().content("""
                            {
                                "firstName": "Jan",
                                "lastName": "Novák",
                                "dateOfBirth": "2005-05-15",
                                "nationality": "CZ",
                                "gender": "MALE",
                                "email": "jan@example.com",
                                "phone": "123",
                                "address": {
                                    "street": "Hlavní 123",
                                    "city": "Praha",
                                    "postalCode": "11000",
                                    "country": "CZ"
                                }
                            }
                            """)
                    )
                    .andExpect(status().isBadRequest())

                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors.phone").value(
                            "must match \"^\\+[0-9\\s]{7,20}$\""));
        }

        @Test
        @DisplayName("with future date of birth should return 400")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WhenDateOfBirthInFuture() throws Exception {
            mockMvc.perform(postMembers().content("""
                            {
                                "firstName": "Jan",
                                "lastName": "Novák",
                                "dateOfBirth": "2120-12-10",
                                "nationality": "CZ",
                                "gender": "MALE",
                                "email": "jan@example.com",
                                "phone": "+420777123456",
                                "address": {
                                    "street": "Hlavní 123",
                                    "city": "Praha",
                                    "postalCode": "11000",
                                    "country": "CZ"
                                }
                            }
                            """))
                    .andExpect(status().isBadRequest())

                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors.dateOfBirth").value("must be a past date"));
        }

        @Test
        @DisplayName("with blank email should leave the requirement of own contacts to the service")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
        void shouldPassBlankEmailAsAbsentToService() throws Exception {
            UUID memberId = UUID.randomUUID();
            when(registrationService.registerMember(any(RegistrationPort.RegisterNewMember.class)))
                    .thenThrow(new IllegalArgumentException("At least one email address is required (member or guardian)"));

            mockMvc.perform(postMembers().content("""
                            {
                                "firstName": "Jan",
                                "lastName": "Novák",
                                "dateOfBirth": "2005-05-15",
                                "nationality": "CZ",
                                "gender": "MALE",
                                "email": "",
                                "phone": "+420777123456",
                                "address": {
                                    "street": "Hlavní 123",
                                    "city": "Praha",
                                    "postalCode": "11000",
                                    "country": "CZ"
                                }
                            }
                            """)
                    )
                    .andExpect(status().isBadRequest());

            Mockito.verify(registrationService).registerMember(argThat(cmd -> cmd.email() == null));
        }

        @Test
        @DisplayName("with valid address and contacts should succeed")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
        void shouldCreateMemberWithValidAddressAndContacts() throws Exception {
            UUID memberId = UUID.randomUUID();

            Address address = Address.of("Test Street", "Test City", "10000", "CZ");
            EmailAddress email = EmailAddress.of("test@example.com");
            PhoneNumber phone = PhoneNumber.of("+420123456789");

            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withName("Test", "Member")
                    .withRegistrationNumber("ZBM1234")
                    .withDateOfBirth(LocalDate.of(2000, 1, 1))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(address)
                    .withPhone(phone)
                    .withEmail(email)
                    .build();

            when(registrationService.registerMember(any(RegistrationPort.RegisterNewMember.class))).thenReturn(member);

            mockMvc.perform(postMembers().content("""
                            {
                                "firstName": "Jan",
                                "lastName": "Novák",
                                "dateOfBirth": "2005-05-15",
                                "nationality": "CZ",
                                "gender": "MALE",
                                "email": "jan.novak@example.com",
                                "phone": "+420777123456",
                                "address": {
                                    "street": "Hlavní 123",
                                    "city": "Praha",
                                    "postalCode": "11000",
                                    "country": "CZ"
                                }
                            }
                            """)
                    )
                    .andExpect(status().isCreated())
                    .andExpect(locationHeaderWithMemberDetailRedirect(memberId));
        }

        @Test
        @DisplayName("with invalid nationality code should return 400")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WhenNationalityCodeInvalid() throws Exception {
            mockMvc.perform(postMembers().content("""
                            {
                                "firstName": "Jan",
                                "lastName": "Novák",
                                "dateOfBirth": "2005-05-15",
                                "nationality": "CZECH",
                                "gender": "MALE",
                                "email": "jan@example.com",
                                "phone": "+420777123456",
                                "address": {
                                    "street": "Hlavní 123",
                                    "city": "Praha",
                                    "postalCode": "11000",
                                    "country": "CZ"
                                }
                            }
                            """)
                    )
                    .andExpect(status().isBadRequest())

                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors.nationality").value(
                            "size must be between 2 and 2"));
        }

        @Test
        @DisplayName("with invalid address missing fields should return 400")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WhenAddressMissingFields() throws Exception {
            mockMvc.perform(postMembers().content("""
                            {
                                "firstName": "Jan",
                                "lastName": "Novák",
                                "dateOfBirth": "2005-05-15",
                                "nationality": "CZ",
                                "gender": "MALE",
                                "email": "jan@example.com",
                                "phone": "+420777123456",
                                "address": {
                                    "street": "",
                                    "city": "Praha",
                                    "postalCode": "11000",
                                    "country": "CZ"
                                }
                            }
                            """)
                    )
                    .andExpect(status().isBadRequest())

                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors['address.street']").value("must not be blank"));
        }

        @Test
        @DisplayName("with invalid address country code format should return 400")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WhenAddressCountryInvalid() throws Exception {
            mockMvc.perform(postMembers().content("""
                            {
                                "firstName": "Jan",
                                "lastName": "Novák",
                                "dateOfBirth": "2005-05-15",
                                "nationality": "CZ",
                                "gender": "MALE",
                                "email": "jan@example.com",
                                "phone": "+420777123456",
                                "address": {
                                    "street": "Hlavní 123",
                                    "city": "Praha",
                                    "postalCode": "11000",
                                    "country": "X"
                                }
                            }
                            """)
                    )
                    .andExpect(status().isBadRequest())

                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors['address.country']").value("must match \"^[A-Za-z]{2}$\""));
        }
    }

    @Nested
    @DisplayName("GET /api/members")
    class ListMembersTests {
        private MockHttpServletRequestBuilder getApiMembers() {
            return get("/api/members")
                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE);
        }

        @Test
        @DisplayName("should return 200 with empty collection when no members exist")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldReturnEmptyCollectionWhenNoMembers() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(
                    List.of()));

            mockMvc.perform(getApiMembers())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList").doesNotExist())
                    .andExpect(jsonPath("$._links.self.href").exists());
        }

        @Test
        @DisplayName("should call repository with correct default pagination parameters")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldCallRepositoryWithDefaultPagination() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(getApiMembers());

            Mockito.verify(managementService).listMembers(any(MemberFilter.class), argThat(pageable ->
                    pageable.getPageNumber() == 0 &&
                    pageable.getPageSize() == 10 &&
                    pageable.getSort().getOrderFor("lastName") != null &&
                    pageable.getSort().getOrderFor("lastName").isAscending()
            ));
        }

        @Test
        @DisplayName("should call repository with custom page and size parameters")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldCallRepositoryWithCustomPagination() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(getApiMembers()
                    .param("page", "2")
                    .param("size", "20"));

            Mockito.verify(managementService).listMembers(any(MemberFilter.class), argThat(pageable ->
                    pageable.getPageNumber() == 2 &&
                    pageable.getPageSize() == 20
            ));
        }

        @Test
        @DisplayName("should call repository with correct sort parameters")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldCallRepositoryWithCorrectSortParameters() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(getApiMembers()
                    .param("sort", "firstName,desc")
                    .param("sort", "registrationNumber,asc"));

            Mockito.verify(managementService).listMembers(any(MemberFilter.class), argThat(pageable ->
                    pageable.getSort().getOrderFor("firstName") != null &&
                    pageable.getSort().getOrderFor("firstName").isDescending() &&
                    pageable.getSort().getOrderFor("registrationNumber") != null &&
                    pageable.getSort().getOrderFor("registrationNumber").isAscending()
            ));
        }

        @Test
        @DisplayName("should return correct JSON structure with page metadata")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldReturnCorrectJsonStructureWithPageMetadata() throws Exception {
            UUID memberId1 = UUID.randomUUID();
            UUID memberId2 = UUID.randomUUID();
            Member member1 = MemberTestDataBuilder.aMemberWithId(memberId1)
                    .withFirstName("Jan")
                    .withLastName("Novák")
                    .withRegistrationNumber(RegistrationNumber.of("ZBM0001"))
                    .build();
            Member member2 = MemberTestDataBuilder.aMemberWithId(memberId2)
                    .withFirstName("Petra")
                    .withLastName("Svobodová")
                    .withRegistrationNumber(RegistrationNumber.of("ZBM0002"))
                    .build();

            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(member1, member2), PageRequest.of(0, 10), 2));

            mockMvc.perform(getApiMembers())
                    .andExpect(status().isOk())
                    // Assert only basic structure - detailed member field mapping is tested in MemberMappingTests
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList").isArray())
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList.length()").value(2))
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0].id").value(memberId1.toString()))
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0]._links.self.href").exists())
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[1].id").value(memberId2.toString()))
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[1]._links.self.href").exists())
                    // Assert page metadata
                    .andExpect(jsonPath("$.page.size").value(10))
                    .andExpect(jsonPath("$.page.totalElements").value(2))
                    .andExpect(jsonPath("$.page.totalPages").value(1))
                    .andExpect(jsonPath("$.page.number").value(0))
                    .andExpect(jsonPath("$._links.self.href").exists());
        }

        @Test
        @DisplayName("HAL+FORMS: user with MEMBERS_MANAGE should see registerMember template on collection")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void adminShouldSeeRegisterMemberTemplateOnCollection() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(getApiMembers())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.registerMember").exists())
                    .andExpect(jsonPath("$._templates.registerMember.method").value("POST"));
        }

        @Test
        @DisplayName("HAL+FORMS: registerMember template carries inline gender options")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void registerMemberTemplateCarriesGenderInlineOptions() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(getApiMembers())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.registerMember.properties[?(@.name=='gender')].options.inline[0]").value("MALE"))
                    .andExpect(jsonPath("$._templates.registerMember.properties[?(@.name=='gender')].options.inline[1]").value("FEMALE"))
                    .andExpect(jsonPath("$._templates.registerMember.properties[?(@.name=='gender')].type").value("Gender"));
        }

        @Test
        @DisplayName("HAL+FORMS: registerMember template binds legal guardian options to the legalGuardians array property")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void registerMemberTemplateCarriesLegalGuardianOptions() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(getApiMembers())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.registerMember.properties[?(@.name=='legalGuardians')].options.link.href")
                            .value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("/api/legal-guardian-options"))))
                    .andExpect(jsonPath("$._templates.registerMember.properties[?(@.name=='legalGuardianUserId')].options.link.href")
                            .value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("kind=LEGAL_GUARDIAN"))));
        }

        @Test
        @DisplayName("HAL+FORMS: collection updateMember template carries inline gender and drivingLicenseGroup options")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void collectionUpdateMemberTemplateCarriesGenderAndDrivingLicenseInlineOptions() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(getApiMembers())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='gender')].options.inline[0]").value("MALE"))
                    .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='drivingLicenseGroup')].options.inline[0]").value("AM"))
                    .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='drivingLicenseGroup')].options.inline[10]").value("T"));
        }

        @Test
        @DisplayName("HAL+FORMS: user without MEMBERS_MANAGE should not see registerMember template on collection")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void memberShouldNotSeeRegisterMemberTemplateOnCollection() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(getApiMembers())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.registerMember").doesNotExist());
        }

        @Test
        @DisplayName("should return 400 when sort field invalid")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldReturn400WhenSortFieldInvalid() throws Exception {
            mockMvc.perform(getApiMembers()
                            .param("sort", "invalidField,asc")
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(
                            "Invalid sort field")));
        }

        @Test
        @DisplayName("admin (MEMBERS:MANAGE) should see email and active in summary items")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void adminShouldSeeEmailAndActiveInSummaryItems() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withEmail("jan.novak@example.com")
                    .withActive(true)
                    .build();
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(member), PageRequest.of(0, 10), 1));

            mockMvc.perform(getApiMembers())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0].email").value("jan.novak@example.com"))
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0].active").value(true));
        }

        @Test
        @DisplayName("7.2 — admin (MEMBERS:MANAGE) sees dataIncomplete in summary items")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void adminShouldSeeDataIncompleteInSummaryItems() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withDateOfBirth(LocalDate.of(1990, 1, 1))
                    .withPhone((PhoneNumber) null)
                    .withDataIncomplete(true)
                    .build();
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(member), PageRequest.of(0, 10), 1));

            mockMvc.perform(getApiMembers())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0].dataIncomplete").value(true));
        }

        @Test
        @DisplayName("7.2 — caller without MEMBERS:MANAGE does not see dataIncomplete in summary items")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void nonManageCallerShouldNotSeeDataIncompleteInSummaryItems() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withDateOfBirth(LocalDate.of(1990, 1, 1))
                    .withPhone((PhoneNumber) null)
                    .withDataIncomplete(true)
                    .build();
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(member), PageRequest.of(0, 10), 1));

            mockMvc.perform(getApiMembers())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0].dataIncomplete").doesNotExist());
        }

        @Test
        @DisplayName("non-admin (only MEMBERS:READ) should not see email and active in summary items")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void nonAdminShouldNotSeeEmailAndActiveInSummaryItems() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withEmail("jan.novak@example.com")
                    .withActive(true)
                    .build();
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(member), PageRequest.of(0, 10), 1));

            mockMvc.perform(getApiMembers())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0].email").doesNotExist())
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0].active").doesNotExist());
        }

        @Test
        @DisplayName("HAL+FORMS: admin should see suspendMember template on active summary item")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void adminShouldSeeSuspendMemberTemplateOnActiveSummaryItem() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withActive(true)
                    .build();
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(member), PageRequest.of(0, 10), 1));

            mockMvc.perform(getApiMembers())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0]._templates.suspendMember.method").value("POST"))
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0]._templates.updateMember.method").value("PATCH"))
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0]._templates.resumeMember").doesNotExist());
        }

        @Test
        @DisplayName("HAL+FORMS: admin should see resumeMember template on inactive summary item")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void adminShouldSeeResumeMemberTemplateOnInactiveSummaryItem() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withActive(false)
                    .build();
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(member), PageRequest.of(0, 10), 1));

            mockMvc.perform(getApiMembers())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0]._templates.resumeMember.method").value("POST"))
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0]._templates.updateMember.method").value("PATCH"))
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0]._templates.suspendMember").doesNotExist());
        }

        @Test
        @DisplayName("HAL+FORMS: non-admin should not see action templates on summary items")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void nonAdminShouldNotSeeActionTemplatesOnSummaryItems() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withActive(true)
                    .build();
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(member), PageRequest.of(0, 10), 1));

            mockMvc.perform(getApiMembers())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0]._templates").doesNotExist());
        }

        @Test
        @DisplayName("HAL+FORMS: user with MEMBERS:PERMISSIONS should see permissions link on active summary item")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_PERMISSIONS})
        void userWithPermissionsAuthorityShouldSeePermissionsLinkOnActiveSummaryItem() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withActive(true)
                    .build();
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(member), PageRequest.of(0, 10), 1));

            mockMvc.perform(getApiMembers())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0]._links.permissions.href")
                            .value("http://localhost/api/users/" + memberId + "/permissions"));
        }

        @Test
        @DisplayName("HAL+FORMS: user with MEMBERS:PERMISSIONS should not see permissions link on inactive summary item")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_PERMISSIONS})
        void userWithPermissionsAuthorityShouldNotSeePermissionsLinkOnInactiveSummaryItem() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withActive(false)
                    .build();
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(member), PageRequest.of(0, 10), 1));

            mockMvc.perform(getApiMembers())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0]._links.permissions").doesNotExist());
        }

        @Test
        @DisplayName("HAL+FORMS: user without MEMBERS:PERMISSIONS should not see permissions link on summary items")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void userWithoutPermissionsAuthorityShouldNotSeePermissionsLinkOnSummaryItems() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withActive(true)
                    .build();
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(member), PageRequest.of(0, 10), 1));

            mockMvc.perform(getApiMembers())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0]._links.permissions").doesNotExist());
        }

        @Test
        @DisplayName("non-admin user receives only active members in paginated list (status forced to ACTIVE)")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void nonAdminUserReceivesOnlyActiveMembers() throws Exception {
            UUID activeMemberId = UUID.randomUUID();
            Member activeMember = MemberTestDataBuilder.aMemberWithId(activeMemberId).withActive(true).build();

            when(managementService.listMembers(
                    argThat(filter -> filter.status() == MemberFilter.StatusFilter.ACTIVE),
                    any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(activeMember)));

            mockMvc.perform(get("/api/members").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList.length()").value(1))
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0].id").value(activeMemberId.toString()))
                    .andExpect(jsonPath("$.page.totalElements").value(1));
        }

        @Test
        @DisplayName("admin user with no status param defaults to ACTIVE filter")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void adminUserDefaultsToActiveFilter() throws Exception {
            UUID activeMemberId = UUID.randomUUID();
            Member activeMember = MemberTestDataBuilder.aMemberWithId(activeMemberId).withActive(true).build();

            when(managementService.listMembers(
                    argThat(filter -> filter.status() == MemberFilter.StatusFilter.ACTIVE),
                    any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(activeMember)));

            mockMvc.perform(get("/api/members").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList.length()").value(1))
                    .andExpect(jsonPath("$.page.totalElements").value(1));
        }

        @Test
        @DisplayName("admin user with status=ALL receives both active and inactive members")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void adminUserWithAllStatusReceivesBothActiveAndInactiveMembers() throws Exception {
            UUID activeMemberId = UUID.randomUUID();
            UUID inactiveMemberId = UUID.randomUUID();
            Member activeMember = MemberTestDataBuilder.aMemberWithId(activeMemberId).withActive(true).build();
            Member inactiveMember = MemberTestDataBuilder.aMemberWithId(inactiveMemberId).withActive(false).build();

            when(managementService.listMembers(
                    argThat(filter -> filter.status() == MemberFilter.StatusFilter.ALL),
                    any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(activeMember, inactiveMember)));

            mockMvc.perform(get("/api/members").param("status", "ALL").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList.length()").value(2))
                    .andExpect(jsonPath("$.page.totalElements").value(2));
        }
    }

    @Nested
    @DisplayName("GET /api/members — list row sync link (design.md D4)")
    class ListRowSyncLinkTests {

        private static String link(String name) {
            return "$._embedded.memberSummaryResponseList[0]._links." + name + ".href";
        }

        @Test
        @DisplayName("ORIS-enrolled member row carries a sync link")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ})
        void enrolledRowCarriesSyncLink() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member orisMember = MemberTestDataBuilder.aMemberWithId(memberId).build();

            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(orisMember), PageRequest.of(0, 10), 1));

            SyncTarget target = new SyncTarget(SyncEntityType.MEMBER, memberId.toString());
            com.klabis.sync.domain.SyncedEntityReference syncedReference = new com.klabis.sync.domain.SyncedEntityReference(
                    target, new ExternalReference(ExternalSystem.ORIS, "42"));
            when(synchronizationPort.findActiveByTargets(eq(SyncEntityType.MEMBER), any()))
                    .thenReturn(List.of(syncedReference));

            mockMvc.perform(get("/api/members").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(link("sync")).exists());
        }

        @Test
        @DisplayName("hand-registered member row does NOT carry a sync link")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ})
        void nonEnrolledRowDoesNotCarrySyncLink() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member handRegisteredMember = MemberTestDataBuilder.aMemberWithId(memberId).build();

            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(handRegisteredMember), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/api/members").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0]._links.sync").doesNotExist());
        }
    }

    @Nested
    @DisplayName("GET /api/members — filter params")
    class ListMembersFilterTests {

        @Test
        @DisplayName("3.1 — q param is passed as fulltextQuery in the filter")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldPassQParamAsFulltextQuery() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/members")
                    .param("q", "novak")
                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE));

            Mockito.verify(managementService).listMembers(
                    argThat(filter -> "novak".equals(filter.fulltextQuery())),
                    any(org.springframework.data.domain.Pageable.class)
            );
        }

        @Test
        @DisplayName("3.2 — q param absent means fulltextQuery is null in filter")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldPassNullFulltextQueryWhenQParamAbsent() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/members")
                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE));

            Mockito.verify(managementService).listMembers(
                    argThat(filter -> filter.fulltextQuery() == null),
                    any(org.springframework.data.domain.Pageable.class)
            );
        }

        @Test
        @DisplayName("3.3 — MANAGE caller with status=INACTIVE gets StatusFilter.INACTIVE in filter")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void shouldPassInactiveStatusForManageCaller() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/members")
                    .param("status", "INACTIVE")
                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk());

            Mockito.verify(managementService).listMembers(
                    argThat(filter -> filter.status() == MemberFilter.StatusFilter.INACTIVE),
                    any(org.springframework.data.domain.Pageable.class)
            );
        }

        @Test
        @DisplayName("3.3 — MANAGE caller with status=ALL gets StatusFilter.ALL in filter")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void shouldPassAllStatusForManageCaller() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/members")
                    .param("status", "ALL")
                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk());

            Mockito.verify(managementService).listMembers(
                    argThat(filter -> filter.status() == MemberFilter.StatusFilter.ALL),
                    any(org.springframework.data.domain.Pageable.class)
            );
        }

        @Test
        @DisplayName("3.3 — unrecognized status value returns 400")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void shouldReturn400ForUnrecognizedStatusValue() throws Exception {
            mockMvc.perform(get("/api/members")
                    .param("status", "INVALID_VALUE")
                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("3.5 — non-MANAGE caller with status=INACTIVE is silently forced to ACTIVE")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldForceActiveStatusForNonManageCaller() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/members")
                    .param("status", "INACTIVE")
                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk());

            Mockito.verify(managementService).listMembers(
                    argThat(filter -> filter.status() == MemberFilter.StatusFilter.ACTIVE),
                    any(org.springframework.data.domain.Pageable.class)
            );
        }

        @Test
        @DisplayName("3.5 — non-MANAGE caller with status=ALL is silently forced to ACTIVE")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldForceActiveStatusWhenNonManageCallerRequestsAll() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/members")
                    .param("status", "ALL")
                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk());

            Mockito.verify(managementService).listMembers(
                    argThat(filter -> filter.status() == MemberFilter.StatusFilter.ACTIVE),
                    any(org.springframework.data.domain.Pageable.class)
            );
        }

        @Test
        @DisplayName("3.7 — default sort includes firstName ASC as secondary tiebreak")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldHaveFirstNameAscAsSecondaryDefaultSort() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/members")
                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE));

            Mockito.verify(managementService).listMembers(
                    any(MemberFilter.class),
                    argThat(pageable ->
                            pageable.getSort().getOrderFor("lastName") != null &&
                            pageable.getSort().getOrderFor("lastName").isAscending() &&
                            pageable.getSort().getOrderFor("firstName") != null &&
                            pageable.getSort().getOrderFor("firstName").isAscending()
                    )
            );
        }

        @Test
        @DisplayName("3.8 — self link is a concrete URL with applied filter params, not a URI template")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void selfLinkMustBeConcreteUrlNotUriTemplate() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/members")
                    .param("status", "ACTIVE")
                    .param("q", "novak")
                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.containsString("status=ACTIVE")))
                    .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.containsString("q=novak")))
                    .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("{"))))
                    .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("}"))))
                    .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("%7B"))))
                    .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("%7D"))));
        }

        @Test
        @DisplayName("3.9 — self link has no template variables when only status is provided (q absent)")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void selfLinkHasNoTemplateVarsWhenOnlyStatusProvided() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/members")
                    .param("status", "ACTIVE")
                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.containsString("status=ACTIVE")))
                    .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("{"))))
                    .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("}"))))
                    .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("%7B"))))
                    .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("%7D"))));
        }

        @Test
        @DisplayName("3.10 — self link has no template variables when only q is provided (status absent)")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void selfLinkHasNoTemplateVarsWhenOnlyQProvided() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/members")
                    .param("q", "novak")
                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.containsString("q=novak")))
                    .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("{"))))
                    .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("}"))))
                    .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("%7B"))))
                    .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("%7D"))));
        }

        @Test
        @DisplayName("7.3 — incomplete=true filters for MEMBERS:MANAGE callers")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void incompleteTrueFiltersForManageCaller() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/members")
                    .param("incomplete", "true")
                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk());

            Mockito.verify(managementService).listMembers(
                    argThat(MemberFilter::incompleteOnly),
                    any(org.springframework.data.domain.Pageable.class)
            );
        }

        @Test
        @DisplayName("7.3 — incomplete=true is ignored for callers without MEMBERS:MANAGE")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void incompleteTrueIsIgnoredForNonManageCaller() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/members")
                    .param("incomplete", "true")
                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk());

            Mockito.verify(managementService).listMembers(
                    argThat(filter -> !filter.incompleteOnly()),
                    any(org.springframework.data.domain.Pageable.class)
            );
        }

        @Test
        @DisplayName("7.3 — incomplete=true is preserved in the collection's self link")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        void incompleteParamPreservedInSelfLink() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/members")
                    .param("incomplete", "true")
                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.containsString("incomplete=true")));
        }
    }

    @Nested
    @DisplayName("POST /api/members/{id}/suspend")
    class SuspendMemberTests {

        private MockHttpServletRequestBuilder postMemberIdSuspend(UUID memberId) {
            return post("/api/members/" + memberId.toString() + "/suspend")
                    .contentType("application/json");
        }

        @Test
        @DisplayName("it should call expected service method with correct arguments")
        @WithKlabisMockUser(userId = "48e11797-a61b-4783-bc1d-1c11d1b1d288", authorities = {Authority.MEMBERS_MANAGE})
        void shouldCallExpectedServiceMethodWithCorrectArguments() throws Exception {
            // Arrange
            UUID memberId = UUID.randomUUID();

            // Act
            mockMvc.perform(postMemberIdSuspend(memberId).content("""
                    {
                        "reason": "ODHLASKA",
                        "note": "Member requested termination"
                    }
                    """)
            );

            // Assert
            Member.SuspendMembership expectedCommand = MemberSuspendMembershipBuilder.builder()
                    .suspendedBy(UserId.fromString("48e11797-a61b-4783-bc1d-1c11d1b1d288"))
                    .reason(DeactivationReason.ODHLASKA)
                    .note("Member requested termination")
                    .build();

            Mockito.verify(managementService)
                    .suspendMember(eq(new MemberId(memberId)), eq(expectedCommand));
        }

        @Test
        @DisplayName("valid suspension request should return 204 NO CONTENT response")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        void shouldSuspendMemberSuccessfully() throws Exception {
            // Arrange
            UUID memberId = UUID.randomUUID();

            // Act & Assert
            mockMvc.perform(postMemberIdSuspend(memberId).content("""
                            {
                                "reason": "ODHLASKA",
                                "note": "Member requested termination"
                            }
                            """)
                    )
                    .andExpect(status().isNoContent())
                    .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/api/members"));
        }

        @Test
        @DisplayName("should return 400 Bad Request when service throws InvalidUpdateException")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WhenSuspendingAlreadySuspendedMember() throws Exception {
            // Arrange
            UUID memberId = UUID.randomUUID();

            when(managementService.suspendMember(eq(new MemberId(memberId)),
                    any(Member.SuspendMembership.class)))
                    .thenThrow(new InvalidUpdateException("Member is already suspended"));

            // Act & Assert
            mockMvc.perform(postMemberIdSuspend(memberId).content("""
                            {
                                "reason": "OTHER",
                                "note": "Second termination attempt"
                            }
                            """)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("already suspended")));
        }

        @Test
        @DisplayName("missing reason should return 400 with fieldErrors.reason")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WithFieldErrorWhenReasonIsMissing() throws Exception {
            // Arrange
            UUID memberId = UUID.randomUUID();

            // Act & Assert
            mockMvc.perform(postMemberIdSuspend(memberId).content("""
                            {
                                "note": "Test note"
                            }
                            """)
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.reason").value("must not be null"));
        }

        @Test
        @DisplayName("note exceeding 500 characters should return 400 with fieldErrors.note")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WithFieldErrorWhenNoteExceeds500Chars() throws Exception {
            // Arrange
            UUID memberId = UUID.randomUUID();
            String longNote = "a".repeat(501);

            // Act & Assert
            mockMvc.perform(postMemberIdSuspend(memberId).content("""
                            {
                                "reason": "ODHLASKA",
                                "note": "%s"
                            }
                            """.formatted(longNote))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.note").value("size must be between 0 and 500"));
        }

        @Test
        @DisplayName("should return 409 with affected groups when member is last owner of a group")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn409WhenMemberIsLastGroupOwner() throws Exception {
            UUID memberId = UUID.randomUUID();
            List<OwnedGroup> groups = List.of(
                    new OwnedGroup("cccccccc-cccc-cccc-cccc-cccccccccccc", "Trail Runners", "FREE")
            );

            when(managementService.suspendMember(eq(new MemberId(memberId)), any(Member.SuspendMembership.class)))
                    .thenThrow(new SuspensionBlockedException(groups, null));

            mockMvc.perform(postMemberIdSuspend(memberId).content("""
                            {
                                "reason": "ODHLASKA"
                            }
                            """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.groups.affectedGroups").isArray())
                    .andExpect(jsonPath("$.groups.affectedGroups[0].groupName").value("Trail Runners"))
                    .andExpect(jsonPath("$.groups.affectedGroups[0].groupType").value("FREE"))
                    .andExpect(jsonPath("$.debt").doesNotExist());
        }

        @Test
        @DisplayName("should return 409 with balance and accountLink when member has outstanding debt")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn409WhenMemberHasOutstandingDebt() throws Exception {
            UUID memberId = UUID.randomUUID();
            var snapshot = new MemberFinancialStatePort.MemberFinancialSnapshot(
                    new MemberId(memberId),
                    new MonetaryAmount(new java.math.BigDecimal("-250"), "CZK"),
                    true);

            when(managementService.suspendMember(eq(new MemberId(memberId)), any(Member.SuspendMembership.class)))
                    .thenThrow(new SuspensionBlockedException(List.of(), snapshot));

            mockMvc.perform(postMemberIdSuspend(memberId).content("""
                            {
                                "reason": "ODHLASKA"
                            }
                            """))
                    .andExpect(status().is(409))
                    .andExpect(jsonPath("$.debt.balance.amount").value(-250))
                    .andExpect(jsonPath("$.debt.balance.currency").value("CZK"))
                    .andExpect(jsonPath("$.debt.accountLink").value(
                            "http://localhost/api/members/%s/account".formatted(memberId)))
                    .andExpect(jsonPath("$.groups").doesNotExist());
        }

        @Test
        @DisplayName("should return 409 with both debt and groups when member has both blockers")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn409WithBothBlockersWhenBothApply() throws Exception {
            UUID memberId = UUID.randomUUID();
            List<OwnedGroup> groups = List.of(
                    new OwnedGroup("cccccccc-cccc-cccc-cccc-cccccccccccc", "Trail Runners", "FREE")
            );
            var snapshot = new MemberFinancialStatePort.MemberFinancialSnapshot(
                    new MemberId(memberId),
                    new MonetaryAmount(new java.math.BigDecimal("-250"), "CZK"),
                    true);

            when(managementService.suspendMember(eq(new MemberId(memberId)), any(Member.SuspendMembership.class)))
                    .thenThrow(new SuspensionBlockedException(groups, snapshot));

            mockMvc.perform(postMemberIdSuspend(memberId).content("""
                            {
                                "reason": "ODHLASKA"
                            }
                            """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.groups.affectedGroups[0].groupName").value("Trail Runners"))
                    .andExpect(jsonPath("$.debt.balance.amount").value(-250))
                    .andExpect(jsonPath("$.debt.accountLink").value(
                            "http://localhost/api/members/%s/account".formatted(memberId)));
        }
    }

    @Nested
    @DisplayName("POST /api/members/{id}/resume")
    class ResumeMemberTests {

        private MockHttpServletRequestBuilder postMemberIdResume(UUID memberId) {
            return post("/api/members/" + memberId.toString() + "/resume");
        }

        @Test
        @DisplayName("valid resume request should return 204 NO CONTENT response")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
        void shouldResumeMemberSuccessfully() throws Exception {
            UUID memberId = UUID.randomUUID();

            mockMvc.perform(postMemberIdResume(memberId))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isNoContent())
                    .andExpect(header().exists(HttpHeaders.LOCATION));
        }

        @Test
        @DisplayName("should call service with correct arguments")
        @WithKlabisMockUser(userId = "48e11797-a61b-4783-bc1d-1c11d1b1d288", authorities = {Authority.MEMBERS_MANAGE})
        void shouldCallServiceWithCorrectArguments() throws Exception {
            UUID memberId = UUID.randomUUID();

            mockMvc.perform(postMemberIdResume(memberId));

            Member.ResumeMembership expectedCommand = MemberResumeMembershipBuilder.builder()
                    .resumedBy(UserId.fromString("48e11797-a61b-4783-bc1d-1c11d1b1d288"))
                    .build();
            Mockito.verify(managementService).resumeMember(eq(new MemberId(memberId)), eq(expectedCommand));
        }

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:MANAGE authority")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldReturn403WhenUnauthorized() throws Exception {
            UUID memberId = UUID.randomUUID();

            mockMvc.perform(postMemberIdResume(memberId))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            UUID memberId = UUID.randomUUID();

            mockMvc.perform(postMemberIdResume(memberId))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("X-Warnings header for birth number consistency")
    class BirthNumberConsistencyWarningsTests {

        private final String REGISTER_BODY = """
                {
                    "firstName": "Jana",
                    "lastName": "Nováková",
                    "dateOfBirth": "1990-05-15",
                    "nationality": "CZ",
                    "gender": "FEMALE",
                    "email": "jana.novakova@example.com",
                    "phone": "+420777123456",
                    "address": {
                        "street": "Hlavní 123",
                        "city": "Praha",
                        "postalCode": "11000",
                        "country": "CZ"
                    },
                    "birthNumber": "905101/1239"
                }
                """;

        @Test
        @DisplayName("POST /api/members should include X-Warnings header when birth number date mismatches dateOfBirth")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
        void shouldIncludeWarningsHeaderWhenBirthNumberDateMismatches() throws Exception {
            UUID memberId = UUID.randomUUID();
            // Member with dateOfBirth 1990-05-15 but birth number 905101 implies 1990-01-01 → date mismatch
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withDateOfBirth(LocalDate.of(1990, 5, 15))
                    .withGender(Gender.FEMALE)
                    .withNationality("CZ")
                    .withBirthNumber("905101/1239")
                    .build();
            when(registrationService.registerMember(any(RegistrationPort.RegisterNewMember.class))).thenReturn(member);

            mockMvc.perform(post("/api/members")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(REGISTER_BODY))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("X-Warnings"));
        }

        @Test
        @DisplayName("POST /api/members should not include X-Warnings header when birth number is consistent")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
        void shouldNotIncludeWarningsHeaderWhenBirthNumberIsConsistent() throws Exception {
            UUID memberId = UUID.randomUUID();
            // Member with dateOfBirth 1990-01-01 and birth number 905101 (female, 1990-01-01) → consistent
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withDateOfBirth(LocalDate.of(1990, 1, 1))
                    .withGender(Gender.FEMALE)
                    .withNationality("CZ")
                    .withBirthNumber("905101/1239")
                    .build();
            when(registrationService.registerMember(any(RegistrationPort.RegisterNewMember.class))).thenReturn(member);

            mockMvc.perform(post("/api/members")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(REGISTER_BODY))
                    .andExpect(status().isCreated())
                    .andExpect(header().doesNotExist("X-Warnings"));
        }

        @Test
        @DisplayName("PATCH /api/members/{id} should include X-Warnings header when birth number gender mismatches")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
        void shouldIncludeWarningsHeaderOnUpdateWhenGenderMismatches() throws Exception {
            UUID memberId = UUID.randomUUID();
            // Member is MALE but birth number 905101 indicates FEMALE → gender mismatch warning
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withDateOfBirth(LocalDate.of(1990, 1, 1))
                    .withGender(Gender.MALE)
                    .withNationality("CZ")
                    .withBirthNumber("905101/1239")
                    .build();
            when(managementService.prefilledUpdateCommand(any(MemberId.class)))
                    .thenReturn(Member.UpdateMember.from(member));
            when(managementService.updateMember(any(MemberId.class), any(Member.UpdateMember.class))).thenReturn(member);

            mockMvc.perform(patch("/api/members/{id}", memberId)
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "birthNumber": "905101/1239"
                                    }
                                    """))
                    .andExpect(status().isNoContent())
                    .andExpect(header().exists("X-Warnings"));
        }

        @Test
        @DisplayName("PATCH /api/members/{id} should not include X-Warnings when no birth number provided")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
        void shouldNotIncludeWarningsWhenNoBirthNumberOnUpdate() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .build();
            when(managementService.prefilledUpdateCommand(any(MemberId.class)))
                    .thenReturn(Member.UpdateMember.from(member));
            when(managementService.updateMember(any(MemberId.class), any(Member.UpdateMember.class))).thenReturn(member);

            mockMvc.perform(patch("/api/members/{id}", memberId)
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "new@example.com"
                                    }
                                    """))
                    .andExpect(status().isNoContent())
                    .andExpect(header().doesNotExist("X-Warnings"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/members/{id} — self-update security")
    class SelfUpdateSecurityTests {

        @Test
        @DisplayName("self-update should only pass allowed fields (email, phone, address, dietaryRestrictions) to service")
        @WithKlabisMockUser(username = MEMBER_USERNAME, memberId = "11111111-1111-1111-1111-111111111111", authorities = {Authority.MEMBERS_READ},
                targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE, type = TargetType.MEMBER,
                        ids = "11111111-1111-1111-1111-111111111111"))
        void selfUpdateShouldOnlyPassAllowedFieldsToService() throws Exception {
            UUID memberId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withEmail("original@example.com")
                    .withPhone("+420111000111")
                    .withDateOfBirth(LocalDate.of(1990, 1, 1))
                    .build();
            when(managementService.prefilledUpdateCommand(any(MemberId.class)))
                    .thenReturn(Member.UpdateMember.from(member));
            when(managementService.updateMember(any(MemberId.class), any(Member.UpdateMember.class))).thenReturn(member);

            mockMvc.perform(patch("/api/members/{id}", memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "new@example.com"
                                    }
                                    """))
                    .andExpect(status().isNoContent());

            Mockito.verify(managementService).updateMember(
                    eq(new MemberId(memberId)),
                    argThat((Member.UpdateMember cmd) ->
                            cmd.email().equals(EmailAddress.of("new@example.com"))
                            && cmd.phone().equals(PhoneNumber.of("+420111000111")))
            );
        }
    }

    @Nested
    @DisplayName("PATCH /api/members/{id} — ownership security")
    class UpdateMemberOwnershipTests {

        @Test
        @DisplayName("authenticated user without a member record should get 403 Forbidden")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void userWithoutMemberRecordShouldBeForbidden() throws Exception {
            UUID anyMemberId = UUID.fromString("11111111-1111-1111-1111-111111111111");

            mockMvc.perform(patch("/api/members/{id}", anyMemberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "new@example.com"
                                    }
                                    """))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("member attempting to edit another member's profile should get 403 Forbidden")
        @WithKlabisMockUser(username = MEMBER_USERNAME, memberId = "22222222-2222-2222-2222-222222222222", authorities = {Authority.MEMBERS_READ})
        void memberEditingOtherMemberShouldBeForbidden() throws Exception {
            UUID otherMemberId = UUID.fromString("11111111-1111-1111-1111-111111111111");

            mockMvc.perform(patch("/api/members/{id}", otherMemberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "new@example.com"
                                    }
                                    """))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/members/options")
    class GetMemberOptionsTests {

        @Test
        @DisplayName("should return 200 with list of active members as value+prompt pairs")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldReturnActiveMembers() throws Exception {
            UUID memberId1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
            UUID memberId2 = UUID.fromString("22222222-2222-2222-2222-222222222222");

            Member activeMember1 = MemberTestDataBuilder.aMemberWithId(memberId1)
                    .withName("Jan", "Novák")
                    .withRegistrationNumber("ZBM0001")
                    .withActive(true)
                    .build();
            Member activeMember2 = MemberTestDataBuilder.aMemberWithId(memberId2)
                    .withName("Eva", "Svobodová")
                    .withRegistrationNumber("ZBM0002")
                    .withActive(true)
                    .build();

            when(managementService.listActiveMembers()).thenReturn(List.of(activeMember1, activeMember2));

            mockMvc.perform(get("/api/members/options")
                            .accept(MediaType.APPLICATION_JSON))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].value").value(memberId1.toString()))
                    .andExpect(jsonPath("$[0].prompt").value("Jan Novák (ZBM0001)"))
                    .andExpect(jsonPath("$[1].value").value(memberId2.toString()))
                    .andExpect(jsonPath("$[1].prompt").value("Eva Svobodová (ZBM0002)"));
        }

        @Test
        @DisplayName("should return empty array when no active members")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldReturnEmptyArrayWhenNoActiveMembers() throws Exception {
            when(managementService.listActiveMembers()).thenReturn(List.of());

            mockMvc.perform(get("/api/members/options")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:READ authority")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn403WhenUnauthorized() throws Exception {
            mockMvc.perform(get("/api/members/options")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/members/options")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Endpoint security (401 / 403 / authorization passes)")
    class EndpointSecurityTests {

        private static final String REGISTER_BODY = """
                {
                    "firstName": "Jan",
                    "lastName": "Novák",
                    "dateOfBirth": "2005-05-15",
                    "nationality": "CZ",
                    "gender": "MALE",
                    "email": "jan@example.com",
                    "phone": "+420777123456",
                    "birthNumber": "050515/1234",
                    "address": {
                        "street": "Hlavní 123",
                        "city": "Praha",
                        "postalCode": "11000",
                        "country": "CZ"
                    }
                }
                """;

        private static final String SUSPEND_BODY = """
                {
                    "reason": "ODHLASKA",
                    "note": "Test termination"
                }
                """;

        @Test
        @DisplayName("POST /api/members without authentication should return 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(post("/api/members").contentType("application/json").content(REGISTER_BODY))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.title").value("Unauthorized"))
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("POST /api/members with wrong authority should return 403")
        @WithKlabisMockUser(username = "ZBM0102", authorities = {Authority.MEMBERS_READ})
        void shouldReturn403WhenInsufficientAuthority() throws Exception {
            when(registrationService.registerMember(any(RegistrationPort.RegisterNewMember.class)))
                    .thenReturn(MemberTestDataBuilder.aMember().build());

            mockMvc.perform(post("/api/members").contentType("application/json").content(REGISTER_BODY))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.title").value("Forbidden"))
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("POST /api/members with MEMBERS:MANAGE authority should return 201")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn201WhenAuthorized() throws Exception {
            when(registrationService.registerMember(any(RegistrationPort.RegisterNewMember.class)))
                    .thenReturn(MemberTestDataBuilder.aMember().build());

            mockMvc.perform(post("/api/members").contentType("application/json").content(REGISTER_BODY))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists(HttpHeaders.LOCATION));
        }

        @Test
        @DisplayName("GET /api/members/{id} without authentication should return 401")
        void shouldReturn401WhenGettingMemberUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/members/" + UUID.randomUUID()).contentType("application/json"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.title").value("Unauthorized"));
        }

        @Test
        @DisplayName("GET /api/members/{id} with wrong authority should return 403")
        @WithKlabisMockUser(username = "ZBM0102", authorities = {})
        void shouldReturn403WhenGettingMemberWithoutReadAuthority() throws Exception {
            UUID memberId = UUID.randomUUID();
            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean()))
                    .thenReturn(MemberTestDataBuilder.aMemberWithId(memberId).build());

            mockMvc.perform(get("/api/members/" + memberId).contentType("application/json"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.title").value("Forbidden"));
        }

        @Test
        @DisplayName("GET /api/members/{id} with MEMBERS:READ authority should pass authorization")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldPassAuthorizationWhenGettingMemberWithReadAuthority() throws Exception {
            UUID memberId = UUID.randomUUID();
            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean()))
                    .thenThrow(new MemberNotFoundException(new MemberId(memberId)));

            mockMvc.perform(get("/api/members/" + memberId).contentType("application/json"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"));
        }

        @Test
        @DisplayName("GET /api/members without authentication should return 401")
        void shouldReturn401WhenListingMembersUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/members").contentType("application/json"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.title").value("Unauthorized"))
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("GET /api/members with wrong authority should return 403")
        @WithKlabisMockUser(username = "ZBM0102", authorities = {Authority.CALENDAR_MANAGE})
        void shouldReturn403WhenListingMembersWithoutReadAuthority() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/members").contentType("application/json"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.title").value("Forbidden"))
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("GET /api/members with MEMBERS:READ authority should return 200")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldReturn200WhenListingMembersWithReadAuthority() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/members").contentType("application/json"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/members/{id}/suspend without authentication should return 401")
        void shouldReturn401WhenSuspendingMemberUnauthenticated() throws Exception {
            mockMvc.perform(post("/api/members/" + UUID.randomUUID() + "/suspend")
                            .contentType("application/json").content(SUSPEND_BODY))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.title").value("Unauthorized"))
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("POST /api/members/{id}/suspend with wrong authority should return 403")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldReturn403WhenSuspendingMemberWithoutUpdateAuthority() throws Exception {
            mockMvc.perform(post("/api/members/" + UUID.randomUUID() + "/suspend")
                            .contentType("application/json").content(SUSPEND_BODY))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.title").value("Forbidden"))
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("POST /api/members/{id}/suspend with MEMBERS:MANAGE authority should pass authorization")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
        void shouldPassAuthorizationWhenSuspendingMemberWithUpdateAuthority() throws Exception {
            UUID memberId = UUID.randomUUID();
            when(managementService.suspendMember(any(MemberId.class), any(Member.SuspendMembership.class)))
                    .thenThrow(new MemberNotFoundException(new MemberId(memberId)));

            mockMvc.perform(post("/api/members/" + memberId + "/suspend")
                            .contentType("application/json").content(SUSPEND_BODY))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("ORIS import disabled (no MemberDiscoveryPort bean, oris profile off)")
    class OrisImportDisabledTests {

        @Test
        @DisplayName("POST /api/members/oris-import -> 404 even for SYNC:MANAGE holder")
        @WithKlabisMockUser(authorities = Authority.SYNC_MANAGE)
        void importEndpointNotFound() throws Exception {
            mockMvc.perform(post("/api/members/oris-import").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("importFromOris affordance absent even with SYNC:MANAGE and club key held")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_READ, Authority.SYNC_MANAGE})
        void affordanceAbsent() throws Exception {
            when(managementService.listMembers(any(MemberFilter.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));
            when(orisClubKeyManagementPort.isSet()).thenReturn(true);

            mockMvc.perform(get("/api/members").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.importFromOris").doesNotExist());
        }
    }

    @Nested
    @DisplayName("GET /api/members/{id} — legal guardian group links")
    class LegalGuardianGroupLinksTests {

        private static final String CHILD_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        private static final MemberId CHILD = new MemberId(UUID.fromString(CHILD_ID));
        private static final String OTHER_CHILD_ID = "33333333-3333-3333-3333-333333333333";
        private static final MemberId GUARDIAN = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        private LegalGuardianGroup groupOfChild() {
            return LegalGuardianGroup.create(
                    java.util.Set.of(new LegalGuardianGroup.Guardian(GUARDIAN.toUserId(), "Novák")),
                    new LegalGuardianGroup.Minor(CHILD, LocalDate.now().minusYears(9)));
        }

        private void givenMember(LocalDate dateOfBirth, LegalGuardianGroup group) {
            Member member = MemberTestDataBuilder.aMemberWithId(CHILD.uuid()).withDateOfBirth(dateOfBirth).build();
            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean()))
                    .thenReturn(member);
            when(legalGuardianGroupPort.findGroupOf(any(MemberId.class))).thenReturn(Optional.ofNullable(group));
        }

        private org.springframework.test.web.servlet.ResultActions getChild() throws Exception {
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
                    .value(org.hamcrest.Matchers.endsWith("/api/legal-guardian-groups/" + group.getId().uuid())));
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
                    .value(org.hamcrest.Matchers.endsWith("/api/legal-guardian-groups/" + group.getId().uuid() + "/guardians")));
        }

        @Test
        @DisplayName("adds legalGuardians link on the minor's own detail without MEMBERS:MANAGE")
        @WithKlabisMockUser(memberId = CHILD_ID, authorities = {Authority.MEMBERS_READ})
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
}
